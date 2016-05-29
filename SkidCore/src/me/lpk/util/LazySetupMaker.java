package me.lpk.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.MappingGen;

/**
 * I'm sick of copy pasting the code in the static methods below. Made this so I
 * can just do it quickly.
 * 
 * @author Matt
 */
public class LazySetupMaker {
	private final String name;
	private final Map<String, ClassNode> nodes;
	private final Map<String, MappedClass> mappings;
	private final Map<String, ClassNode> libNodes;
	private final Map<String, MappedClass> libMappings;
	private static final Set<File> libraries = new HashSet<File>();

	public LazySetupMaker(String name, Map<String, ClassNode> nodes, Map<String, MappedClass> mappings) {
		this.name = name;
		this.nodes = nodes;
		this.mappings = mappings;
		this.libNodes = new HashMap<String, ClassNode>();
		this.libMappings = new HashMap<String, MappedClass>();
	}

	public LazySetupMaker(String name, Map<String, ClassNode> nodes, Map<String, MappedClass> mappings, Map<String, ClassNode> libNodes,
			Map<String, MappedClass> libMappings) {
		this.name = name;
		this.nodes = nodes;
		this.mappings = mappings;
		this.libNodes = libNodes;
		this.libMappings = libMappings;
	}

	public static LazySetupMaker get(String jarIn, boolean readDefaultLibraries, boolean readExtraLibs) {
		Logger.logLow("Loading: " + jarIn + " (Reading Libraries: " + readDefaultLibraries + ")");
		File in = new File(jarIn);
		Map<String, ClassNode> nodes = null;
		Map<String, ClassNode> libNodes = new HashMap<String, ClassNode>();
		if (readExtraLibs) {
			try {
				for (File lib : getExtraLibs()) {
					libNodes.putAll(JarUtils.loadClasses(lib));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (readDefaultLibraries) {
			try {
				for (File lib : getLibraries()) {
					libNodes.putAll(JarUtils.loadClasses(lib));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		nodes = loadNodes(in);
		//
		//
		Logger.logLow("Generating nodes");
		Map<String, MappedClass> mappings = MappingGen.mappingsFromNodesNoLinking(nodes);
		Map<String, MappedClass> libMappings = new HashMap<String, MappedClass>();
		if (readDefaultLibraries || readExtraLibs) {
			Logger.logLow("Marking library nodes as read-only...");
			libMappings.putAll(MappingGen.mappingsFromNodesNoLinking(libNodes));
			for (MappedClass mc : libMappings.values()) {
				mc.setIsLibrary(true);
				for (MappedMember mm : mc.getFields()) {
					mm.setIsLibrary(true);
				}
				for (MappedMember mm : mc.getMethods()) {
					mm.setIsLibrary(true);
				}
			}
		}
		//
		//
		Logger.logLow("Merging target jar and library nodes");
		if (readDefaultLibraries  || readExtraLibs) {
			mappings.putAll(libMappings);
		}
		Logger.logLow("Linking node structures");
		for (MappedClass mc : mappings.values()) {
			MappingGen.linkMappings(mc, mappings);
		}
		Logger.logLow("Completed loading from: " + jarIn);
		return new LazySetupMaker(jarIn, nodes, mappings, libNodes, libMappings);
	}

	public static void addExtraLibraryJar(File lib) {
		libraries.add(lib);
	}

	public static void clearExtraLibraries() {
		libraries.clear();
	}

	public static Set<File> getExtraLibs() {
		return libraries;
	}

	public String getName() {
		return name;
	}

	public Map<String, ClassNode> getNodes() {
		return nodes;
	}

	public Map<String, MappedClass> getMappings() {
		return mappings;
	}

	public Map<String, ClassNode> getLibNodes() {
		return libNodes;
	}

	public Map<String, MappedClass> getLibMappings() {
		return libMappings;
	}

	private static Map<String, ClassNode> loadNodes(File file) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtils.loadClasses(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (nodes == null) {
			System.err.println("COULD NOT READ CLASSES FROM " + file.getAbsolutePath());
			return null;
		}
		return nodes;
	}

	private static List<File> getLibraries() {
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
