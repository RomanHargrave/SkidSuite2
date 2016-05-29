package me.lpk;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.lpk.util.RegexUtils;

public class Remover {
	private final static Set<String> visited = new HashSet<String>();

	public static Set<String> evaluate(String mainClass, Map<String, ClassNode> nodes) {
		Set<String> keep = new HashSet<String>();
		ClassNode initNode = nodes.get(mainClass);
		if (initNode == null){
			JOptionPane.showMessageDialog(null, "Main class '" + mainClass + "' is not in the Jar!", "Error", JOptionPane.ERROR_MESSAGE);
			return keep;
		}
		keep.add(mainClass);
		keep.addAll(check(initNode, nodes));
		return keep;
	}

	/**
	 * 
	 * @param node
	 * @param nodes
	 * @return
	 */
	private static Set<String> check(ClassNode node, Map<String, ClassNode> nodes) {
		Set<String> keep = new HashSet<String>();
		visited.add(node.name);
		String parent = node.superName;
		if (parent != null) {
			keep.add(parent);
			if (!visited.contains(parent) && nodes.containsKey(parent)) {
				keep.addAll(check(nodes.get(parent), nodes));
			}
		}
		for (String name : node.interfaces) {
			keep.add(name);
			if (!visited.contains(name) && nodes.containsKey(name)) {
				keep.addAll(check(nodes.get(name), nodes));
			}
		}
		for (FieldNode fn : node.fields) {
			for (String name : RegexUtils.matchDescriptionClasses(fn.desc)) {
				keep.add(name);
				if (!visited.contains(name) && nodes.containsKey(name)) {
					keep.addAll(check(nodes.get(name), nodes));
				}
			}
		}
		for (MethodNode mn : node.methods) {
			for (String name : RegexUtils.matchDescriptionClasses(mn.desc)) {
				keep.add(name);
				if (!visited.contains(name) && nodes.containsKey(name)) {
					keep.addAll(check(nodes.get(name), nodes));
				}
			}
			for (AbstractInsnNode ain : mn.instructions.toArray()) {
				if (ain.getType() == AbstractInsnNode.FIELD_INSN) {
					FieldInsnNode fin = (FieldInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(fin.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check(nodes.get(name), nodes));
						}
					}
					keep.add(fin.owner);
					if (!visited.contains(fin.owner) && nodes.containsKey(fin.owner)) {
						keep.addAll(check(nodes.get(fin.owner), nodes));
					}
				} else if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
					MethodInsnNode min = (MethodInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(min.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check(nodes.get(name), nodes));
						}
					}
					keep.add(min.owner);
					if (!visited.contains(min.owner) && nodes.containsKey(min.owner)) {
						keep.addAll(check(nodes.get(min.owner), nodes));
					}
				} else if (ain.getType() == AbstractInsnNode.LDC_INSN) {
					LdcInsnNode ldc = (LdcInsnNode) ain;
					if (ldc.cst instanceof Type) {
						Type t = (Type) ldc.cst;
						String name = t.getClassName().replace(".", "/");
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check(nodes.get(name), nodes));
						}
					}
				} else if (ain.getType() == AbstractInsnNode.TYPE_INSN) {
					TypeInsnNode tin = (TypeInsnNode) ain;
					for (String name : RegexUtils.matchDescriptionClasses(tin.desc)) {
						keep.add(name);
						if (!visited.contains(name) && nodes.containsKey(name)) {
							keep.addAll(check(nodes.get(name), nodes));
						}
					}
				}
			}
		}
		return keep;
	}

}
