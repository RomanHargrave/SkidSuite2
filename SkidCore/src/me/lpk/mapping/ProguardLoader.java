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
				put("byte", "B");
				put("double", "D");
				put("float", "F");
				put("boolean", "Z");
				put("char", "C");
				put("short", "S");
			}
		};
	}

	/**
	 * Instantiates the loader with a map of classnodes to be mapped.
	 * 
	 * @param nodes
	 */
	public ProguardLoader(Map<String, ClassNode> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Returns a map of MappedClasses based on the nodes given in the
	 * constructor and the mapping file read through the parameter.
	 * 
	 * @param in
	 *            FileReader of the Proguard mappings file
	 * @return
	 */
	public Map<String, MappedClass> read(FileReader in) {
		try {
			return read(new BufferedReader(in));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Reads each line in a reader and parses mappings from the Proguard format.
	 * 
	 * @param fileReader
	 * @return
	 * @throws Exception
	 */
	public Map<String, MappedClass> read(BufferedReader fileReader) throws Exception {
		Map<String, MappedClass> origNameMap = new HashMap<String, MappedClass>();
		Map<String, MappedClass> newNameMap = new HashMap<String, MappedClass>();
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
					if (curClass != null) {
						origNameMap.put(curClass.getOriginalName(), curClass);
						newNameMap.put(curClass.getNewName(), curClass);
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
		Set<String> origMapSet = new HashSet<String>();
		origMapSet.addAll(origNameMap.keySet());
		for (String className : origMapSet) {
			MappedClass mappedClass = origNameMap.get(className);
			origNameMap = MappingGen.linkMappings(mappedClass, origNameMap);
		}
		for (String className : origMapSet) {
			MappedClass mappedClass = origNameMap.get(className);
			for (MappedMember field : mappedClass.getFields()) {
				field.setDesc(StringUtil.fixDescReverse(field.getDesc(), origNameMap, newNameMap));
			}
			for (MappedMember method : mappedClass.getMethods()) {
				method.setDesc(StringUtil.fixDescReverse(method.getDesc(), origNameMap, newNameMap));
			}
			origNameMap.put(className, mappedClass);
		}
		return origNameMap;
	}

	private boolean isMethod(String text) {
		return text.contains("(");
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
		MappedMember mm = new MappedMember(clazz, null, -1, desc, original);
		mm.setNewName(newName);
		clazz.addMethod(mm);
	}

	/**
	 * Painfully creates a proper ASM description (for a method) given a return
	 * type and parameters.
	 * 
	 * @param type
	 * @param parameters
	 * @return
	 */
	private String fixDesc(String type, String parameters) {
		// type : parameters
		// void : (java.lang.Iterable,java.lang.Iterable,java.util.Map)
		String strReturnDesc = null, strParamsDesc = "", typeNoArr = type.replace("[]", "");
		// Apply primitive names
		for (String key : primitives.keySet()) {
			if (typeNoArr.equals(key)) {
				strReturnDesc = getArrStr(type) + primitives.get(key);
			}
		}
		if (parameters.contains(",")) {
			// Multiple parameters
			String[] params = parameters.substring(1, parameters.length() - 1).split(",");
			for (String param : params) {
				boolean done = false;
				for (String key : primitives.keySet()) {
					if (param.replace("[]", "").equals(key)) {
						strParamsDesc += getArrStr(param) + primitives.get(key);
						done = true;
					}
				}

				if (!done) {
					strParamsDesc += getArrStr(param) + "L" + param.replace(".", "/").replace("[]", "") + ";";
				}
			}
		} else if (parameters.equals("()")) {
			// No parameters
			strParamsDesc = "";
		} else {
			// One parameter
			String param = parameters.substring(1, parameters.length() - 1);
			boolean done = false;
			for (String key : primitives.keySet()) {
				if (param.replace("[]", "").equals(key)) {
					strParamsDesc += getArrStr(param) + primitives.get(key);
					done = true;
				}
			}
			if (!done) {
				strParamsDesc += getArrStr(param) + "L" + param.replace("[]", "").replace(".", "/") + ";";
			}
		}
		strParamsDesc = "(" + strParamsDesc + ")";
		// Type is not just a primitive
		if (strReturnDesc == null) {
			strReturnDesc = "L" + typeNoArr.replace(".", "/") + ";";
		}
		return strParamsDesc + strReturnDesc;
	}

	/**
	 * Creates a proper ASM description given a type.
	 * 
	 * @param type
	 * @return
	 */
	private String fixDesc(String type) {
		// net.minecraft.util.IntHashMap$Entry[]
		String returnDesc = null, typeNoArr = type.replace("[]", "");
		// Apply primitive names
		for (String key : primitives.keySet()) {
			if (type.replace("[]", "").equals(key)) {
				returnDesc = getArrStr(type) + primitives.get(key);
			}
		}
		// Type is not just a primitive
		if (returnDesc == null) {
			returnDesc = "L" + typeNoArr.replace(".", "/") + ";";
		}
		return getArrStr(type) + returnDesc;
	}

	/**
	 * Returns a string that signifies array depth in the proper ASM format.
	 * 
	 * @param param
	 * @return
	 */
	private String getArrStr(String param) {
		String arrayPrefix = "";
		if (param.contains("[]")) {
			int array = 0;
			String paramCopy = param + "";
			while (paramCopy.contains("[]")) {
				array++;
				paramCopy = paramCopy.substring(paramCopy.indexOf("[]") + 2);
				for (int i = 0; i < array; i++) {
					arrayPrefix = "[" + arrayPrefix;
				}
			}
		}
		return arrayPrefix;
	}
}