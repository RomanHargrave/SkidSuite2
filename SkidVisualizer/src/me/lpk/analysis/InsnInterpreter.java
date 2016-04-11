package me.lpk.analysis;

import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

/**
 * An {@link Interpreter} for {@link InsnValue} values.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 * 
 * @editor Matt
 */
public class InsnInterpreter extends Interpreter<InsnValue> implements
        Opcodes {

    public InsnInterpreter() {
        super(ASM5);
    }

    protected InsnInterpreter(final int api) {
        super(api);
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
            return  new InsnValue(type);
            		//InsnValue.REFERENCE_VALUE;
        default:
            throw new Error("Internal error");
        }
    }

    @Override
    public InsnValue newOperation(final AbstractInsnNode insn)
            throws AnalyzerException {
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
                    return newValue(Type
                            .getObjectType("java/lang/invoke/MethodType"));
                } else {
                    throw new IllegalArgumentException("Illegal LDC constant "
                            + cst);
                }
            } else if (cst instanceof Handle) {
                return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
            } else {
                throw new IllegalArgumentException("Illegal LDC constant " + cst);
            }
        case JSR:
            return InsnValue.RETURNADDRESS_VALUE;
        case GETSTATIC:
            return newValue(Type.getType(((FieldInsnNode) insn).desc));
        case NEW:
            return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public InsnValue copyOperation(final AbstractInsnNode insn,
            final InsnValue value) throws AnalyzerException {
        return value;
    }

    @Override
    public InsnValue unaryOperation(final AbstractInsnNode insn,
            final InsnValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
        case INEG:
        case IINC:
        case L2I:
        case F2I:
        case D2I:
        case I2B:
        case I2C:
        case I2S:
            return InsnValue.INT_VALUE;
        case FNEG:
        case I2F:
        case L2F:
        case D2F:
            return InsnValue.FLOAT_VALUE;
        case LNEG:
        case I2L:
        case F2L:
        case D2L:
            return InsnValue.LONG_VALUE;
        case DNEG:
        case I2D:
        case L2D:
        case F2D:
            return InsnValue.DOUBLE_VALUE;
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case TABLESWITCH:
        case LOOKUPSWITCH:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case PUTSTATIC:
            return null;
        case GETFIELD:
            return newValue(Type.getType(((FieldInsnNode) insn).desc));
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
            return InsnValue.INT_VALUE;
        case ATHROW:
            return null;
        case CHECKCAST:
            desc = ((TypeInsnNode) insn).desc;
            return newValue(Type.getObjectType(desc));
        case INSTANCEOF:
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

    @Override
    public InsnValue binaryOperation(final AbstractInsnNode insn,
            final InsnValue value1, final InsnValue value2)
            throws AnalyzerException {
        switch (insn.getOpcode()) {
        case IALOAD:
        case BALOAD:
        case CALOAD:
        case SALOAD:
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
            return InsnValue.INT_VALUE;
        case FALOAD:
        case FADD:
        case FSUB:
        case FMUL:
        case FDIV:
        case FREM:
            return InsnValue.FLOAT_VALUE;
        case LALOAD:
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
            return InsnValue.LONG_VALUE;
        case DALOAD:
        case DADD:
        case DSUB:
        case DMUL:
        case DDIV:
        case DREM:
            return InsnValue.DOUBLE_VALUE;
        case AALOAD:
            return InsnValue.REFERENCE_VALUE;
        case LCMP:
        case FCMPL:
        case FCMPG:
        case DCMPL:
        case DCMPG:
            return InsnValue.INT_VALUE;
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
        case PUTFIELD:
            return null;
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public InsnValue ternaryOperation(final AbstractInsnNode insn,
            final InsnValue value1, final InsnValue value2,
            final InsnValue value3) throws AnalyzerException {
        return null;
    }

    @Override
    public InsnValue naryOperation(final AbstractInsnNode insn,
            final List<? extends InsnValue> values) throws AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
        } else if (opcode == INVOKEDYNAMIC) {
            return newValue(Type
                    .getReturnType(((InvokeDynamicInsnNode) insn).desc));
        } else {
            return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
        }
    }

    @Override
    public void returnOperation(final AbstractInsnNode insn,
            final InsnValue value, final InsnValue expected)
            throws AnalyzerException {
    }

    @Override
    public InsnValue merge(final InsnValue v, final InsnValue w) {
        if (!v.equals(w)) {
            return InsnValue.UNINITIALIZED_VALUE;
        }
        return v;
    }
}
