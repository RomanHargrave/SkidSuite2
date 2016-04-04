package me.lpk.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;

public class ParentUtils {
	/**
	 * Returns true if the method has called it's super method.
	 * 
	 * @param mn
	 * @return
	 */
	public static boolean callsSuper(MethodNode mn) {
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getOpcode() == Opcodes.INVOKESPECIAL) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (min.name.equals(mn.name)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the field in the given class with the name and description. If
	 * it's not in the given class, parents are checked. Returns null if nothing
	 * is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findMethod(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getMethods()) {
			if (matches(mm, name, desc)) {
				return mm;
			}
		}
		return findMethodParent(owner, name, desc);
	}

	/**
	 * Returns the method in the given class with the name and description. If
	 * it's not in the given class, parents are checked. Returns null if nothing
	 * is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findField(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getFields()) {
			if (matches(mm, name, desc)) {
				return mm;
			}
		}
		return findFieldInParent(owner, name, desc);
	}

	/**
	 * Returns the method in the given class's parent with the name and
	 * description. If it's not in the given class, further parents are checked.
	 * Returns null if nothing is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findMethodParent(MappedClass owner, String name, String desc) {
		// Check for interfaces in the method's class.
		for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
			MappedMember mm = findMethod(interfaceClass, name, desc);
			if (mm != null) {
				return mm;
			}
		}
		// Check the parents
		if (owner.getParent() != null) {
			MappedMember mm = findMethod(owner.getParent(), name, desc);
			if (mm != null) {
				return mm;
			}
		}
		return null;
	}

	/**
	 * Returns the field in the given class's parent with the name and
	 * description. If it's not in the given class, further parents are checked.
	 * Returns null if nothing is found.
	 * 
	 * @param owner
	 * @param name
	 * @param desc
	 * @return
	 */
	public static MappedMember findFieldInParent(MappedClass owner, String name, String desc) {
		// Check for interfaces in the field's class.
		for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
			MappedMember mm = findField(interfaceClass, name, desc);
			if (mm != null) {
				return mm;
			}
		}
		// Check the parents
		if (owner.getParent() != null) {
			MappedMember mm = findField(owner.getParent(), name, desc);
			if (mm != null) {
				return mm;
			}
		}
		return null;
	}

	/**
	 * Finds the parent-most overridden member.
	 * 
	 * @param mm
	 * @return
	 */
	public static MappedMember findMethodOverride(MappedMember mm) {
		if (mm.doesOverride()) {
			// Overridden method's parent == given method's parent.
			if (mm.getOverride().getOwner().getOriginalName().equals(mm.getOwner().getOriginalName())) {
				return mm;
			}
			return findMethodOverride(mm.getOverride());
		}
		return mm;
	}

	public static boolean matches(MappedMember mm, String name, String desc) {
		if (mm.getOriginalName().equals(name)) {
			String o = "java/lang/Object";
			if (mm.getDesc().equals(desc)) {
				return true;
			} else if (mm.getDesc().contains(o) && !desc.contains(o)) {
				// Generic info is saved in the signature so if there is data in the signature, check for generics.
				if (mm.getOwner().getNode().signature != null) {
					List<String> classes = Regexr.matchDescriptionClasses(desc);
					String descCopy = desc + "";
					for (String detection : classes) {
						descCopy = descCopy.replace(detection, o);
					}
					if (mm.getDesc().equals(descCopy)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean matches(MappedMember mm, MappedMember mm2, boolean orig) {
		return matches(mm, orig ? mm2.getOriginalName() : mm2.getNewName(), mm2.getDesc());
	}
}
