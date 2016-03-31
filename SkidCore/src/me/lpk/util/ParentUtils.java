package me.lpk.util;

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

	public static MappedMember findMethod(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getMethods()) {
			if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
				return mm;
			}
		}
		return findMethodParent(owner,name,desc);
	}
	
	public static MappedMember findMethodParent(MappedClass owner, String name, String desc) {
		// Check for interfaces in the method's class.
		for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
			for (MappedMember mm : interfaceClass.getMethods()) {
				if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
					return mm;
				}
			}
		}
		// No match so far. Check in the parent.
		owner = owner.getParent();
		while (owner != null) {
			// Check interfaces of parent.
			for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
				for (MappedMember mm : interfaceClass.getMethods()) {
					if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
						return mm;
					}
				}
			}
			// Check members of the parent.
			for (MappedMember mm : owner.getMethods()) {
				if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
					return mm;
				}
			}
			owner = owner.getParent();
		}
		return null;
	}

	public static MappedMember findField(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getFields()) {
			if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
				return mm;
			}
		}
		return findFieldInParent(owner, name, desc);
	}

	public static MappedMember findFieldInParent(MappedClass owner, String name, String desc) {
		// Check the class itself
		for (MappedMember mm : owner.getFields()) {
			if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
				return mm;
			}
		}
		// Check for interfaces in the method's class.
		for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
			for (MappedMember mm : interfaceClass.getFields()) {
				if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
					return mm;
				}
			}
		}
		// No match so far. Check in the parent.
		owner = owner.getParent();
		while (owner != null) {
			// Check interfaces of parent.
			for (MappedClass interfaceClass : owner.getInterfacesMap().values()) {
				for (MappedMember mm : interfaceClass.getFields()) {
					if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
						return mm;
					}
				}
			}
			// Check members of the parent.
			for (MappedMember mm : owner.getFields()) {
				if (mm.getOriginalName().equals(name) && mm.getDesc().equals(desc)) {
					return mm;
				}
			}
			owner = owner.getParent();
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
			if (mm.getOverride().getOwner().getOriginalName().equals(mm.getOwner().getOriginalName())){
				System.err.println("findMethodOverride");
				return mm;
			}
			return findMethodOverride(mm.getOverride());
		}
		return mm;
	}
}
