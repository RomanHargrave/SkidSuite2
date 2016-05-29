package me.lpk.mapping;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.util.ASMUtils;

public class MappingProcessor {
	public static boolean PRINT;

	/**
	 * Given a map of ClassNodes and mappings, returns a map of class names to
	 * class bytes.
	 * 
	 * @param nodes
	 * @param mappings
	 * @return
	 */
	public static Map<String, byte[]> process(Map<String, ClassNode> nodes, Map<String, MappedClass> mappings, boolean useMaxs) {
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		SkidRemapper mapper = new SkidRemapper(mappings);
		for (ClassNode cn : nodes.values()) {
			ClassWriter cw = new ClassWriter(useMaxs ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
			ClassVisitor remapper = new ClassRemapper(cw, mapper);
			cn.accept(remapper);
			out.put(mappings.get(cn.name).getNewName(), cw.toByteArray());
		}
		return out;
	}

	public static Map<String, byte[]> process(Map<String, ClassNode> nodes) {
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		int workIndex = 1;
		for (ClassNode cn : nodes.values()) {
			out.put(cn.name, ASMUtils.getNodeBytes(cn));
			//
			if (PRINT) {
				String percentStr = "" + ((workIndex + 0.000000001f) / (nodes.size() - 0.00001f)) * 100;
				percentStr = percentStr.substring(0, percentStr.length() > 5 ? 5 : percentStr.length());
				System.out.println("	" + workIndex + "/" + nodes.size() + " [" + percentStr + "%]");
			}
			workIndex++;
		}
		return out;
	}
}
