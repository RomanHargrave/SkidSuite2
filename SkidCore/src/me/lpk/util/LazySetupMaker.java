package me.lpk.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static LazySetupMaker get(String jarIn, boolean readLibraries) {
		Logger.logLow("Loading: " + jarIn + " (Reading Libraries: " + readLibraries + ")");
		File in = new File(jarIn);
		Map<String, ClassNode> nodes = null;
		Map<String, ClassNode> libNodes = new HashMap<String, ClassNode>();
		if (readLibraries) {
			try {
				for (File lib : getLibraries()) {
					libNodes.putAll(JarUtil.loadClasses(lib));
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		nodes = loadNodes(in);
		//
		//
		Logger.logLow("Generating nodes");
		Map<String, MappedClass> mappings = MappingGen.mappingsFromNodesNoLinking(nodes);
		Map<String, MappedClass> libMappings = new HashMap<String, MappedClass>();
		if (readLibraries) {
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
		if (readLibraries) {
			mappings.putAll(libMappings);
		}
		Logger.logLow("Linking node structures");
		for (MappedClass mc : mappings.values()) {
			MappingGen.linkMappings(mc, mappings);
		}
		Logger.logLow("Completed loading from: " + jarIn);
		return new LazySetupMaker(jarIn, nodes, mappings, libNodes, libMappings);
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
