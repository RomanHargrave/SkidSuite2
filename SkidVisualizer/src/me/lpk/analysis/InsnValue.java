package me.lpk.analysis;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

import org.objectweb.asm.Opcodes;

import me.lpk.util.OpUtil;

/**
 * A {@link Value} that is represented by its type in a seven types type system.
 * This type system distinguishes the UNINITIALZED, INT, FLOAT, LONG, DOUBLE,
 * REFERENCE and RETURNADDRESS types.
 * 
 * @author Eric Bruneton
 * @editor Matt
 */
public class InsnValue implements Value {

	public static final InsnValue UNINITIALIZED_VALUE = new InsnValue(null);

	public static final InsnValue INT_VALUE = new InsnValue(Type.INT_TYPE);

	public static final InsnValue FLOAT_VALUE = new InsnValue(Type.FLOAT_TYPE);

	public static final InsnValue LONG_VALUE = new InsnValue(Type.LONG_TYPE);

	public static final InsnValue DOUBLE_VALUE = new InsnValue(Type.DOUBLE_TYPE);

	public static final InsnValue REFERENCE_VALUE = new InsnValue(Type.getObjectType("java/lang/Object"));

	public static final InsnValue RETURNADDRESS_VALUE = new InsnValue(Type.VOID_TYPE);

	private final Type type;
	private final Object value;

	public InsnValue(final Type type) {
		this(type, null);
	}

	public InsnValue(final Type type, final Object value) {
		this.type = type;
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public int getSize() {
		return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
	}

	public boolean isReference() {
		return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
	}

	@Override
	public boolean equals(final Object value) {
		if (value == this) {
			return true;
		} else if (value instanceof InsnValue) {
			if (type == null) {
				return ((InsnValue) value).type == null;
			} else {
				return type.equals(((InsnValue) value).type);
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return type == null ? 0 : type.hashCode();
	}

	@Override
	public String toString() {
		if (value != null) {
			return type.getDescriptor() + " " + value;
		}
		if (this == UNINITIALIZED_VALUE) {
			return "Uninitialized Null";
		} else if (this == RETURNADDRESS_VALUE) {
			return "Return Address";
		} else if (this == REFERENCE_VALUE) {
			return "Misc. Ref Value";
		} else {
			return type.getDescriptor();
		}
	}

	public static InsnValue intValue(AbstractInsnNode opcode) {
		return new InsnValue(Type.INT_TYPE, OpUtil.getIntValue(opcode));
	}

	public static InsnValue longValue(int opcode) {
		switch (opcode) {
		case Opcodes.LCONST_0:
			return new InsnValue(Type.LONG_TYPE, 0L);
		case Opcodes.LCONST_1:
			return new InsnValue(Type.LONG_TYPE, 1L);
		}
		return InsnValue.LONG_VALUE;
	}

	public static InsnValue doubleValue(int opcode) {
		switch (opcode) {
		case Opcodes.DCONST_0:
			return new InsnValue(Type.DOUBLE_TYPE, 0.0);
		case Opcodes.DCONST_1:
			return new InsnValue(Type.DOUBLE_TYPE, 1.0);
		}
		return InsnValue.DOUBLE_VALUE;
	}

	public static InsnValue floatValue(int opcode) {
		switch (opcode) {
		case Opcodes.FCONST_0:
			return new InsnValue(Type.FLOAT_TYPE, 0.0f);
		case Opcodes.FCONST_1:
			return new InsnValue(Type.FLOAT_TYPE, 1.0f);
		case Opcodes.FCONST_2:
			return new InsnValue(Type.FLOAT_TYPE, 2.0f);
		}
		return InsnValue.FLOAT_VALUE;
	}

	public static InsnValue intValue(Object cst) {
		return new InsnValue(Type.INT_TYPE, cst);
	}

	public static InsnValue longValue(Object cst) {
		return new InsnValue(Type.LONG_TYPE, cst);
	}

	public static InsnValue doubleValue(Object cst) {
		return new InsnValue(Type.DOUBLE_TYPE, cst);
	}

	public static InsnValue floatValue(Object cst) {
		return new InsnValue(Type.FLOAT_TYPE, cst);
	}

	public static InsnValue stringValue(Object cst) {
		return new InsnValue(Type.getType(String.class), cst);
	}
}
