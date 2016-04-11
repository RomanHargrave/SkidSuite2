package me.lpk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import me.lpk.gui.windows.mapping.WindowProguard;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.impl.ModeNone;
import me.lpk.mapping.remap.impl.ModeSimple;
import me.lpk.util.JarUtil;
import me.lpk.util.OpUtil;
import me.lpk.util.Timer;

public class Main {
	public static void main(String[] args) {
		WindowProguard.showWindow();
	}
	public static void old() {
		Timer t = new Timer();
		// Loading
		File night = new File("NigtmareB12FixedNames.jar");
		File base = new File("Base.jar");
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
		mappedClasses = CorrelationMapperr.fillInTheGaps(mappedClasses, new ModeNone());
		mappedClasses = uglyHacks(mappedClasses);
		t.log("Filled the gaps in: ");
		// Processing
		System.out.println("Processing output jar...");
		saveJar(night, nightNodes, mappedClasses);
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
				put("Class62903", "me/lpk/client/MCHook");
				put("Class106053", "net/minecraft/client/resources/FallbackResourceManager");
				put("Class99954", "net/minecraft/util/Timer");
				put("Class107370", "net/minecraft/client/gui/GuiVideoSettings");
				put("Class108049", "net/minecraft/client/gui/GuiMultiplayer");
				put("Class103732", "net/minecraft/client/gui/GuiCreateWorld");
				put("Class39766", "org/newdawn/slick/Input");
				put("Class40080", "org/newdawn/slick/BigImage");
				put("Class37539", "org/newdawn/slick/opengl/InternalTextureLoader");
				put("Class38124", "org/newdawn/slick/openal/AudioLoader");
				put("Class38116", "org/newdawn/slick/imageout/ImageWriterFactory");
				put("Class37454", "org/newdawn/slick/gui/TextField");
				put("Class37474", "org/newdawn/slick/gui/MouseOverArea");
				put("Class37351", "org/newdawn/slick/gui/BasicComponent");
				put("Class38022", "org/newdawn/slick/particles/ConfigurableEmitter");
				put("Class39812", "org/newdawn/slick/font/HieroSettings");
				put("Class38099", "org/newdawn/slick/muffin/FileMuffin");
				put("Class38153", "org/newdawn/slick/muffin/WebstartMuffin");
				put("Class59347", "org/newdawn/slick/state/StateBasedGame");
				put("Class37864", "org/newdawn/slick/svg/SVGMorph");
				put("Class37893", "org/newdawn/slick/svg/InkscapeLoader");
				put("Class38411", "org/newdawn/slick/tiled/TiledMap");
				put("Class38410", "org/newdawn/slick/tiled/TileSet");
				put("Class38247", "org/newdawn/slick/util/xml/ObjectTreeParser");
				put("Class38006", "org/newdawn/slick/util/pathfinding/AStarPathFinder");
				put("Class40029", "org/newdawn/slick/AppGameContainer");
				put("Class39981", "org/darkstorm/minecraft/gui/ExampleGuiManager");
				put("Class39305", "org/darkstorm/minecraft/gui/component/basic/BasicRadioButton");
				put("Class39894", "org/darkstorm/minecraft/gui/layout/GridLayoutManager");
				put("Class135574", "me/lpk/client/management/AbstractManager");
				put("Class43982", "me/lpk/client/event/RegisterEvent");
				put("Class104981", "me/lpk/client/gui/screen/impl/GuiClientInit");
				put("Class104968", "me/lpk/client/gui/screen/impl/GuiAccountList");
				put("Class105623", "me/lpk/client/module/ModuleManager");
				put("Class105767", "me/lpk/client/gui/screen/impl/mainmenu/GuiModdedMainMenu");
			}
		};
		for (String obfu : h.keySet()) {
			MappedClass targetClass = mappedClasses.get(obfu);
			MappedClass cleanClass = baseClasses.get(h.get(obfu));
			// If either are null, do not skip. Get notified by the error!
			if (targetClass == null || cleanClass == null) {
				System.err.println("NULL: " + obfu + ":" + h.get(obfu));
				continue;
			}
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
		JarUtil.saveAsJar(out, "NightmareDeob.jar");
	}

	private static Map<String, MappedClass> uglyHacksPre(Map<String, MappedClass> mappedClasses) {
		mappedClasses.get("Main").setNewName("Main");
		return mappedClasses;
	}

	private static Map<String, MappedClass> uglyHacks(Map<String, MappedClass> mappedClasses) {
		mappedClasses.get("Main").setNewName("Main");
		//
		return mappedClasses;
	}

	private static Map<String, MappedClass> resetRemapped(Map<String, MappedClass> mappedClasses) {
		for (String name : mappedClasses.keySet()) {
			MappedClass mc = mappedClasses.get(name);
			mc.setRenamedOverride(false);
			mappedClasses.put(name, mc);
		}
		return mappedClasses;
	}

	protected static List<File> getLibraries() {
		List<File> files = new ArrayList<File>();
		files.add(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
		//
		File libDir = new File("libraries");
		libDir.mkdirs();
		for (File lib : FileUtils.listFiles(libDir, new String[] { "jar" }, true)) {
			files.add(lib);
		}
		return files;
	}
}
