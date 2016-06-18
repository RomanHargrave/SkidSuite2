package me.lpk.hijack;

public class ClassMapping {
	private final String clean, obfuscated;

	public ClassMapping(String clean, String obfuscated) {
		this.clean = clean;
		this.obfuscated = obfuscated;
	}

	public String getClean() {
		return clean;
	}

	public String getObfuscated() {
		return obfuscated;
	}
}
