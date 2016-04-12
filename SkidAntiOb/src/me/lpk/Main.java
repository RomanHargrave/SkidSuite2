package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.antis.AntiBase;
import me.lpk.antis.impl.AntiDashO;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.JarUtil;

public class Main {

	public static void main(String[] args) {
		File file = new File("DashOPro.jar");
		try {
			Map<String, ClassNode> nodes =	JarUtil.loadClasses(file);
			AntiBase anti = new AntiDashO(nodes);
			anti.scan(nodes);
			Map<String, byte[]> out = MappingProcessor.process(nodes);
			out.putAll(JarUtil.loadNonClassEntries(file));
			JarUtil.saveAsJar(out, "DashO.jar");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
