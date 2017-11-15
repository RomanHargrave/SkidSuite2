package me.lpk.hijack;

import java.lang.instrument.Instrumentation;

public class Premain {
	public static void premain(String agentArgument, Instrumentation instrumentation) {
		System.out.println("Agent Args:" + agentArgument);
	}
}