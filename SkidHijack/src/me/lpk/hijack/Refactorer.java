package me.lpk.hijack;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.util.ASMUtils;

public class Refactorer implements ClassFileTransformer, Opcodes {
	private static final Map<String, ClassModder> modders = new HashMap<String, ClassModder>();

	public static void register(String name, ClassModder modder) {
		modders.put(name, modder);
	}
	
	public static void unregister(String name) {
		modders.remove(name);
	}

	/**
	 * Receives classes before they are loaded.
	 * 
	 * @param loader
	 * @param name
	 * @param clazz
	 * @param domain
	 * @param bytes
	 * @return
	 * @throws IllegalClassFormatException
	 */
	public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
		if (isRegistered(name)) {
			ClassModder cm = modders.get(name);
			ClassNode cn = getClass(name);
			cm.setDomain(domain);
			cm.setBackup(bytes);
			cm.setLoader(loader);
			cm.setClass(clazz);
			cm.modify(cn);
			return ASMUtils.getNodeBytes(cn, false);
		}
		return bytes;
	}

	/**
	 * Checks if a classname <i>(com/example/Format)</i> is registered.
	 * 
	 * @param name
	 * @return
	 */
	private boolean isRegistered(String name) {
		return modders.containsKey(name);
	}

	/**
	 * Loads a classnode from a class in the JVM.
	 * 
	 * @param name
	 * @return
	 */
	private ClassNode getClass(String name) {
		ClassReader cr = null;
		try {
			cr = new ClassReader(name.replace('/', '.'));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		ClassNode cn = new ClassNode();
		cr.accept(cn, ClassReader.EXPAND_FRAMES);
		return cn;
	}
}