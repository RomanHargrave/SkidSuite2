package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.gui.windows.mapping.WindowProguard;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.impl.ModeSimple;
import me.lpk.util.JarUtil;
import me.lpk.util.Timer;

public class Main {
	public static void main(String[] args) {
		WindowProguard.showWindow();
	}
	
	public static void old(){
		Timer t = new Timer();
		// Loading
		File night = new File("Tigur.jar");
		File base = new File("mcOld.jar");
		System.out.println("Loading classes...");
		Map<String, ClassNode> nodes = loadNodes(night);
		t.log("\tLoaded in: ");
		// Making maps
		System.out.println("Generating mappings");
		Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromJar(night);
		Map<String, MappedClass> baseClasses = MappingGen.mappingsFromJar(base);
		t.log("\tMapped in: ");
		// Linking
		System.out.println("Linking correlating sources...");
		mappedClasses = resetRemapped(mappedClasses);
		correlate(mappedClasses, baseClasses);
		t.log("\tCorrelated in: ");
		// Filling in the gaps
		System.out.println("Filling in missing classes...");
		mappedClasses = uglyHacksPre(mappedClasses);
		mappedClasses = CorrelationMapperr.fillInTheGaps(mappedClasses, new ModeSimple());
		mappedClasses = uglyHacks(mappedClasses);
		t.log("Filled the gaps in: ");
		// Processing
		System.out.println("Processing output jar...");
		saveJar(night, nodes, mappedClasses);
		// SaveMappings(mappedClasses);
		System.out.println("Done!");
		t.log("\tExported in: ");
	}

	private static Map<String, ClassNode> loadNodes(File night) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtil.loadClasses(night);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (nodes == null) {
			System.err.println("COULD NOT READ CLASSES FROM " + night.getAbsolutePath());
			return null;
		}
		return nodes;
	}

	private static void correlate(Map<String, MappedClass> mappedClasses, Map<String, MappedClass> baseClasses) {
		HashMap<String, String> h = new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;

			{
				put("packageName/tigurMed_pvpgUW0Pup2L06r", "net/minecraft/client/Minecraft");
			}
		};
		for (String obfu : h.keySet()) {
			MappedClass targetClass = mappedClasses.get(obfu);
			MappedClass cleanClass = baseClasses.get(h.get(obfu));
			// If either are null, do not skip. Get notified by the error!
			mappedClasses = CorrelationMapperr.correlate(targetClass, cleanClass, mappedClasses, baseClasses);
		}
	}

	private static void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses);
		try {
			out.putAll(JarUtil.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int renamed = 0;
		for (MappedClass mc : mappedClasses.values()) {
			if (mc.isTruelyRenamed()) {
				renamed++;
			}
		}
		System.out.println("Saving...  [Ranemed " + renamed + " classes]");
		JarUtil.saveAsJar(out, "TigurDeob.jar");
	}

	private static Map<String, MappedClass> uglyHacksPre(Map<String, MappedClass> mappedClasses) {
		mappedClasses.get("net/minecraft/client/main/Main").setNewName("net/minecraft/client/main/Main");
		mappedClasses.get("net/minecraft/client/main/Main").setRenamed(true);
		return mappedClasses;
	}

	private static Map<String, MappedClass> uglyHacks(Map<String, MappedClass> mappedClasses) {
		mappedClasses.get("net/minecraft/client/main/Main").setNewName("net/minecraft/client/main/Main");
		//
		return mappedClasses;
	}

	private static Map<String, MappedClass> resetRemapped(Map<String, MappedClass> mappedClasses) {
		for (String name : mappedClasses.keySet()) {
			MappedClass mc = mappedClasses.get(name);
			mc.setRenamed(false);
			mappedClasses.put(name, mc);
		}
		return mappedClasses;
	}

}
