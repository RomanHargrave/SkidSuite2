package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.antis.AntiBase;
import me.lpk.antis.impl.AntiZKM5;
import me.lpk.antis.impl.AntiZKM8;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.JarUtil;

public class Main {

	public static void main(String[] args) {
		File file = new File("Nigger.jar");
		try {
			Map<String, ClassNode> nodes =	JarUtil.loadClasses(file);
			for (String className : nodes.keySet()) {
				AntiBase anti = new AntiZKM5();
				ClassNode node = nodes.get(className);
				nodes.put(className, anti.scan(node));
			}
			Map<String, byte[]> out = MappingProcessor.process(nodes);
			out.putAll(JarUtil.loadNonClassEntries(file));
			JarUtil.saveAsJar(out, "Nigger-re.jar");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
