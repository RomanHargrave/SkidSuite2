package me.lpk;

public class Timer {
	final long init;
	long then, now;

	public Timer() {
		init = System.currentTimeMillis();
		then = System.currentTimeMillis();
	}

	void log(String s) {
		now = System.currentTimeMillis();
		System.out.println(s + (now - then));
		then = now;
	}

	void logTotal(String s) {
		now = System.currentTimeMillis();
		System.out.println(s + (now - init));
		then = now;
	}
}