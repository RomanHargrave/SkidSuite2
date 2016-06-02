package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.antis.AntiBase;
import me.lpk.antis.impl.AntiDashO;
import me.lpk.antis.impl.AntiZKM5;
import me.lpk.antis.impl.AntiZKM8;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;

public class Main {

	public static void main(String[] args) {
		try {
			String file = "DashOPro.jar";
			LazySetupMaker lsm = LazySetupMaker.get(file, false, false);
			File dir = new File("x");
			for (File f : dir.listFiles()){
				lsm.addExtraLibraryJar(f);
			}
			for (File f : lsm.getExtraLibs()){
				Classpather.addFile(f);
			}
			Classpather.addFile(file);

			for (String className : lsm.getNodes().keySet()) {
				AntiBase anti = new AntiDashO(lsm.getNodes());
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
