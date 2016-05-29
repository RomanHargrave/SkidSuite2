package me.lpk.analysis;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

import me.lpk.log.Logger;
import me.lpk.util.OpUtils;

/**
 * An {@link Interpreter} for {@link InsnValue} values.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 * 
 * @editor Matt
 */
public class InsnInterpreter extends Interpreter<InsnValue> implements Opcodes {
	private final Map<String, ClassNode> nodes;

	public InsnInterpreter(Map<String, ClassNode> nodes) {
		super(ASM5);
		this.nodes = nodes;
	}

	@Override
	public InsnValue newValue(final Type type) {
		if (type == null) {
			return InsnValue.UNINITIALIZED_VALUE;
		}
		switch (type.getSort()) {
		case Type.VOID:
			return null;
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.BYTE:
		case Type.SHORT:
		case Type.INT:
			return InsnValue.INT_VALUE;
		case Type.FLOAT:
			return InsnValue.FLOAT_VALUE;
		case Type.LONG:
			return InsnValue.LONG_VALUE;
		case Type.DOUBLE:
			return InsnValue.DOUBLE_VALUE;
		case Type.ARRAY:
		case Type.OBJECT:
			return new InsnValue(type);
		// InsnValue.REFERENCE_VALUE;
		default:
			throw new Error("Internal error");
		}
	}

	@Override
	public InsnValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case ACONST_NULL:
			return newValue(Type.getObjectType("null"));
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
		case BIPUSH:
		case SIPUSH:
			return InsnValue.intValue(insn);
		case LCONST_0:
		case LCONST_1:
			return InsnValue.longValue(insn.getOpcode());
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return InsnValue.floatValue(insn.getOpcode());
		case DCONST_0:
		case DCONST_1:
			return InsnValue.doubleValue(insn.getOpcode());
		case LDC:
			Object cst = ((LdcInsnNode) insn).cst;
			if (cst instanceof Integer) {
				return InsnValue.intValue(cst);
			} else if (cst instanceof Float) {
				return InsnValue.floatValue(cst);
			} else if (cst instanceof Long) {
				return InsnValue.longValue(cst);
			} else if (cst instanceof Double) {
				return InsnValue.doubleValue(cst);
			} else if (cst instanceof String) {
				return InsnValue.stringValue(cst);
			} else if (cst instanceof Type) {
				int sort = ((Type) cst).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY) {
					return newValue(Type.getObjectType("java/lang/Class"));
				} else if (sort == Type.METHOD) {
					return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
				} else {
					throw new IllegalArgumentException("Illegal LDC constant " + cst);
				}
			} else if (cst instanceof Handle) {
				return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
			} else {
				throw new IllegalArgumentException("Illegal LDC constant " + cst);
			}
		case JSR:
			return InsnValue.RETURNADDRESS_VALUE;
		case GETSTATIC:
			return doFieldStatic((FieldInsnNode) insn);
		//
		case NEW:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
		default:
			throw new Error("Internal error.");
		}
	}

	@Override
	public InsnValue copyOperation(final AbstractInsnNode insn, final InsnValue value) throws AnalyzerException {
		return value;
	}

	@Override
	public InsnValue unaryOperation(final AbstractInsnNode insn, final InsnValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case INEG:
		case IINC:
		case L2I:
		case F2I:
		case D2I:
		case I2B:
		case I2C:
		case I2S:
			return doUnaryInt(insn, value);
		case FNEG:
		case I2F:
		case L2F:
		case D2F:
			return doUnaryFloat(insn, value);
		case LNEG:
		case I2L:
		case F2L:
		case D2L:
			return doUnaryLong(insn, value);
		case DNEG:
		case I2D:
		case L2D:
		case F2D:
			return doUnaryDouble(insn, value);
		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
			return doIfLogic(((JumpInsnNode) insn), value);
		case TABLESWITCH:
			return doTableLogic(((TableSwitchInsnNode) insn), value);
		case LOOKUPSWITCH:
			return doLookupLogic(((LookupSwitchInsnNode) insn), value);
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
		case ARETURN:
		case PUTSTATIC:
			return null;
		case GETFIELD:
			FieldInsnNode fin = ((FieldInsnNode) insn);
			return newValue(Type.getType(fin.desc));
		case NEWARRAY:
			switch (((IntInsnNode) insn).operand) {
			case T_BOOLEAN:
				return newValue(Type.getType("[Z"));
			case T_CHAR:
				return newValue(Type.getType("[C"));
			case T_BYTE:
				return newValue(Type.getType("[B"));
			case T_SHORT:
				return newValue(Type.getType("[S"));
			case T_INT:
				return newValue(Type.getType("[I"));
			case T_FLOAT:
				return newValue(Type.getType("[F"));
			case T_DOUBLE:
				return newValue(Type.getType("[D"));
			case T_LONG:
				return newValue(Type.getType("[J"));
			default:
				throw new AnalyzerException(insn, "Invalid array type");
			}
		case ANEWARRAY:
			String desc = ((TypeInsnNode) insn).desc;
			return newValue(Type.getType("[" + Type.getObjectType(desc)));
		case ARRAYLENGTH:
			if (value.getValue() != null) {
				int len = getArrLen(value.getValue());
				if (len != -1) {
					return InsnValue.intValue(len);
				}
			}
			System.err.println("ARRAYLENGTH");
			return InsnValue.INT_VALUE;
		case ATHROW:
			return null;
		case CHECKCAST:
			desc = ((TypeInsnNode) insn).desc;
			return newValue(Type.getObjectType(desc));
		case INSTANCEOF:
			System.err.println("INSTANCEOF");
			return InsnValue.INT_VALUE;
		case MONITORENTER:
		case MONITOREXIT:
		case IFNULL:
		case IFNONNULL:
			return null;
		default:
			throw new Error("Internal error.");
		}
	}

	private InsnValue doTableLogic(TableSwitchInsnNode table, InsnValue value) {
		if (value.getValue() == null) {
			InsnValue.intValue(-1);
		}
		int index = ((Number) value.getValue()).intValue();
		return InsnValue.intValue(index);
	}

	private InsnValue doLookupLogic(LookupSwitchInsnNode lookup, InsnValue value) {
		if (value.getValue() == null) {
			InsnValue.intValue(-1);
		}
		int index = ((Number) value.getValue()).intValue();
		return InsnValue.intValue(index);
	}

	private InsnValue doIfLogic(JumpInsnNode jin, InsnValue value) {
		if (value.getValue() == null || !(value.getValue() instanceof Number)) {
			return InsnValue.intValue(0);
		}
		int i = ((Number) value.getValue()).intValue();
		switch (jin.getOpcode()) {
		case IFEQ:
			return InsnValue.intValue(i == 0 ? 1 : 0);
		case IFNE:
			return InsnValue.intValue(i != 0 ? 1 : 0);
		case IFLT:
			return InsnValue.intValue(i < 0 ? 1 : 0);
		case IFGE:
			return InsnValue.intValue(i >= 0 ? 1 : 0);
		case IFGT:
			return InsnValue.intValue(i > 0 ? 1 : 0);
		case IFLE:
			return InsnValue.intValue(i <= 0 ? 1 : 0);
		}
		return InsnValue.intValue(0);
	}

	private InsnValue doIfLogic(JumpInsnNode jumpInsnNode, InsnValue value1, InsnValue value2) {
		if (value1.getValue() == null || value2.getValue() == null) {
			return InsnValue.intValue(0);
		}
		if (value1.getValue() instanceof Number && value2.getValue() instanceof Number) {
			int i1 = ((Number) value1.getValue()).intValue();
			int i2 = ((Number) value2.getValue()).intValue();
			switch (jumpInsnNode.getOpcode()) {
			case IF_ICMPEQ:
				return InsnValue.intValue(i1 == i2 ? 1 : 0);
			case IF_ICMPNE:
				return InsnValue.intValue(i1 != i2 ? 1 : 0);
			case IF_ICMPLT:
				return InsnValue.intValue(i1 < i2 ? 1 : 0);
			case IF_ICMPGE:
				return InsnValue.intValue(i1 >= i2 ? 1 : 0);
			case IF_ICMPGT:
				return InsnValue.intValue(i1 > i2 ? 1 : 0);
			case IF_ICMPLE:
				return InsnValue.intValue(i1 <= i2 ? 1 : 0);
			}
		} else {
			Object o1 = value1.getValue();
			Object o2 = value2.getValue();
			switch (jumpInsnNode.getOpcode()) {
			case IF_ACMPEQ:
				return InsnValue.intValue(o1.equals(o2) ? 1 : 0);
			case IF_ACMPNE:
				return InsnValue.intValue(!o1.equals(o2) ? 1 : 0);
			}
		}
		return InsnValue.intValue(0);
	}

	private int getArrLen(Object value) {
		int ix = -1;
		Class<?> ofArray = value.getClass().getComponentType();
		if (ofArray.isPrimitive()) {
			List<Object> ar = new ArrayList<Object>();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				ar.add(Array.get(value, i));
			}
			ix = ar.toArray().length;
		} else {
			ix = ((Object[]) value).length;
		}
		return ix;
	}

	private InsnValue doUnaryDouble(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.DOUBLE_VALUE;
		}
		switch (insn.getOpcode()) {
		case DNEG:
			double d = (double) value.getValue();
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), -d);
		case I2D:
		case L2D:
		case F2D:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), (double) value.getValue());
		}
		return InsnValue.DOUBLE_VALUE;
	}

	private InsnValue doUnaryLong(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.LONG_VALUE;
		}
		switch (insn.getOpcode()) {
		case LNEG:
			long l = (long) value.getValue();
			return new InsnValue(InsnValue.LONG_VALUE.getType(), -l);
		case I2L:
		case F2L:
		case D2L:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), (long) value.getValue());
		}
		return InsnValue.LONG_VALUE;
	}

	private InsnValue doUnaryFloat(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			return InsnValue.FLOAT_VALUE;
		}
		switch (insn.getOpcode()) {
		case FNEG:
			float f = (float) value.getValue();
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), -f);
		case I2F:
		case L2F:
		case D2F:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), (float) value.getValue());
		}
		return InsnValue.FLOAT_VALUE;
	}

	private InsnValue doUnaryInt(AbstractInsnNode insn, InsnValue value) {
		if (value.getValue() == null) {
			System.err.println("FUUUGGGG: " + OpUtils.getOpcodeText(insn.getOpcode()));
			return InsnValue.INT_VALUE;
		}
		switch (insn.getOpcode()) {
		case INEG:
			int i = (int) value.getValue();
			return new InsnValue(InsnValue.INT_VALUE.getType(), -i);
		case IINC:
			int i2 = (int) value.getValue();
			IincInsnNode iinc = (IincInsnNode) insn;

			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 + iinc.incr);
		case L2I:
		case F2I:
		case D2I:
			return new InsnValue(InsnValue.INT_VALUE.getType(), value.getValue());
		case I2B:
			byte b = (byte) value.getValue();
			return new InsnValue(InsnValue.BYTE_VALUE.getType(), b);
		case I2S:
			int s = (int) value.getValue();
			return InsnValue.intValue(s);
		case I2C:
			char c = (char) ((Number) value.getValue()).intValue();
			return new InsnValue(InsnValue.CHAR_VALUE.getType(), c);

		}
		System.err.println("doUnaryInt: FAILSAFE");
		return InsnValue.INT_VALUE;
	}

	@Override
	public InsnValue binaryOperation(final AbstractInsnNode insn, final InsnValue value1, final InsnValue value2) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			return new InsnValue(InsnValue.INT_VALUE.getType(), value1.getValue() == value2.getValue() ? 1 : 0);
		case IALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
		case DALOAD:
		case FALOAD:
		case LALOAD:
		case AALOAD:
			// Value 1 = array
			// Value 2 = index
			int index = (int) value2.getValue();
			return loadFromArray(insn.getOpcode(), value1, index);
		case IADD:
		case ISUB:
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
		case IAND:
		case IOR:
		case IXOR:

			int i1 = 0;
			int i2 = 0;
			if (value1.getValue() instanceof Number) {
				i1 = (int) value1.getValue();
			} else if (value1.getValue() instanceof Character) {
				i1 = ((Character) value1.getValue()).charValue();
			} else {
				System.err.println("MATH BUT NOT A INT --> " + value1.getValue().toString());
				return InsnValue.INT_VALUE;
			}
			if (value2.getValue() instanceof Number) {
				i2 = (int) value2.getValue();
			} else if (value2.getValue() instanceof Character) {
				i2 = ((Character) value2.getValue()).charValue();
			} else {
				System.err.println("MATH BUT NOT A INT --> " + value2.getValue().toString());
				return InsnValue.INT_VALUE;
			}
			return doBinaryInt(insn, i1, i2);
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
			if (!(value1.getValue() instanceof Float) || !(value2.getValue() instanceof Float)) {
				return InsnValue.FLOAT_VALUE;
			}
			float f1 = (float) value1.getValue();
			float f2 = (float) value2.getValue();
			return doBinaryFloat(insn, f1, f2);
		case LADD:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LSHL:
		case LSHR:
		case LUSHR:
		case LAND:
		case LOR:
		case LXOR:
			if (!(value1.getValue() instanceof Long) || !(value2.getValue() instanceof Long)) {
				return InsnValue.LONG_VALUE;
			}
			long l1 = (long) value1.getValue();
			long l2 = (long) value2.getValue();
			return doBinaryLong(insn, l1, l2);
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			if (!(value1.getValue() instanceof Double) || !(value2.getValue() instanceof Double)) {
				return InsnValue.DOUBLE_VALUE;
			}
			double d1 = (double) value1.getValue();
			double d2 = (double) value2.getValue();
			return doBinaryDouble(insn, d1, d2);
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			return doIfLogic(((JumpInsnNode) insn), value1, value2);
		case PUTFIELD:
			return null;
		default:
			throw new Error("Internal error.");
		}
	}

	private InsnValue loadFromArray(int i, InsnValue value1, int index) {
		if (value1.getValue() == null) {
			return InsnValue.REFERENCE_VALUE;
		}
		switch (i) {
		case IALOAD:
			int[] ia = (int[]) value1.getValue();
			return InsnValue.intValue(ia[index]);
		case CALOAD:
			char[] ca = (char[]) value1.getValue();
			return new InsnValue(InsnValue.CHAR_VALUE.getType(), ca[index]);
		case SALOAD:
			int[] sa = (int[]) value1.getValue();
			return InsnValue.intValue(sa[index]);
		case DALOAD:
			double[] da = (double[]) value1.getValue();
			return InsnValue.doubleValue(da[index]);
		case FALOAD:
			float[] fa = (float[]) value1.getValue();
			return InsnValue.floatValue(fa[index]);
		case LALOAD:
			long[] la = (long[]) value1.getValue();
			return InsnValue.longValue(la[index]);
		case BALOAD:
		case AALOAD:
			break;
		}
		return InsnValue.REFERENCE_VALUE;
	}

	private InsnValue doBinaryDouble(AbstractInsnNode insn, double d1, double d2) {
		switch (insn.getOpcode()) {
		case DADD:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 + d1);
		case DSUB:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 - d1);
		case DMUL:
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 * d1);
		case DDIV:
			if (d1 == 0L) {
				return InsnValue.DOUBLE_VALUE;
			}
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 / d1);
		case DREM:
			if (d1 == 0L) {
				return InsnValue.DOUBLE_VALUE;
			}
			return new InsnValue(InsnValue.DOUBLE_VALUE.getType(), d2 % d1);
		}
		return InsnValue.DOUBLE_VALUE;
	}

	private InsnValue doBinaryLong(AbstractInsnNode insn, long l1, long l2) {
		switch (insn.getOpcode()) {
		case LADD:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 + l1);
		case LSUB:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 - l1);
		case LMUL:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 * l1);
		case LDIV:
			if (l1 == 0L) {
				return InsnValue.LONG_VALUE;
			}
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 / l1);
		case LREM:
			if (l1 == 0L) {
				return InsnValue.LONG_VALUE;
			}
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 % l1);
		case LSHL:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 << l1);
		case LSHR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 >> l1);
		case LUSHR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 >>> l1);
		case LAND:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 & l1);
		case LOR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 | l1);
		case LXOR:
			return new InsnValue(InsnValue.LONG_VALUE.getType(), l2 ^ l1);
		}
		return InsnValue.LONG_VALUE;
	}

	private InsnValue doBinaryFloat(AbstractInsnNode insn, float f1, float f2) {
		switch (insn.getOpcode()) {
		case FADD:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 + f1);
		case FSUB:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 - f1);
		case FMUL:
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 * f1);
		case FDIV:
			if (f1 == 0f) {
				return InsnValue.FLOAT_VALUE;
			}
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 / f1);
		case FREM:
			if (f1 == 0f) {
				return InsnValue.FLOAT_VALUE;
			}
			return new InsnValue(InsnValue.FLOAT_VALUE.getType(), f2 % f1);
		}
		return InsnValue.FLOAT_VALUE;
	}

	private InsnValue doBinaryInt(AbstractInsnNode insn, int i1, int i2) {
		switch (insn.getOpcode()) {
		case IADD:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 + i1);
		case ISUB:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 - i1);
		case IMUL:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 * i1);
		case IDIV:
			if (i1 == 0) {
				System.err.println("IDIV BY ZERO");
				return InsnValue.INT_VALUE;
			}
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 / i1);
		case IREM:
			if (i1 == 0) {
				System.err.println("IREM BY ZERO");
				return InsnValue.INT_VALUE;
			}
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 % i1);
		case ISHL:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 << i1);
		case ISHR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 >> i1);
		case IUSHR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 >>> i1);
		case IAND:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 & i1);
		case IOR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 | i1);
		case IXOR:
			return new InsnValue(InsnValue.INT_VALUE.getType(), i2 ^ i1);
		}
		System.err.println("doBinaryInt: FAILSAFE");
		return InsnValue.INT_VALUE;
	}

	@Override
	public InsnValue ternaryOperation(final AbstractInsnNode insn, final InsnValue arrayRef, final InsnValue index, final InsnValue value) throws AnalyzerException {
		if (arrayRef.getValue() == null) {
			System.err.println("ArrayRef null");
			return null;
		}
		if (index.getValue() == null) {
			System.err.println("ArrayIndex null");
			return null;
		}
		if (value.getValue() == null) {
			System.err.println("ArrayNewValue null");
			return null;
		}
		int i = (int) index.getValue();
		switch (insn.getOpcode()) {
		case Opcodes.IASTORE:
			int[] ia = (int[]) arrayRef.getValue();
			ia[i] = (int) value.getValue();
			return new InsnValue(InsnValue.INT_ARR_VALUE.getType(), ia);
		case Opcodes.LASTORE:
			long[] la = (long[]) arrayRef.getValue();
			la[i] = (long) value.getValue();
			return new InsnValue(InsnValue.LONG_ARR_VALUE.getType(), la);
		case Opcodes.FASTORE:
			float[] fa = (float[]) arrayRef.getValue();
			fa[i] = (float) value.getValue();
			return new InsnValue(InsnValue.FLOAT_ARR_VALUE.getType(), fa);
		case Opcodes.DASTORE:
			double[] da = (double[]) arrayRef.getValue();
			da[i] = (double) value.getValue();
			return new InsnValue(InsnValue.DOUBLE_ARR_VALUE.getType(), da);
		case Opcodes.AASTORE:
			// Object[] aa = (Object[]) arrayRef.getValue();
			//
			// aa[i] = value.getValue();
			//
			// return new InsnValue(InsnValue.(Find the type).getType(), ca);
			break;
		case Opcodes.BASTORE:
			// byte or Boolean
			break;
		case Opcodes.CASTORE:
			char[] ca = (char[]) arrayRef.getValue();
			ca[i] = (char) value.getValue();
			return new InsnValue(InsnValue.CHAR_ARR_VALUE.getType(), ca);
		case Opcodes.SASTORE:
			// short. Should handle as boolean?
			// int[] sa = (int[]) arrayRef.getValue();
			break;
		}
		return null;
	}

	@Override
	public InsnValue naryOperation(final AbstractInsnNode insn, final List<? extends InsnValue> values) throws AnalyzerException {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
		} else if (opcode == INVOKEDYNAMIC) {
			return newValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
		} else {
			MethodInsnNode min = ((MethodInsnNode) insn);
			InsnValue retVal = newValue(Type.getReturnType(min.desc));
			if (nodes == null || min.desc.endsWith("V")) {
				return retVal;
			} else {
				ClassNode node = nodes.get(min.owner);
				if (node == null) {
					InsnValue val = doMethod(min, values);
					if (val == null) {
						return retVal;
					}
					return val;
				}
				MethodNode methn = null;
				for (MethodNode mn : node.methods) {
					if (mn.name.equals(min.name) && mn.desc.equals(min.desc)) {
						methn = mn;
					}
				}
				if (methn == null) {
					return retVal;
				}
				Logger.logHigh("Emulating: " + node.name + "#" + methn.name + methn.desc);
				List<? extends InsnValue> list = values.subList(values.size() - Type.getArgumentTypes(methn.desc).length, values.size());
				int parameterIndex = 0;
				for (InsnValue value : list){
					if (value.getValue() == null){
						Logger.logHigh("Emulation Aborted, Parameter["+parameterIndex+"] without a value: " + value.toString());
						return retVal;
					}
					parameterIndex++;
				}
				InsnValue ret = (InsnValue) InsnHandler.getFrameExec(methn, nodes, list).pop();
				if (ret == null) {
					Logger.errHigh("Emulation Failed!");
					return retVal;
				}
				Logger.logHigh("Emulation Returned: " + ret.toString());
				return ret;
			}

		}
	}

	private InsnValue doMethod(MethodInsnNode min, List<? extends InsnValue> values) {
		if (values.size() == 0) {
			return null;
		}
		InsnValue top = values.get(0);
		if (top.getValue() == null) {
			return null;
		}
		Logger.logHigh("\tInvoking: " + min.owner + "#" + min.name + min.desc);
		if (min.owner.equals("java/lang/String")) {
			if (min.name.equals("intern")) {
				return InsnValue.stringValue(top.getValue());
			} else if (min.name.equals("toCharArray")) {
				return new InsnValue(InsnValue.CHAR_ARR_VALUE.getType(), ((String) top.getValue()).toCharArray());
			} else if (min.name.equals("valueOf")) {
				if (min.desc.equals("([C)Ljava/lang/String;")) {
					return InsnValue.stringValue(String.valueOf(top));
				} else if (min.desc.equals("([CII)Ljava/lang/String;")) {
					if (values.size() < 3) {
						return null;
					}
					InsnValue second = values.get(1);
					InsnValue third = values.get(2);
					if (second.getValue() == null || third.getValue() == null) {
						return null;
					}
					return InsnValue.stringValue(String.copyValueOf((char[]) top.getValue(), (int) second.getValue(), (int) third.getValue()));
				}
				// Different desc's
				// return InsnValue.stringValue(o);
			} else if (min.name.equals("length")) {
				return InsnValue.intValue(top.getValue().toString().length());
			}
		}
		return null;
	}

	private InsnValue doFieldStatic(FieldInsnNode insn) {
		//Logger.logVeryHigh("\tGETSTATIC: " + insn.owner + ":" + insn.name + ":" + insn.desc);
		return newValue(Type.getType(insn.desc));
	}

	@Override
	public void returnOperation(final AbstractInsnNode insn, final InsnValue value, final InsnValue expected) throws AnalyzerException {
	}

	@Override
	public InsnValue merge(final InsnValue v, final InsnValue w) {
		if (!v.equals(w)) {
			return InsnValue.UNINITIALIZED_VALUE;
		}
		return v;
	}
}
