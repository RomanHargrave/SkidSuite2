package me.lpk.mapping;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

public class MappingProcessor {
	public static boolean PRINT;

	public static Map<String, byte[]> process(Map<String, ClassNode> nodes, Map<String, MappedClass> mappings) {
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		int workIndex = 1;
		SkidRemapper mapper = new SkidRemapper(mappings);
		for (ClassNode cn : nodes.values()) {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor remapper = new ClassRemapper(cw, mapper);
			cn.accept(remapper);
			boolean isRenamed = mappings.containsKey(cn.name);
			if (isRenamed) {
				out.put(mappings.get(cn.name).getNewName(), cw.toByteArray());
			} else {
				out.put(cn.name, cw.toByteArray());
			}
			//
			if (PRINT) {
				String percentStr = "" + ((workIndex + 0.000000001f) / (mappings.size() - 0.00001f)) * 100;
				percentStr = percentStr.substring(0, percentStr.length() > 5 ? 5 : percentStr.length());
				System.out.println("	" + workIndex + "/" + mappings.size() + " [" + percentStr + "%]");
			}
			workIndex++;
		}
		return out;
	}
}
