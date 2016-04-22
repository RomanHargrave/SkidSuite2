package me.lpk.analysis;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class InsnHandler {
	public static InsnFrame[] getFrames(MethodNode mn) {
		try {
			return makeAnalyzer().analyze(mn.owner, mn);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InsnFrame[] getFrames(Map<String, ClassNode> nodes, String owner, String name, String desc) {
		try {
			ClassNode cn = nodes.get(owner);
			if (cn == null) {
				return new InsnFrame[] {};
			}
			for (MethodNode mn : cn.methods) {
				if (mn.name.equals(name) && mn.desc.equals(desc)) {
					return makeAnalyzer().analyze(owner, mn);
				}
			}
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return new InsnFrame[] {};
	}

	@SuppressWarnings({ "rawtypes" })
	private static InsnAnalyzer makeAnalyzer() {
		return new InsnAnalyzer(new InsnInterpreter());
	}
}
