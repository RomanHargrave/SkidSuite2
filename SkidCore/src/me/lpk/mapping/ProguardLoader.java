package me.lpk.mapping;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.util.StringUtil;

class ProguardLoader {
	private final Map<String, ClassNode> nodes;
	private final static Map<String, String> primitives;

	static {
		primitives = new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;

			{
				put("void", "V");
				put("int", "I");
				put("long", "J");
				put("double", "D");
				put("float", "F");
				put("boolean", "Z");
				put("char", "C");
				put("short", "S");
			}
		};
	}

	public ProguardLoader(Map<String, ClassNode> nodes) {
		this.nodes = nodes;
	}

	public Map<String, MappedClass> read(FileReader in) {
		try {
			return read(new BufferedReader(in));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, MappedClass> read(BufferedReader fileReader) throws Exception {
		Map<String, MappedClass> remap = new HashMap<String, MappedClass>();
		Map<String, MappedClass> remap2 = new HashMap<String, MappedClass>();
		String line = null;
		MappedClass curClass = null;
		while ((line = fileReader.readLine()) != null) {
			if (!line.contains("->")) {
				continue;
			}
			String[] parts = line.trim().split(" ");
			try {
				if (line.trim().endsWith(":")) {
					// Class definition
					curClass = readClass(parts);
					// System.err.println(curClass.getOriginalName() + ":" +
					// curClass.getNewName());
					if (curClass != null) {
						remap.put(curClass.getOriginalName(), curClass);
						remap2.put(curClass.getNewName(), curClass);
					}
				} else if (curClass != null) {
					if (isMethod(line.trim())) {
						// Method definition
						addMethod(curClass, parts);
					} else {
						// Field definition
						addField(curClass, parts);
					}
				}
			} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				throw new Exception("Malformed line:\n" + line);
			}
		}
		// Fixing the MappedClass's parent / child structure.
		Set<String> x = new HashSet<String>();
		x.addAll(remap.keySet());
		for (String className : x) {
			MappedClass mappedClass = remap.get(className);
			remap = MappingGen.linkMappings(mappedClass, remap);
		}
		for (String className : x) {
			MappedClass mappedClass = remap.get(className);
			for (MappedMember field : mappedClass.getFields()) {
				field.setDesc(StringUtil.fixDescReverse(field.getDesc(), remap, remap2));
			}
			for (MappedMember method : mappedClass.getMethods()) {
				method.setDesc(StringUtil.fixDescReverse(method.getDesc(), remap, remap2));
			}
		}
		/*
		 * for (String className : remap.keySet()) { MappedClass classMap =
		 * remap.get(className); // MappedClass has no parent. if
		 * (classMap.getParent() == null) { // Find its parent. MappedClass
		 * parent = remap.get(classMap.getNode().superName); // If found, set
		 * it's parent. Have the parent set it as its // child. if (parent !=
		 * null) { classMap.setParent(parent); parent.addChild(classMap); } }
		 * else { // MappedClass has parent. // If the parent does not have it
		 * as a child, add it. if
		 * (!classMap.getParent().getChildrenMap().containsKey(classMap.
		 * getOriginalName())) { classMap.getParent().addChild(classMap); } } }
		 */
		return remap;
	}

	private boolean isMethod(String trim) {
		return trim.contains("(");
	}

	/**
	 * Generating mapping for a class.
	 * 
	 * @param parts
	 * @return
	 */
	private MappedClass readClass(String[] parts) {
		String original = parts[0].replace(".", "/");
		String obfuscated = parts[2].replace(".", "/").substring(0, parts[2].length() - 1);
		ClassNode node = nodes.get(obfuscated);
		MappedClass mc = new MappedClass(node, obfuscated);
		if (mc != null) {
			mc.setNewName(original);
		}
		return mc;
	}

	/**
	 * Add a field to the given class.
	 * 
	 * @param clazz
	 * @param parts
	 */
	private void addField(MappedClass clazz, String[] parts) {
		String newName = parts[1];
		String original = parts[3];
		String desc = fixDesc(parts[0]);
		// (MappedClass owner, Object memberNode, int index, String desc, String
		// orig)
		MappedMember mm = new MappedMember(clazz, null, -1, desc, original);
		mm.setNewName(newName);
		clazz.addField(mm);
	}

	/**
	 * Add a method to the given class.
	 * 
	 * @param clazz
	 * @param parts
	 */
	private void addMethod(MappedClass clazz, String[] parts) {
		String newName = parts[1].substring(0, parts[1].indexOf("("));
		String original = parts[3];
		String desc = fixDesc(parts[0], parts[1].substring(parts[1].indexOf("(")));
		// (MappedClass owner, Object memberNode, int index, String desc, String
		// orig)
		MappedMember mm = new MappedMember(clazz, null, -1, desc, original);
		mm.setNewName(newName);
		clazz.addMethod(mm);
	}

	private String fixDesc(String type, String parameters) {
		// type : parameters
		// void : (java.lang.Object)
		// void : (java.lang.Iterable,java.lang.Iterable,java.util.Map)
		String type_array_prefix = "", type_mid = null, type_params = "", type_noArr = type.replace("[]", "");
		// Apply primitive names
		for (String key : primitives.keySet()) {
			if (type_noArr.equals(key)) {
				type_mid = primitives.get(key);
			}
		}
		if (parameters.contains(",")) {
			// Multiple parameters
			String[] params = parameters.substring(1, parameters.length() - 1).split(",");
			for (String param : params) {
				boolean done = false;
				for (String key : primitives.keySet()) {
					if (param.equals(key)) {
						type_params += primitives.get(key);
						done = true;
					}
				}
				if (!done) {
					type_params += "L" + param.replace(".", "/") + ";";
				}

			}
		} else if (parameters.equals("()")) {
			// No parameters
			type_params = "";
		} else {
			// One parameter
			String param = parameters.substring(1, parameters.length() - 1);
			boolean done = false;
			for (String key : primitives.keySet()) {
				if (param.equals(key)) {
					type_params += primitives.get(key);
					done = true;
				}
			}
			if (!done) {
				type_params += "L" + param.replace(".", "/") + ";";
			}
		}
		type_params = "(" + type_params + type_params + ")";
		// Checking for arrays
		if (type.contains("[]")) {
			int array = 0;
			String type_copy = type + "";
			while (type_copy.contains("[]")) {
				array++;
				type_copy = type_copy.substring(type_copy.indexOf("[]") + 2);
				for (int i = 0; i < array; i++) {
					type_array_prefix = "[" + type_array_prefix;
				}
			}
		}
		// Type is not just a primitive
		if (type_mid == null) {
			type_mid = "L" + type_noArr.replace(".", "/") + ";";
		}
		return type_array_prefix + type_params + type_mid;
	}

	private String fixDesc(String type) {
		// net.minecraft.util.IntHashMap$Entry[]
		String type_array_prefix = "", type_mid = null, type_noArr = type.replace("[]", "");
		// Apply primitive names
		for (String key : primitives.keySet()) {
			if (type_noArr.equals(key)) {
				type_mid = primitives.get(key);
			}
		}
		// Checking for arrays
		if (type.contains("[]")) {
			int array = 0;
			String type_copy = type + "";
			while (type_copy.contains("[]")) {
				array++;
				type_copy = type_copy.substring(type_copy.indexOf("[]") + 2);
				for (int i = 0; i < array; i++) {
					type_array_prefix = "[" + type_array_prefix;
				}
			}
		}
		// Type is not just a primitive
		if (type_mid == null) {
			type_mid = "L" + type_noArr.replace(".", "/") + ";";
		}
		return type_array_prefix + type_mid;
	}
}