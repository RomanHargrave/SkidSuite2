package me.lpk.analysis;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import me.lpk.log.Logger;
import me.lpk.util.AccessHelper;
import me.lpk.util.OpUtils;

public class StandaloneAnalyzer {
	private final InsnInterpreter interpreter;

	public StandaloneAnalyzer(InsnInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	/**
	 * Simulates a method.
	 * 
	 * @param owner
	 * @param m
	 * @param list
	 * @return
	 * @throws AnalyzerException
	 */
	public InsnFrame analyze(String owner, MethodNode m, List<? extends InsnValue> list) throws AnalyzerException {
		return analyzeUntil(owner, m, list, null);
	}

	/**
	 * Simulates a method up until a given Insn.
	 * 
	 * @param owner
	 * @param m
	 * @param list
	 * @param end
	 * @return
	 * @throws AnalyzerException
	 */
	public InsnFrame analyzeUntil(String owner, MethodNode m, List<? extends InsnValue> list, AbstractInsnNode end) throws AnalyzerException {
		InsnFrame frame = setupFrame(owner, m, list);
		if (m == null) {
			return frame;
		}
		AbstractInsnNode ain = m.instructions.getFirst();
		while (ain.getNext() != null) {
			if (ain == end) {
				break;
			}
			frame = analyzeInsn(frame, ain);
			if (frame.doJump) {
				int curIndex = OpUtils.getIndex(ain);
				int labelIndex = OpUtils.getIndex(frame.jin);
				ain = m.instructions.get(labelIndex);
				frame.doJump = false;
				Logger.logVeryHigh("Jumping[" + OpUtils.getOpcodeText(ain.getOpcode()) + "]: " + curIndex + " --> " + labelIndex);
			} else {
				ain = ain.getNext();
			}
		}
		return frame;
	}

	/**
	 * Simulates the given insn in a given frame.
	 * 
	 * @param frame
	 * @param ain
	 * @return
	 * @throws AnalyzerException
	 */
	public InsnFrame analyzeInsn(InsnFrame frame, AbstractInsnNode ain) throws AnalyzerException {
		if (ain.getType() == AbstractInsnNode.FRAME || ain.getType() == AbstractInsnNode.LABEL || ain.getType() == AbstractInsnNode.LINE) {

		} else {
			frame.execute(ain, interpreter);
		}
		return frame;
	}

	/**
	 * Creates a frame given information about a method.
	 * 
	 * @param owner
	 * @param m
	 * @return
	 * @throws AnalyzerException
	 */
	public InsnFrame setupFrame(String owner, MethodNode m) {
		return setupFrame(owner, m, new ArrayList<InsnValue>());
	}

	/**
	 * Creates a frame given information about a method.
	 * 
	 * @param owner
	 * @param m
	 * @param list
	 * @return
	 * @throws AnalyzerException
	 */
	@SuppressWarnings("unchecked")
	public InsnFrame setupFrame(String owner, MethodNode m, List<? extends InsnValue> list) {
		if (AccessHelper.isAbstract(m.access) || AccessHelper.isNative(m.access)) {
			return new InsnFrame(0, 0);
		}
		// + 1 stack fixes some crashes... Gotta figure out what causes it to
		// crash otherwise and fix that,
		//
		InsnFrame current = new InsnFrame(m.maxLocals, m.maxStack + 1);
		current.setReturn(interpreter.newValue(Type.getReturnType(m.desc)));
		Type[] args = Type.getArgumentTypes(m.desc);
		int local = 0;
		if (!AccessHelper.isStatic(m.access)) {
			Type ctype = Type.getObjectType(owner);
			current.setLocal(local++, interpreter.newValue(ctype));
		}
		for (int i = 0; i < args.length; ++i) {
			if (list != null && i < list.size()) {
				current.setLocal(local++, list.get(i));
			} else {
				current.setLocal(local++, interpreter.newValue(args[i]));
			}
			if (args[i].getSize() == 2) {
				current.setLocal(local++, interpreter.newValue(null));
			}
		}
		while (local < m.maxLocals) {
			current.setLocal(local++, interpreter.newValue(null));
		}
		return current;
	}
}
