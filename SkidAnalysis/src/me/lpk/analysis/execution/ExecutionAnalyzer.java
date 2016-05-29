package me.lpk.analysis.execution;

import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

import me.lpk.analysis.*;
import me.lpk.log.Logger;
import me.lpk.util.OpUtils;

/**
 * Used to emulate the stack of a method. 
 * @author Matt
 */
public class ExecutionAnalyzer<V extends Value> implements Opcodes {
	private final InsnInterpreter interpreter;

	/**
	 * Constructs a new {@link ExecutionAnalyzer}.
	 * 
	 * @param interpreter
	 *            the interpreter to be used to symbolically interpret the
	 *            bytecode instructions.
	 */
	public ExecutionAnalyzer(final InsnInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	/**
	 * Analyzes the given method.
	 * 
	 * @param owner
	 *            the internal name of the class to which the method belongs.
	 * @param m
	 *            the method to be analyzed.
	 * @param list
	 *            the parameter initial values.
	 * @return the symbolic state of the execution stack frame at each bytecode
	 *         instruction of the method. The size of the returned array is
	 *         equal to the number of instructions (and labels) of the method. A
	 *         given frame is <tt>null</tt> if and only if the corresponding
	 *         instruction cannot be reached (dead code).
	 * @throws AnalyzerException
	 *             if a problem occurs during the analysis.
	 */
	@SuppressWarnings("unchecked")
	public InsnFrame analyze(final String owner, final MethodNode m, List<? extends InsnValue> list) throws AnalyzerException {
		if ((m.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
			return new InsnFrame(0,0);
		}
		// initializes the data structures for the control flow analysis
		InsnFrame current = new InsnFrame(m.maxLocals, m.maxStack);
		current.setReturn(interpreter.newValue(Type.getReturnType(m.desc)));
		Type[] args = Type.getArgumentTypes(m.desc);
		int local = 0;
		if ((m.access & ACC_STATIC) == 0) {
			Type ctype = Type.getObjectType(owner);
			current.setLocal(local++, interpreter.newValue(ctype));
		}
		for (int i = 0; i < args.length; ++i) {
			if (list != null && i < list.size()) {
				InsnValue value = list.get(i);
				current.setLocal(local++, value);
				Logger.logMedium("Set Local[" + i + "] to (From Param): " + value.toString());
			} else {
				InsnValue value = interpreter.newValue(args[i]);
				current.setLocal(local++, value);
				Logger.logMedium("Set Local[" + i + "] to (From Descr): " + value.toString());
			}
			if (args[i].getSize() == 2) {
				current.setLocal(local++, interpreter.newValue(null));
			}
		}
		while (local < m.maxLocals) {
			current.setLocal(local++, interpreter.newValue(null));
		}
		AbstractInsnNode insn = m.instructions.get(0);
		while (insn.getNext() != null) {
			int insnType = insn.getType();
			if (insnType == AbstractInsnNode.LABEL || insnType == AbstractInsnNode.LINE || insnType == AbstractInsnNode.FRAME) {

			} else {
				current.execute(insn, interpreter);
			}
			insn = insn.getNext();
			if (current.doJump) {
				int curIndex = OpUtils.getIndex(insn);
				int labelIndex = OpUtils.getIndex(current.jin);
				insn = m.instructions.get(labelIndex);
				current.doJump = false;
				Logger.logVeryHigh("Jumping[" + OpUtils.getOpcodeText(insn.getOpcode()) + "]: " + curIndex + " --> " + labelIndex);
			}
		}
		current.ain = new InsnNode(Opcodes.RETURN);
		return current;
	}
}