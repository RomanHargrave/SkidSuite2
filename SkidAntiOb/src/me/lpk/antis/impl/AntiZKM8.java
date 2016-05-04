package me.lpk.antis.impl;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.antis.AntiBase;

public class AntiZKM8 extends AntiBase {
	private final Map<Integer, String> strings1 = new HashMap<Integer, String>();
	private final Map<Integer, String> strings2 = new HashMap<Integer, String>();
	private final Map<Integer, Integer> modifiers = new HashMap<Integer, Integer>();
	private String zkmField1;
	private boolean multiZKM = false;

	public AntiZKM8() {
		// No nodes are needed for reversing.
		// TODO: Look at recent JRat builds. They use the newest ZKM 8.
		// TODO: Port ZKM5 Anti features that apply for this update (Most of it should still apply)
		super(null);
	}

	@Override
	public ClassNode scan(ClassNode node) {
		for (MethodNode mnode : node.methods) {
			if (mnode.name.startsWith("<c")) {
				extractStatic(mnode);
				cleanStatic(mnode);
			}
		}
		for (MethodNode mnode : node.methods) {
			if (mnode.desc.endsWith("(II)Ljava/lang/String;")) {
				extractDecrypt(mnode);
			}
		}
		for (MethodNode mnode : node.methods) {
			if (mnode.name.startsWith("<")) {
				continue;
			}
			replace(mnode);
		}
		for (String s : strings1.values()) {
			System.out.println(s);
		}
		for (String s : strings2.values()) {
			System.out.println(s);
		}
		return node;
	}

	/**
	 * Update values of the ZKM decrypt(int,int) with the original strings.
	 * 
	 * @param method
	 *            The method to replace strings in.
	 */
	private void replace(MethodNode method) {

	}

	private void extractDecrypt(MethodNode mnode) {

	}

	/**
	 * Extracts the strings from the static block and deobfuscates them.
	 * 
	 * @param method
	 */
	private void extractStatic(MethodNode method) {

	}

	/**
	 * Finds the begining of the ZKM blurb, the end, then removed everything in
	 * between!
	 * 
	 * @param method
	 * @return
	 */
	private void cleanStatic(MethodNode method) {

	}

	/**
	 * Decrypts a string based on their index in the array (or alone if only one
	 * string) and the existing modifiers.
	 * 
	 * @param input
	 *            Obfuscated string
	 * @return Deobfuscated string
	 */

	private String decrypt(String input) {
		String decrypted = "";
		// TODO: 
		return decrypted;
	}
}
