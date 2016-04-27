package me.lpk.util;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.MainWindow;
import me.lpk.gui.component.SearchResultEntry;
import me.lpk.mapping.MappedClass;
import me.lpk.util.Reference;
import me.lpk.util.ReferenceUtil;

public class SearchUtil {
	/**
	 * Finds strings similiar to the given parameter.
	 * 
	 * @param text
	 * @return
	 */
	public static List<SearchResultEntry> findStringsSimiliar(String text) {
		List<SearchResultEntry> results = findStringsContaining(text);
		// new ArrayList<SearchResultEntry>();
		return results;
	}

	/**
	 * Finds strings in methods containing the given text.
	 * 
	 * @param text
	 * @return
	 */
	public static List<SearchResultEntry> findStringsContaining(String text) {
		List<SearchResultEntry> results = new ArrayList<SearchResultEntry>();
		for (ClassNode cn : MainWindow.instance.getNodes().values()) {
			for (MethodNode mn : cn.methods) {
				for (AbstractInsnNode ain : mn.instructions.toArray()) {
					if (ain.getType() == AbstractInsnNode.LDC_INSN) {
						if (((LdcInsnNode) ain).cst.toString().toLowerCase().contains(text.toLowerCase())) {
							results.add(new SearchResultEntry(cn, mn, OpUtil.getIndex(ain)));
						}
					}
				}
			}
		}
		return results;
	}

	/**
	 * Finds references to the given MethodNode.
	 * 
	 * @param node
	 * @param method
	 * @return
	 */
	public static List<SearchResultEntry> findReferences(ClassNode node, MethodNode method) {
		List<SearchResultEntry> results = findChildren(node);
		List<Reference> references = new ArrayList<Reference>();
		for (ClassNode cn : MainWindow.instance.getNodes().values()) {
			references.addAll(ReferenceUtil.getReferences(node, method, cn));
		}
		for (Reference reference : references) {
			results.add(new SearchResultEntry(reference.getNode(), reference.getMethod(), OpUtil.getIndex(reference.getAin())));
		}
		return results;
	}

	/**
	 * Finds references to the given FieldNode.
	 * 
	 * @param node
	 * @param field
	 * @return
	 */
	public static List<SearchResultEntry> findReferences(ClassNode node, FieldNode field) {
		List<SearchResultEntry> results = findChildren(node);
		List<Reference> references = new ArrayList<Reference>();
		for (ClassNode cn : MainWindow.instance.getNodes().values()) {
			references.addAll(ReferenceUtil.getReferences(node, field, cn));
		}
		for (Reference reference : references) {
			results.add(new SearchResultEntry(reference.getNode(), reference.getMethod(), OpUtil.getIndex(reference.getAin())));
		}
		return results;
	}

	/**
	 * Finds references to the given ClassNode.
	 * 
	 * @param node
	 * @return
	 */
	public static List<SearchResultEntry> findReferences(ClassNode node) {
		List<SearchResultEntry> results = findChildren(node);
		List<Reference> references = new ArrayList<Reference>();
		for (ClassNode cn : MainWindow.instance.getNodes().values()) {
			references.addAll(ReferenceUtil.getReferences(node, cn));
		}
		for (Reference reference : references) {
			results.add(new SearchResultEntry(reference.getNode(), reference.getMethod(), OpUtil.getIndex(reference.getAin())));
		}
		return results;
	}

	/**
	 * Finds children of the given ClassNode.
	 * 
	 * @param node
	 * @return
	 */
	public static List<SearchResultEntry> findChildren(ClassNode node) {
		List<SearchResultEntry> results = new ArrayList<SearchResultEntry>();
		MappedClass parent = fromNode(node);
		for (MappedClass mc : MainWindow.instance.getMappings().values()) {
			if (mc.equals(parent)) {
				continue;
			}
			if (mc.hasParent(parent)) {
				results.add(new SearchResultEntry(mc.getNode()));
			}
		}
		return results;
	}

	/**
	 * Finds methods by the given name or description.
	 * 
	 * @param text
	 * @return
	 */
	public static List<SearchResultEntry> findMethods(String text) {
		List<SearchResultEntry> results = new ArrayList<SearchResultEntry>();
		for (MappedClass mc : MainWindow.instance.getMappings().values()) {
			if (mc.findMethodByName(text, false) != -1) {
				results.add(new SearchResultEntry(mc.getNode()));
				// Class already has a result. It does not need any further
				// results.
				continue;
			}
			List<Integer> finds = mc.findMethodsByDesc(text);
			if (finds.size() > 0) {
				results.add(new SearchResultEntry(mc.getNode()));
			}
		}
		return results;
	}

	/**
	 * Finds fields by the given name or description.
	 * 
	 * @param text
	 * @return
	 */
	public static List<SearchResultEntry> findFields(String text) {
		List<SearchResultEntry> results = new ArrayList<SearchResultEntry>();
		for (MappedClass mc : MainWindow.instance.getMappings().values()) {
			if (mc.findFieldByName(text, false) != -1) {
				results.add(new SearchResultEntry(mc.getNode()));
				// Class already has a result. It does not need any further
				// results.
				continue;
			}
			List<Integer> finds = mc.findFieldsByDesc(text);
			if (finds.size() > 0) {
				results.add(new SearchResultEntry(mc.getNode()));
			}
		}
		return results;
	}

	public static String getOuter(ClassNode node) {
		MappedClass mc = fromNode(node);
		if (mc != null && mc.getOuterClass() != null) {
			return mc.getOuterClass().getNewName();
		}
		return null;
	}

	public static MappedClass fromNode(ClassNode node) {
		return fromString(node.name);
	}

	public static MappedClass fromString(String owner) {
		return MainWindow.instance.getMappings().get(owner);
	}

}
