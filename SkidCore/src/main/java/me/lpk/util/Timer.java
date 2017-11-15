package me.lpk.util;
public class Timer {
	final long init;
	long then, now;

	public Timer() {
		init = System.currentTimeMillis();
		then = System.currentTimeMillis();
	}

	public void log(String s) {
		now = System.currentTimeMillis();
		System.out.println(s + (now - then));
		then = now;
	}

	public void logTotal(String s) {
		now = System.currentTimeMillis();
		System.out.println(s + (now - init));
		then = now;
	}
}