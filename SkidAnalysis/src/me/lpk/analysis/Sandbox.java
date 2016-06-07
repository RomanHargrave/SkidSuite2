package me.lpk.analysis;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

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
	public static Object getReturn(ClassNode owner, MethodInsnNode min, Object[] args) {
		if (owner == null){
			return null;
		}
		ClassNode newNode = new ClassNode();
		newNode.version = 52;
		newNode.name = VIRTUAL_NAME;
		newNode.superName = "java/lang/Object";
		
		int methodIndex = -1, i = 0;
		for (MethodNode mn : owner.methods){
			if (mn.name.equals(min.name) && mn.desc.equals(min.desc)){
				methodIndex = i;
			}
			i++;
		}
		newNode.methods.add(owner.methods.get(methodIndex));
		MethodNode meth = newNode.methods.get(0);
		System.out.println("Invoking: " + min.desc);
		System.out.println("Args:");
		for (Object o : args) {
			System.out.println("\t" + o);
		}
		try {
			ClassWriter cw = new ClassWriter(0);
			newNode.accept(new TrimVisitor(cw, meth));
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

	static class TrimVisitor extends ClassVisitor {
		private final MethodNode mn;

		public TrimVisitor(ClassVisitor cv, MethodNode mn) {
			super(Opcodes.ASM5, cv);
			this.mn = mn;
		}
/*
		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			return null;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if (name.equals(mn.name) && cv != null) {
				return cv.visitMethod(access, name, desc, signature, exceptions);
			}
			return null;
		}
*/
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			if (cv != null) {
				cv.visit(version, access, name, signature, superName, interfaces);
			}
		}
	}
}
