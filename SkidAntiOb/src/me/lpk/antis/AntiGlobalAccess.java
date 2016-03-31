package me.lpk.antis;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

public abstract class AntiGlobalAccess extends AntiBase {
	private final Map<String, ClassNode> nodes;

	public AntiGlobalAccess(Map<String, ClassNode> nodes) {
		this.nodes = nodes;
	}

	protected final Map<String, ClassNode> getNodes() {
		return nodes;
	}
}