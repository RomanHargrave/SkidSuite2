package me.lpk.antis.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.analysis.Sandbox;
import me.lpk.antis.AntiBase;
import me.lpk.util.OpUtils;

public class AntiStringer extends AntiBase {
	/**
	 * I'm only 'kinda' sure this was tested on actual stringer jars. It looked
	 * like it but there was 0 confirmation.
	 * 
	 * Oh well! I'll fix it later if it isn't.
	 * 
	 * @param nodes
	 */
	public AntiStringer(Map<String, ClassNode> nodes) {
		super(nodes);
	}

	@Override
	public ClassNode scan(ClassNode node) {
		for (MethodNode mnode : node.methods) {
			replace(mnode);
		}
		return node;
	}

	private void replace(MethodNode method) {
		AbstractInsnNode ain = method.instructions.getFirst();
		List<String> strings = new ArrayList<String>();
		List<Integer> argSizes = new ArrayList<Integer>();
		List<Integer> indecies = new ArrayList<Integer>();
		while (ain != null) {
			if (ain.getPrevious() != null && ain.getPrevious().getType() == AbstractInsnNode.LDC_INSN && ain.getOpcode() == Opcodes.INVOKESTATIC) {
				String desc = ((MethodInsnNode) ain).desc;
				if (isStringerDesc(desc)) {
					MethodInsnNode min = (MethodInsnNode) ain;
					ClassNode owner = getNodes().get(min.owner);
					String text = ((LdcInsnNode) ain.getPrevious()).cst.toString();
					if (text.length() == 0 || owner == null) {
						ain = ain.getNext();
						continue;
					}
					Object o = Sandbox.getReturn(owner, min, new Object[] { text });
					if (o != null) {
						strings.add(o.toString());
						argSizes.add(1);
						indecies.add(OpUtils.getIndex(ain));
					}
				}
			}
			ain = ain.getNext();
		}
		if (strings.size() == 0) {
			return;
		}
		ain = method.instructions.getFirst();
		while (ain != null) {
			if (ain.getPrevious() != null && ain.getPrevious().getType() == AbstractInsnNode.LDC_INSN && ain.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (isStringerDesc(min.desc)) {
					int opIndex = OpUtils.getIndex(ain);
					if (indecies.size() > 0 && indecies.get(0) == opIndex) {
						indecies.remove(0);
						int args = argSizes.remove(0);
						String string = strings.remove(0);
						for (int i = 0; i < args; i++) {
							method.instructions.set(ain.getPrevious(), new InsnNode(Opcodes.NOP));
						}
						LdcInsnNode ldc = new LdcInsnNode(string);
						method.instructions.set(ain, ldc);
						ain = ldc;
					}
				}
			}
			ain = ain.getNext();
		}
	}

	private boolean isStringerDesc(String desc) {
		return desc.equals("(Ljava/lang/String;)Ljava/lang/String;");
	}
}
