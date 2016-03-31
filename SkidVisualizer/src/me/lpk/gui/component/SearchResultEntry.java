package me.lpk.gui.component;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class SearchResultEntry {
	private final ClassNode target;
	private final MethodNode method;
	private final int opcodeIndex;
	private final String display;

	public SearchResultEntry(ClassNode target) {
		this(target, null, 0);
	}

	public SearchResultEntry(ClassNode target, MethodNode method, int opcodeIndex) {
		this.target = target;
		this.method = method;
		this.opcodeIndex = opcodeIndex;
		this.display = method != null ? target.name + "#" + method.name + "@" + opcodeIndex :  target.name;
	}

	public MethodNode getMethod() {
		return method;
	}

	public int getOpcodeIndex() {
		return opcodeIndex;
	}

	public ClassNode getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return display;
	}
}