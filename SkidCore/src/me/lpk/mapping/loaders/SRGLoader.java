package me.lpk.mapping.loaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;

public class SRGLoader extends MappingLoader {
	/**
	 * Instantiates the loader with a map of classnodes to be mapped.
	 * 
	 * @param nodes
	 */
	public SRGLoader(Map<String, ClassNode> nodes) {
		super(nodes);
	}
	/**
	 * Returns a map of MappedClasses based on the nodes given in the
	 * constructor and the mapping file read through the parameter.
	 * 
	 * @param in
	 *            FileReader of the SRG mappings file
	 * @return
	 */
	@Override
	public Map<String, MappedClass> read(FileReader in) {
		try {
			return read(new BufferedReader(in));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Reads each line in a reader and parses mappings from the SRG format.
	 * 
	 * @param fileReader
	 * @return
	 * @throws Exception
	 */
	@Override
	public Map<String, MappedClass> read(BufferedReader fileReader) throws Exception {
		Map<String, MappedClass> remap = new HashMap<String, MappedClass>();
		String line = null;
		while ((line = fileReader.readLine()) != null) {
			if (line.startsWith("CL: ")){
				String className = line.split(" ")[1];
				String newClassName = line.split(" ")[2];
				if (!nodes.containsKey(className)){
					continue;
				}
				MappedClass mc = new MappedClass(nodes.get(className), newClassName);
				remap.put(className, mc);
			} else if (line.startsWith("FD: ")){
				String s1 = line.split(" ")[1], s2 = line.split(" ")[2];
				String className = s1.substring(0, s1.lastIndexOf('/'));
				if (!remap.containsKey(className)){
					continue;
				}
				String fieldName = s1.substring(s1.lastIndexOf('/') + 1);
				String newFieldName = s2.substring(s2.lastIndexOf('/') + 1);
				String desc = "L" + className + ";";
				MappedClass mc = remap.get(className);
				//
				Object node = getFieldNode(mc.getNode().fields, fieldName, desc);
				MappedMember field = new MappedMember(mc, node, 0, desc,fieldName );
				field.setNewName(newFieldName);
				mc.addField(field);
			} else if (line.startsWith("MD: ")){
				String 
					s1 = line.split(" ")[1], 
					desc = line.split(" ")[2],
					s3 = line.split(" ")[3];
				String className =  s1.substring(0, s1.lastIndexOf('/'));
				if (!remap.containsKey(className)){
					continue;
				}
				String methodName =  s1.substring(s1.lastIndexOf('/') + 1);
				String newMethodName =  s3.substring(s3.lastIndexOf('/') + 1);
				MappedClass mc = remap.get(className);
				//
				Object node = getMethodNode(mc.getNode().methods, methodName, desc);
				MappedMember method = new MappedMember(mc, node, 0, desc,methodName );
				method.setNewName(newMethodName);
				mc.addMethod(method);
			}
		}
		return remap;
	}
	
	private static FieldNode getFieldNode(List<FieldNode> fields, String name, String desc){
		for (FieldNode fn : fields){
			if (fn.name.equals(name) && fn.desc.equals(desc)){
				return fn;
			}
		}
		return null;
	}
	
	private static MethodNode getMethodNode(List<MethodNode> methods, String name, String desc){
		for (MethodNode mn : methods){
			if (mn.name.equals(name) && mn.desc.equals(desc)){
				return mn;
			}
		}
		return null;
	}
}
