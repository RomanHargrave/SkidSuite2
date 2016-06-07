package me.lpk.analysis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.util.AccessHelper;

public class Sandbox {
private final static String VIRTUAL_NAME = "Sandbox";
	/**
	 * Jesus christ this is so ugly. But hey it means a safer reflection invoke!
	 * 
	 * @param owner
	 * @param min
	 * @param args
	 * @return
	 */
public static Object getIsolatedReturn(ClassNode owner, MethodInsnNode min, Object[] args) {
	if (owner == null){
		return null;
	}
	ClassNode newNode = new ClassNode();
	newNode.version = 52;
	newNode.name = VIRTUAL_NAME;
	newNode.superName = "java/lang/Object";
	int i = 0;
	for (MethodNode mn : owner.methods){
		if (mn.name.equals(min.name) && mn.desc.equals(min.desc)){
			newNode.methods.add(owner.methods.get(i));
		}
		i++;
	}
	try {
		ClassWriter cw = new ClassWriter(0);
		newNode.accept(new VisitorImpl(cw));
		Class<?> clazz = new ClassDefiner(ClassLoader.getSystemClassLoader()).get(newNode.name.replace("/", "."), cw.toByteArray());
		for (Method m : clazz.getMethods()) {
			if (m.getName().equals(min.name) && Type.getMethodDescriptor(m).equals(min.desc)) {
				m.setAccessible(true);
				return m.invoke(null, args);
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	return null;
}

public static Object getReturn(ClassNode owner, MethodInsnNode min, Object[] args) {
	if (owner == null){
		return null;
	}
	ClassNode newNode = new ClassNode();
	newNode.version = 52;
	newNode.name = owner.name;
	newNode.superName = "java/lang/Object";
	newNode.methods.addAll(owner.methods);
	newNode.fields.addAll(owner.fields);
	try {
		ClassWriter cw = new ClassWriter(0);
		newNode.accept(new VisitorImpl(cw));
		Class<?> clazz = new ClassDefiner(ClassLoader.getSystemClassLoader()).get(newNode.name.replace("/", "."), cw.toByteArray());
		for (Method m : clazz.getMethods()) {
			if (m.getName().equals(min.name) && Type.getMethodDescriptor(m).equals(min.desc)) {
				m.setAccessible(true);
				return m.invoke(null, args);
			}
		}
	} catch (IllegalAccessError | IllegalAccessException | IllegalArgumentException | InvocationTargetException  e) {
		//e.printStackTrace();
	}
	return null;
}

	static class ClassDefiner extends ClassLoader {
		public ClassDefiner(ClassLoader parent) {
			super(parent);
		}

		public Class<?> get(String name, byte[] bytes) {
			Class<?> c =  defineClass(name, bytes, 0, bytes.length);
			resolveClass(c);
			return c;
		}
	}

	static class VisitorImpl extends ClassVisitor {

		public VisitorImpl(ClassVisitor cv) {
			super(Opcodes.ASM5, cv);
		}
		@Override
		 public MethodVisitor visitMethod(int access, String name, String desc,   String signature, String[] exceptions) {
			if (name.startsWith("<")) {
				// We DO NOT want static blocks.
				return null;
			}
			access = AccessHelper.isPublic(access) ? access : access | Opcodes.ACC_PUBLIC;
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			if (cv != null) {
				access = AccessHelper.isPublic(access) ? access : access | Opcodes.ACC_PUBLIC;
				cv.visit(version, access, name, signature, superName, interfaces);
			}
		}
	}
}
