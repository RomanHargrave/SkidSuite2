package me.lpk.antis;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

public abstract class AntiBase {
	public final Map<String, ClassNode> scan(Map<String, ClassNode> nodes) {
		for (String className : nodes.keySet()) {
			ClassNode node = nodes.get(className);
			nodes.put(className, scan(node));
		}
		return nodes;
	}

	protected abstract ClassNode scan(ClassNode node);
}
