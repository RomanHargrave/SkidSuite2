package me.lpk.mapping;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class MappedMember extends MappedObject {
	/**
	 * The member's owner.
	 */
	private final MappedClass owner;
	/**
	 * The FieldNode or MethodNode of the current MappedMember.
	 */
	private final Object memberNode;
	/**
	 * The index in the owner that the current member appears in.
	 */
	private final int index;
	/**
	 * The MappedMember (Method) which this member overrides.
	 */
	private MappedMember override;

	public MappedMember(MappedClass owner, Object memberNode, int index, String desc, String nameOriginal) {
		super(desc, nameOriginal, nameOriginal);
		this.memberNode = memberNode;
		this.owner = owner;
		this.index = index;
	}

	/**
	 * The class the member belongs to.
	 * 
	 * @return
	 */
	public MappedClass getOwner() {
		return owner;
	}

	/**
	 * The order in which the member was indexed (Ex: One field after another)
	 * 
	 * @return
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Returns the node of the member. Can be a Field/MethodNode.
	 * 
	 * @return
	 */
	public Object getMemberNode() {
		// EnigmaLoader makes this null.
		return memberNode;
	}

	/**
	 * Returns the node of the member as a FieldNode.
	 * 
	 * @return
	 */
	public FieldNode getFieldNode() {
		return (FieldNode) memberNode;
	}

	/**
	 * Returns the node of the member as a MethodNode.
	 * 
	 * @return
	 */
	public MethodNode getMethodNode() {
		return (MethodNode) memberNode;
	}

	/**
	 * Returns true if the memberNode is a FieldNode.
	 * 
	 * @return
	 */
	public boolean isField() {
		return memberNode == null ? false : memberNode instanceof FieldNode;
	}

	/**
	 * Returns true if the memberNode is a MethodNode.
	 * 
	 * @return
	 */
	public boolean isMethod() {
		return memberNode == null ? false : memberNode instanceof MethodNode;
	}

	/**
	 * Returns true if the member has an override.
	 * 
	 * @return
	 */
	public boolean doesOverride() {
		return override != null;
	}

	/**
	 * Gets the MappedMember object of the overridden (method) member. May be
	 * null.
	 * 
	 * @return
	 */
	public MappedMember getOverride() {
		return override;
	}

	/**
	 * Sets the overridden (method) member object.
	 * 
	 * @param override
	 */
	public void setOverride(MappedMember override) {
		this.override = override;
	}

}
