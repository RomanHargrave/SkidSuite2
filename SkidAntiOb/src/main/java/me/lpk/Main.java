package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.antis.AntiBase;
import me.lpk.antis.impl.*;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Main {

	public static void main(String[] args) {

		try {
			String file = "Stringer1.jar";
			LazySetupMaker lsm = LazySetupMaker.get(file, false, false);

			File dir = new File("y");
			for (File f : dir.listFiles()) {
				LazySetupMaker.addExtraLibraryJar(f);
			}
			for (File f : LazySetupMaker.getExtraLibs()) {
				Classpather.addFile(f);
			}
			Classpather.addFile(file);
			for (String className : lsm.getNodes().keySet()) {
				AntiBase anti = new AntiStringer(lsm.getNodes());
				ClassNode node = lsm.getNodes().get(className);
				lsm.getNodes().put(className, anti.scan(node));
			}
			Map<String, byte[]> out = MappingProcessor.process(lsm.getNodes(), new HashMap<String, MappedClass>(), false);
			out.putAll(JarUtils.loadNonClassEntries(new File(file)));
			JarUtils.saveAsJar(out, file + "-re.jar");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
