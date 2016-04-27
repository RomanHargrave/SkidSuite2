package me.lpk.analysis;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import me.lpk.analysis.execution.ExecutionAnalyzer;
import me.lpk.log.Logger;
import me.lpk.util.OpUtil;

public class InsnHandler {
	public static InsnFrame[] getFrames(MethodNode mn, Map<String, ClassNode> nodes, List<? extends InsnValue> list ) {
		try {
			return makeAnalyzer(nodes).analyze(mn.owner, mn, list);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
		try {
			return makeExecAnalyzer(nodes).analyze(mn.owner, mn, list);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static InsnFrame[] getFrames(MethodNode mn, Map<String, ClassNode> nodes) {
		try {
			return makeAnalyzer(nodes).analyze(mn.owner, mn, null);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InsnFrame[] getFrames(MethodNode mn) {
		try {
			return makeAnalyzer(null).analyze(mn.owner, mn);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InsnFrame[] getFrames(Map<String, ClassNode> nodes, String owner, String name, String desc) {
		ClassNode cn = nodes.get(owner);
		if (cn == null) {
			return new InsnFrame[] {};
		}
		for (MethodNode mn : cn.methods) {
			if (mn.name.equals(name) && mn.desc.equals(desc)) {
				return getFrames(mn, nodes);
			}
		}
		return new InsnFrame[] {};
	}

	@SuppressWarnings({ "rawtypes" })
	private static InsnAnalyzer<?> makeAnalyzer(Map<String, ClassNode> nodes) {
		return new InsnAnalyzer(new InsnInterpreter(nodes));
	}
	@SuppressWarnings({ "rawtypes" })
	private static ExecutionAnalyzer<?> makeExecAnalyzer(Map<String, ClassNode> nodes) {
		return new ExecutionAnalyzer(new InsnInterpreter(nodes));
	}

	public static InsnValue getReturn(InsnFrame[] frames) {
		if (frames == null) {
			return null;
		}
		InsnFrame retFrame = null;
		for (InsnFrame frame : frames) {
			if (frame == null || frame.ain == null){
				continue;
			}
			if (frame.ain.getOpcode() == Opcodes.RET || frame.ain.getOpcode() == Opcodes.ARETURN || frame.ain.getOpcode() == Opcodes.DRETURN
					|| frame.ain.getOpcode() == Opcodes.FRETURN || frame.ain.getOpcode() == Opcodes.RETURN || frame.ain.getOpcode() == Opcodes.LRETURN
					|| frame.ain.getOpcode() == Opcodes.IRETURN) {
				retFrame = frame;
				
			}
		}
		if (retFrame == null) {
			Logger.errLow("Could not find frame with returnable value!");
			return null;
		}
		return (InsnValue) retFrame.pop();
	}

	
}
