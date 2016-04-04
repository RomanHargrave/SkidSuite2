package me.lpk.mapping.remap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.util.AccessHelper;
import me.lpk.util.ParentUtils;

public class MappingRenamer {
	private static final Set<String> whitelist = new HashSet<String>();

	/**
	 * Updates the information of the given map of MappedClasses according to
	 * the mapping standards given by the MappingMode.
	 * 
	 * @param mappings
	 * @param mode
	 * @return
	 */
	public static Map<String, MappedClass> remapClasses(Map<String, MappedClass> mappings, MappingMode mode) {
		for (MappedClass mc : mappings.values()) {
			remapClass(mc, mappings, mode);
		}
		return mappings;
	}

	private static Map<String, MappedClass> remapClass(MappedClass mc, Map<String, MappedClass> mappings, MappingMode mode) {
		if (mc.hasParent()) {
			mappings = remapClass(mc.getParent(), mappings, mode);
		}
		for (MappedClass interfaze : mc.getInterfacesMap().values()) {
			mappings = remapClass(interfaze, mappings, mode);
		}
		if (mc.isInnerClass()) {
			mappings = remapClass(mc.getOuterClass(), mappings, mode);
		}
		if (!mc.isInnerClass()) {
			// Handling naming of normal class
			mc.setNewName(mode.getClassName(mc.getNode()));
		} else {
			// Handling naming of inner class names
			if (mc.getOriginalName().contains("$")) {
				String post = mc.getOriginalName().substring(mc.getOriginalName().indexOf("$") + 1);
				mc.setNewName(mc.getOuterClass().getNewName() + "$" + post);
			} else {
				int index = 0;
				for (String name : mc.getOuterClass().getInnerClassMap().keySet()) {
					index += 1;
					if (name.equals(mc.getOriginalName())) {
						break;
					}
				}
				mc.setNewName(mc.getOuterClass().getNewName() + "$" + index);
			}
		}
		for (MappedMember mm : mc.getFields()) {
			// Rename fields
			mm.setNewName(mode.getFieldName(mm.getFieldNode()));
		}
		for (MappedMember mm : mc.getMethods()) {
			// Rename methods
			if (keepName(mm)) {
				// Skip methods that should not be renamed
				continue;
			}
			MappedMember parentMember = ParentUtils.findMethodOverride(mm);
			// Check and see if theres a parent member to pull names from.
			if (parentMember == null || parentMember.equals(mm)) {
				// No parent found. Not currently renamed.
				if (ParentUtils.callsSuper(mm.getMethodNode())) {
					// Don't rename the method, but mark it as if we did.
					// Parent can't be found but it DOES call a parent.
					mm.setRenamedOverride(true);
				} else {
					// Rename the method.
					mm.setNewName(mode.getMethodName(mm.getMethodNode()));
				}
			} else {
				// Parent found
				// Rename and override current entry.
				mm.setNewName(parentMember.getNewName());
			}
			MethodNode mn = mm.getMethodNode();
			updateStrings(mn, mappings);
		}
		return mappings;
	}

	private static void updateStrings(MethodNode mn, Map<String, MappedClass> mappings) {
		// TODO: Check for Class.forName(String)
	}

	public static boolean keepName(MappedMember mm) {
		// Main class
		if (mm.getDesc().equals("([Ljava/lang/String;)V") && mm.getOriginalName().equals("main")) {
			return true;
		}
		// <init> or <clinit>
		if (mm.getOriginalName().contains("<")) {
			return true;
		}
		// Synthetic
		// Do all natural synthetic names contain "$"?
		// If so TODO: Add "$" name checks.
		if (AccessHelper.isSynthetic(mm.getMethodNode().access) && !AccessHelper.isPublic(mm.getMethodNode().access)
				&& !AccessHelper.isProtected(mm.getMethodNode().access)) {
			return true;
		}
		// A method name that shan't be renamed!
		if (isNameWhitelisted(mm.getOriginalName())) {
			return true;
		}
		return false;
	}

	public static boolean isNameWhitelisted(String name) {
		return whitelist.contains(name);
	}

	static {
		// TODO: Can I just fix the program so this isn't even needed?
		// TODO: Will need to set this up for a lot of basic methods.
		// Should let user add additional names to the list
		// Have the ugly reflection hack optional for lazy retards like myself.
		Collections.addAll(whitelist, "accept", "actionPerformed", "add", "apply", "clear", "clone", "clone", "compare", "compareTo", "copy", "create", "defineClass",
				"deserialize", "equals", "equals", "finalize", "findClass", "findResource", "forEach", "get", "getClass", "getName", "getResource", "getResourceAsStream",
				"handle", "hashCode", "hasNext", "indexOf", "iterator", "name", "next", "ordinal", "put", "read", "remove", "replace", "run", "serialize", "set", "size",
				"spliterator", "toString", "valueOf", "values", "write");
	}
}
