package me.lpk.hijack;

import java.lang.instrument.Instrumentation;

public class InstAgent {
	private static Instrumentation instrumentation;

	/**
	 * Called when declared before JVM start.
	 * 
	 * @param args
	 * @param inst
	 * @throws Exception
	 */
	public static void premain(String args, Instrumentation inst) throws Exception {
		setAndAddTransformer(inst);
	}

	/**
	 * Called when declared after JVM start.
	 * 
	 * @param args
	 * @param inst
	 * @throws Exception
	 */
	public static void agentmain(String args, Instrumentation inst) throws Exception {
		setAndAddTransformer(inst);
	}

	private static void setAndAddTransformer(Instrumentation inst) {
		instrumentation = inst;
		instrumentation.addTransformer(new Refactorer());
		System.out.println("Instrumentation: " + "[ Redefinition:" + instrumentation.isRedefineClassesSupported() + ", Retransformation:"
				+ instrumentation.isRetransformClassesSupported() + " ]");
	}

	public static void initialize(String jarFilePath) {
		if (instrumentation == null) {
			Loader.loadAgent(jarFilePath);
		}
	}
}