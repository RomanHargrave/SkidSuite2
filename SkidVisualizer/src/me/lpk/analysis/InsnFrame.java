package me.lpk.analysis;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * @editor Matt
 */
@SuppressWarnings("all")
public class InsnFrame extends Frame {
	public AbstractInsnNode ain;

	public InsnFrame(Frame src, AbstractInsnNode ain2) {
		super(src);
		this.ain = ain;
	}

	public InsnFrame(int nLocals, int nStack) {
		super(nLocals, nStack);
		this.ain = null;
	}

}
