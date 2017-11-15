package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.impl.ModeNone;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtils;
import me.lpk.util.LazySetupMaker;
import me.lpk.util.Timer;

public class Main {
	public static void main(String[] args) {
		//WindowEnigma.showWindow();
		//old();
	}
	
	public static void dank(){
		
	}
	
	public static void old() {
		Timer t = new Timer();
		// Loading
		File night = new File("1.9.jar");
		try {
			me.lpk.util.Classpather.addFile(night);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File base = new File("Base 1.9.jar");
		System.out.println("Loading classes...");
		Map<String, ClassNode> nightNodes = loadNodes(night);
		Map<String, ClassNode> baseNodes = loadNodes(base);
		t.log("\tLoaded in: ");
		// Making maps
		System.out.println("Generating mappings");
		Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromNodes(nightNodes);
		for (MappedClass mappedClass : mappedClasses.values()) {
			mappedClasses = MappingGen.linkMappings(mappedClass, mappedClasses);
		}
		Map<String, MappedClass> baseClasses = MappingGen.mappingsFromNodes(baseNodes);
		t.log("\tMapped in: ");
		// Linking
		System.out.println("Linking correlating sources...");
		mappedClasses = resetRemapped(mappedClasses);
		correlate(mappedClasses, baseClasses);
		t.log("\tCorrelated in: ");
		// Filling in the gaps
		System.out.println("Filling in missing classes...");
		mappedClasses = uglyHacksPre(mappedClasses);
		mappedClasses = CorrelationMapper.fillInTheGaps(mappedClasses, new ModeNone());
		mappedClasses = uglyHacks(mappedClasses);
		t.log("Filled the gaps in: ");
		// Processing
		System.out.println("Processing output jar...");
		saveJar(night, nightNodes, mappedClasses);
		// SaveMappings(mappedClasses);
		saveMappings(mappedClasses, "map.map");
		System.out.println("Done!");
		t.log("\tExported in: ");
	}
	
	public static void go(String pathTarget, String pathClean) {
		// Loading
		File targetJar = new File(pathTarget);
		File cleanJar = new File(pathClean);
		LazySetupMaker l = LazySetupMaker.get(targetJar.getAbsolutePath(), false, false);
		LazySetupMaker l2 = LazySetupMaker.get(cleanJar.getAbsolutePath(), false, false);
		try {
			Classpather.addFile(targetJar);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Loading classes...");
		Map<String, ClassNode> targetNodes = l.getNodes();
		Map<String, ClassNode> baseNodes = l2.getNodes();
		// Making maps
		System.out.println("Generating mappings");
		Map<String, MappedClass> targetMappings = MappingGen.mappingsFromNodes(targetNodes);
		for (MappedClass mappedClass : targetMappings.values()) {
			targetMappings = MappingGen.linkMappings(mappedClass, targetMappings);
		}
		Map<String, MappedClass> cleanMappings = MappingGen.mappingsFromNodes(baseNodes);
		// Linking
		System.out.println("Linking correlating sources...");
		targetMappings = resetRemapped(targetMappings);
		correlate(targetMappings, cleanMappings);
		// Filling in the gaps
		System.out.println("Filling in missing classes...");
		targetMappings = uglyHacksPre(targetMappings);
		targetMappings = CorrelationMapper.fillInTheGaps(targetMappings, new ModeNone());
		targetMappings = uglyHacks(targetMappings);
		// Processing
		System.out.println("Processing output jar...");
		saveJar(targetJar, targetNodes, targetMappings);
		saveMappings(targetMappings, l.getName() + ".enigma.map");
		System.out.println("Done!");
	}

	private static void saveMappings(Map<String, MappedClass> mappedClasses, String string) {
		StringBuilder sb = new StringBuilder();
		for (MappedClass clazz : mappedClasses.values()){
			sb.append("CLASS " + clazz.getOriginalName() + " " + clazz.getNewName()+"\n");
			for (MappedMember f : clazz.getFields()){
				sb.append("\tFIELD " + f.getOriginalName() + " " + f.getNewName() + " " + f.getDesc()+"\n");
			}
			for (MappedMember m : clazz.getMethods()){
				if (m.getOriginalName().contains("<")){
					continue;
				}
				sb.append("\tMETHOD " + m.getOriginalName() + " " + m.getNewName() + " " + m.getDesc()+"\n");
			}
		} 
		try {
			FileUtils.write(new File(string) , sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, ClassNode> loadNodes(File night) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtils.loadClasses(night);
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
		HashMap<String, String> h = new HashMap<String, String>();
		
		
		
		for (String obfu : h.keySet()) {
			MappedClass targetClass = mappedClasses.get(obfu);
			MappedClass cleanClass = baseClasses.get(h.get(obfu));
			if (targetClass == null || cleanClass == null) {
				System.err.println("NULL: " + obfu + ":" + h.get(obfu));
				continue;
			}
			mappedClasses = CorrelationMapper.correlate(targetClass, cleanClass, mappedClasses, baseClasses);
		}
	}

	private static void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses, true);
		try {
			out.putAll(JarUtils.loadNonClassEntries(nonEntriesJar));
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
		JarUtils.saveAsJar(out, "Hasure2.jar");
	}
	
	private static Map<String, MappedClass> resetRemapped(Map<String, MappedClass> mappedClasses) {
		for (String name : mappedClasses.keySet()) {
			MappedClass mc = mappedClasses.get(name);
			mc.setRenamedOverride(false);
			mappedClasses.put(name, mc);
		}
		return mappedClasses;
	}

	private static Map<String, MappedClass> uglyHacksPre(Map<String, MappedClass> mappedClasses) {
		//mappedClasses.get("Main").setNewName("Main");

		return uglyHacks(mappedClasses);
	}

	private static Map<String, MappedClass> uglyHacks(Map<String, MappedClass> mappedClasses) {
		/*
		mappedClasses.get("me/seriexcode/hasureclient/ai").setNewName("me/seriexcode/hasureclient/event/impl/BlockBoundsEvent");
		mappedClasses.get("me/seriexcode/hasureclient/e").setNewName("me/seriexcode/hasureclient/event/IEvent");
		mappedClasses.get("me/seriexcode/hasureclient/c").setNewName("me/seriexcode/hasureclient/event/EventTarget");
		mappedClasses.get("me/seriexcode/hasureclient/M").setNewName("me/seriexcode/hasureclient/command/CommandBase");
		mappedClasses.get("me/seriexcode/hasureclient/di").setNewName("me/seriexcode/hasureclient/irc/AbstractIRC");
		mappedClasses.get("me/seriexcode/hasureclient/dk").setNewName("me/seriexcode/hasureclient/irc/IIRC");
		mappedClasses.get("me/seriexcode/hasureclient/dI").setNewName("me/seriexcode/hasureclient/util/DamageExploit");
		mappedClasses.get("me/seriexcode/hasureclient/dD").setNewName("me/seriexcode/hasureclient/font/TruetypeFont");
		mappedClasses.get("me/seriexcode/hasureclient/aX").setNewName("me/seriexcode/hasureclient/mod/ModManager");
		mappedClasses.get("me/seriexcode/hasureclient/aW").setNewName("me/seriexcode/hasureclient/mod/Mod");
		mappedClasses.get("me/seriexcode/hasureclient/aN").setNewName("me/seriexcode/hasureclient/Client");
		//
		mappedClasses.get("me/seriexcode/hasureclient/tt").setNewName("me/seriexcode/hasureclient/killswitch/Jumpscare0");
		mappedClasses.get("me/seriexcode/hasureclient/vC").setNewName("me/seriexcode/hasureclient/killswitch/Jumpscare1");
		mappedClasses.get("me/seriexcode/hasureclient/dT").setNewName("me/seriexcode/hasureclient/killswitch/Thread0");
		mappedClasses.get("me/seriexcode/hasureclient/dQ").setNewName("me/seriexcode/hasureclient/killswitch/Thread1");
		mappedClasses.get("me/seriexcode/hasureclient/dR").setNewName("me/seriexcode/hasureclient/killswitch/Thread2");
		mappedClasses.get("me/seriexcode/hasureclient/dS").setNewName("me/seriexcode/hasureclient/killswitch/Thread3");
		mappedClasses.get("me/seriexcode/hasureclient/dU").setNewName("me/seriexcode/hasureclient/killswitch/Thread4");
		mappedClasses.get("me/seriexcode/hasureclient/dV").setNewName("me/seriexcode/hasureclient/killswitch/Thread5");
		mappedClasses.get("me/seriexcode/hasureclient/dW").setNewName("me/seriexcode/hasureclient/killswitch/Thread6");
		mappedClasses.get("me/seriexcode/hasureclient/dX").setNewName("me/seriexcode/hasureclient/killswitch/Thread7");
		mappedClasses.get("me/seriexcode/hasureclient/dY").setNewName("me/seriexcode/hasureclient/killswitch/Thread8");
		mappedClasses.get("me/seriexcode/hasureclient/dZ").setNewName("me/seriexcode/hasureclient/killswitch/Thread9");
		mappedClasses.get("me/seriexcode/hasureclient/dR").setNewName("me/seriexcode/hasureclient/killswitch/Thread10");
		*/
		return mappedClasses;
	}


}
