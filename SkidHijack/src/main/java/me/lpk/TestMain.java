package me.lpk;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.hijack.ClassModder;
import me.lpk.hijack.InstAgent;
import me.lpk.hijack.Refactorer;

public class TestMain {
	static {
		// agent.jar is this library compiled.
		//
		// MANIFEST.MF of jar:
		// Manifest-Version: 1.0
		// Main-Class: me.lpk.TestMain
		// Agent-Class: me.lpk.hijack.InstAgent
		// Premain-Class: me.lpk.hijack.Premain
		// Can-Redefine-Classes: true
		// Can-Retransform-Classes: true
		// Class-Path: .

		InstAgent.initialize("agent.jar");
		Refactorer.register("me/lpk/Thing", new ClassModder() {
			@Override
			public void modify(ClassNode cn) {
				for (MethodNode mn : cn.methods) {
					if (mn.desc.endsWith("String;")) {
						for (AbstractInsnNode ain : mn.instructions.toArray()) {
							if (ain.getType() == AbstractInsnNode.LDC_INSN) {
								LdcInsnNode ldc = (LdcInsnNode) ain;
								ldc.cst = "override-text";
							}
						}
					}
				}
			}
		});
	}

	public static void main(String[] args) {
		System.out.println(">>>Entry");
		Thing t = new Thing();
		// Without the ClassModder above:
		// prints "default-return-text"
		// With
		// prints "override-text"
		System.out.println(t.method());
		System.out.println(">>>Exit");
	}
}