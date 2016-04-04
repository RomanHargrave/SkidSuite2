package me.lpk.mapping;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

class EnigmaLoader {
	private final Map<String, ClassNode> nodes;

	public EnigmaLoader(Map<String, ClassNode> nodes) {
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
		int lineNumber = 0;
		String line = null;
		MappedClass curClass = null;
		while ((line = fileReader.readLine()) != null) {
			lineNumber++;
			int commentPos = line.indexOf('#');
			if (commentPos >= 0) {
				line = line.substring(0, commentPos);
			}
			if (line.trim().length() <= 0) {
				continue;
			}
			int indent = 0;
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) != '\t') {
					break;
				}
				indent++;
			}
			String[] parts = line.trim().split("\\s");
			try {
				// read the first token
				String token = parts[0];
				if (token.equalsIgnoreCase("CLASS")) {
					if (indent <= 0) {
						// outer class
						curClass = readClass(parts);
						remap.put(curClass.getOriginalName(), curClass);
					} else {
						// inner class
						// TODO: If Engima is ever updated so that inner classes
						// aren't borked add this.
					}
				} else if (token.equalsIgnoreCase("FIELD")) {
					if (curClass == null) {
						throw new Exception("Unexpected FIELD entry (Line: " + lineNumber + " )");
					}
					addField(curClass, parts);
				} else if (token.equalsIgnoreCase("METHOD")) {
					if (curClass == null) {
						throw new Exception("Unexpected METHOD entry (Line: " + lineNumber + " )");
					}
					addMethod(curClass, parts);
				} else if (token.equalsIgnoreCase("ARG")) {
					// SkidGUI does not map method args yet.
					if (curClass == null) {
						throw new Exception("Unexpected ARG entry (Line: " + lineNumber + " )");
					}
				}
			} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				throw new Exception("Malformed line:\n" + line);
			}
		}
		// Fixing the MappedClass's parent / child structure.
		for (String className : remap.keySet()) {
			MappedClass mappedClass = remap.get(className);
			remap = MappingGen.linkMappings(mappedClass, remap);
		}
		return remap;
	}

	/**
	 * Generating mapping for a class.
	 * 
	 * @param parts
	 * @return
	 */
	private MappedClass readClass(String[] parts) {
		String original = parts[1];
		if (original.startsWith("none/")) {
			original = original.substring("none/".length());
		}
		MappedClass mc = null;
		if (parts.length == 2) {
			mc = new MappedClass(nodes.get(original), original);
		} else if (parts.length == 3) {
			String newName = parts[2];
			mc = new MappedClass(nodes.get(original), newName);
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
		String original = "";
		String newName = "";
		String desc = "";
		if (parts.length == 3) {
			original = parts[1];
			newName = parts[1];
			desc = parts[2];
		} else if (parts.length == 4) {
			original = parts[1];
			newName = parts[2];
			desc = parts[3];
		} else {
			return;
		}
		if (desc.contains("Lnone/")) {
			desc = desc.replace("Lnone/", "L");
		}
		MappedMember mm = new MappedMember(clazz, findField(clazz.getNode(), original, desc), -1, desc, original);
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
		String original = "";
		String newName = "";
		String desc = "";
		if (parts.length == 3) {
			original = parts[1];
			newName = parts[1];
			desc = parts[2];
		} else if (parts.length == 4) {
			original = parts[1];
			newName = parts[2];
			desc = parts[3];
		} else {
			return;
		}
		if (desc.contains("Lnone/")) {
			desc = desc.replace("Lnone/", "L");
		}
		MappedMember mm = new MappedMember(clazz, findMethod(clazz.getNode(), original, desc), -1, desc, original);
		mm.setNewName(newName);
		clazz.addMethod(mm);
	}

	private FieldNode findField(ClassNode cn, String name, String desc) {
		if (cn == null) {
			return null;
		}
		for (FieldNode fn : cn.fields){
			if (fn.desc.equals(desc) && fn.name.equals(name)){
				return fn;
			}
		}
		return null;
	}

	private MethodNode findMethod(ClassNode cn, String name, String desc) {
		if (cn == null) {
			return null;
		}
		for (MethodNode mn : cn.methods){
			if (mn.desc.equals(desc) && mn.name.equals(name)){
				return mn;
			}
		}
		return null;
	}
}