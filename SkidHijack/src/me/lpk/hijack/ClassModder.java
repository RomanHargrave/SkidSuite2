package me.lpk.hijack;

import java.security.ProtectionDomain;

import org.objectweb.asm.tree.ClassNode;

public abstract class ClassModder {
	private ProtectionDomain domain;
	private byte[] backup;
	private ClassLoader loader;
	private Class<?> clazz;

	public abstract void modify(ClassNode cn);

	public void setDomain(ProtectionDomain domain) {
		this.domain = domain;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public ProtectionDomain getDomain() {
		return domain;
	}

	public byte[] getBackup() {
		return backup;
	}

	public ClassLoader getLoader() {
		return loader;
	}

	public void setBackup(byte[] backup) {
		this.backup = backup;
	}

	public void setLoader(ClassLoader loader) {
		this.loader = loader;
	}

	public void setClass(Class<?> clazz) {
		this.clazz = clazz;
	}
}
