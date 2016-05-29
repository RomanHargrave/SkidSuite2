package me.lpk.antis.impl;

import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.antis.AntiBase;
import me.lpk.util.OpUtils;

public class AntiDashO extends AntiBase {

	public AntiDashO(Map<String, ClassNode> nodes) {
		// This currently only works on low/default settings for DashO (As seen in the eval copy)
		//
		// TODO: Improve so the anti will work for higher levels of DashO
		// encryption (Multiple methods w/ multiple different parameters)
		// Unsure if different methods have different decrypt patterns. Doesn't
		// really look like they do aside from some index shifts.
		//
		// A decently finished stack emulator will make this super easy since
		// the values needed for decryption are all hard-coded in the same
		// method.
		super(nodes);
	}

	@Override
	public ClassNode scan(ClassNode node) {
		for (MethodNode mnode : node.methods) {
			replace(mnode);
		}
		for (MethodNode mnode : node.methods) {
			replace(mnode);
		}
		return node;
	}

	/**
	 * Iterates through Insns in a method. If a certain pattern matching DashO
	 * usage is met, the insns are reformatted to only contain the output
	 * string.
	 * 
	 * @param method
	 */
	private void replace(MethodNode method) {
		for (AbstractInsnNode ain : method.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.LDC_INSN
					&& (ain.getNext().getOpcode() == Opcodes.BIPUSH || (ain.getNext().getOpcode() >= Opcodes.ICONST_M1 && ain.getNext().getOpcode() <= Opcodes.ICONST_5))
					&& ain.getNext().getNext().getOpcode() == Opcodes.INVOKESTATIC) {
				String desc = ((MethodInsnNode) ain.getNext().getNext()).desc;
				if (!desc.equals("(Ljava/lang/String;I)Ljava/lang/String;")) {
					continue;
				}
				String inText = ((LdcInsnNode) ain).cst.toString();
				int inNum = OpUtils.getIntValue(ain.getNext());
				String out = deobfuscate(inText, inNum);
				method.instructions.remove(ain.getNext().getNext());
				method.instructions.remove(ain.getNext());
				method.instructions.set(ain, new LdcInsnNode(out));
			}
		}
	}

	public static String deobfuscate(final String paramText, int paramIndex) {
		final char[] inArray = paramText.toCharArray();
		final int inLength = inArray.length;
		final char[] array = inArray;
		int index = 0;
		final int mod = (4 << 5) - 1 ^ 0x20;
		char[] outArray;
		while (true) {
			outArray = array;
			if (index == inLength) {
				break;
			}
			final int indexCopy = index;
			final int charInt = (paramIndex & mod) ^ outArray[indexCopy];
			++paramIndex;
			++index;
			outArray[indexCopy] = (char) charInt;
		}
		return String.valueOf(outArray, 0, inLength).intern();
	}

	public static String lastIndexOf(final String s, int paramIndex, final int indexCounter) {
		paramIndex += 15;
		final char[] charArray = s.toCharArray();
		final int inLength = charArray.length;
		final char[] array = charArray;
		int index = 0;
		final int mod = (4 << 5) - 1 ^ 0x20;
		char[] charArrayOut;
		while (true) {
			charArrayOut = array;
			if (index == inLength) {
				break;
			}
			final int indexCopy = index;
			final int charInt = (paramIndex & mod) ^ charArrayOut[indexCopy];
			paramIndex += indexCounter;
			++index;
			charArrayOut[indexCopy] = (char) charInt;
		}
		return String.valueOf(charArrayOut, 0, inLength).intern();
	}
}
