package me.lpk.mapping.remap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.util.AccessHelper;
import me.lpk.util.ParentUtils;

public class ClassRemapper {
	private static final Set<String> whitelist = new HashSet<String>();

	/**
	 * Updates the information of the given map of MappedClasses according to
	 * the mapping standards given by the MappingMode.
	 * 
	 * @param mappedClasses
	 * @param mode
	 * @return
	 */
	public static Map<String, MappedClass> remapClasses(Map<String, MappedClass> mappedClasses, MappingMode mode) {
		Set<String> classNames = mappedClasses.keySet();
		for (String className : classNames) {
			MappedClass mappedClass = mappedClasses.get(className);
			Map<Integer, MappedMember> fields = mappedClass.getFieldMap();
			Map<Integer, MappedMember> methods = mappedClass.getMethodMap();
			for (int index : fields.keySet()) {
				MappedMember mm = fields.get(index);
				mm.setNewName(mode.getFieldName(mm.getFieldNode()));
			}
			for (int index : methods.keySet()) {
				MappedMember mm = methods.get(index);
				if (keepName(mm)) {
					// Add more checks in keepName?
					continue;
				}
				MappedMember parentMember = ParentUtils.findMethodOverride(mm);
				// Check and see if theres a parent member to pull names from.
				if (parentMember == null) {
					// No parent found. Not currently renamed.
					if (ParentUtils.callsSuper(mm.getMethodNode())) {
						// Don't rename the method, but mark it as if we did.
						// Parent can't be found but it DOES call a parent.
						mm.setRenamed(true);
					} else {
						// Rename the method.
						mm.setNewName(mode.getMethodName(mm.getMethodNode()));
					}
				} else {
					// Parent found
					// Rename and override current entry.
					mm.setNewName(parentMember.getNewName());
				}
				methods.put(index, mm);
			}
		}
		return mappedClasses;
	}

	/**
	 * Given a map of already renamed classes, fill in the gaps for classes that
	 * were not reached, but have parents that can be pulled from.
	 * 
	 * @param mappedClasses
	 * @param mode
	 * @return
	 */
	public static Map<String, MappedClass> fillInTheGaps(Map<String, MappedClass> mappedClasses, MappingMode mode) {
		for (String originalName : mappedClasses.keySet()) {
			mappedClasses = fillGap(mappedClasses.get(originalName), mappedClasses, mode);
		}
		return mappedClasses;
	}

	private static Map<String, MappedClass> fillGap(MappedClass mappedClass, Map<String, MappedClass> mappedClasses, MappingMode mode) {
		// If already renamed, pass
		if (mappedClass.isTruelyRenamed()) {
			return mappedClasses;
		}
		// Map interfaces
		for (MappedClass interfaceClass : mappedClass.getInterfacesMap().values()) {
			mappedClasses = fillGap(interfaceClass, mappedClasses, mode);
		}
		// Map the parents
		MappedClass parent = mappedClass.getParent();
		if (parent != null && !parent.isRenamed()) {
			mappedClasses = fillGap(parent, mappedClasses, mode);
			// Update parent
			parent = mappedClasses.get(parent.getOriginalName());
		}
		// Map name for inner class
		if (mappedClass.isInnerClass()) {
			MappedClass outerClass = mappedClass.getOuterClass();
			mappedClasses = fillGap(outerClass, mappedClasses, mode);
			// I'm lazy and this can be broken.
			// TODO: Properly increment the amount of inner classes
			int post = (int) (Math.random() * 200);
			mappedClass.setNewName(outerClass.getNewName() + "$" + post);
		} else {
			// Normal class
			String newNameClass = mode.getClassName(mappedClass.getNode());
			if (parent != null) {
				// Move next to parent. Organizes packages and is less likely to
				// cass AccessErrors for non-public methods.
				String newNamePackage = parent.getNewName().substring(0, parent.getNewName().lastIndexOf("/") + 1);
				mappedClass.setNewName(newNamePackage + newNameClass);
			} else {
				// Check for interfaces. Put them in that package if there is
				// one or they all are in the same package.
				if (mappedClass.getInterfacesMap().size() > 0) {
					String s = null;
					boolean failed = false;
					for (MappedClass interfaceClass : mappedClass.getInterfacesMap().values()) {
						int index = interfaceClass.getNewName().lastIndexOf("/");
						if (index == -1){
							continue;
						}
						if (s == null) {
							s = interfaceClass.getNewName().substring(0, index);
						} else {
							String temp = interfaceClass.getNewName().substring(0, interfaceClass.getNewName().lastIndexOf("/"));
							if (s != temp){
								failed = true;
							}
						}
					}
					if (failed || s == null){
						mappedClass.setNewName(newNameClass);
					}else{
						mappedClass.setNewName(s + "/" + newNameClass);
					}
				} else if (!mappedClass.isRenamed()){
					mappedClass.setNewName(newNameClass);
				}
			}
		}
		// Map fields since those don't inherit literally.
		for (MappedMember mm : mappedClass.getFields()) {
			mm.setNewName(mode.getFieldName(mm.getFieldNode()));
		}
		// Map methods
		for (int key : mappedClass.getMethodMap().keySet()) {
			MappedMember mm = mappedClass.getMethodMap().get(key);
			// Probably shouldn't touch this.
			if (keepName(mm)) {
				continue;
			}
			// Find the method in a parent class.
			if (mm.doesOverride()) {
				//mappedClasses = fillGap(mm.getOverride().getOwner(), mappedClasses, mode);
				mm.setNewName(ParentUtils.findMethodOverride(mm).getNewName());
			} else {
				mm.setNewName(mode.getMethodName(mm.getMethodNode()));
			}
			mappedClass.getMethodMap().put(key, mm);
		}
		mappedClasses.put(mappedClass.getOriginalName(), mappedClass);
		return mappedClasses;
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
		Collections.addAll(whitelist, "clone", "compareTo", "equals", "add", "hashCode", "name", "getName", "ordinal", "toString", "valueOf", "values", "get", "clear",
				"iterator", "forEach", "read","put", "size", "run", "hasNext", "compare", "equals", "defineClass", "findClass", "findResource", "getResource", "getResourceAsStream",
				"indexOf", "replace", "getClass", "finalize", "handle", "actionPerformed", "next");
	}
}
