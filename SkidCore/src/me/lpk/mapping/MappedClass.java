package me.lpk.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

public class MappedClass extends MappedObject {
	/**
	 * Field index : Field
	 */
	private final Map<Integer, MappedMember> fieldMap = new HashMap<Integer, MappedMember>();
	/**
	 * Method index : Method
	 */
	private final Map<Integer, MappedMember> methodMap = new HashMap<Integer, MappedMember>();
	/**
	 * Original name : Child
	 */
	private final Map<String, MappedClass> children = new HashMap<String, MappedClass>();
	/**
	 * Original name : Inner Class
	 */
	private final Map<String, MappedClass> inners = new HashMap<String, MappedClass>();
	/**
	 * Interface index : Interface Class
	 */
	private final Map<Integer, MappedClass> interfaces = new HashMap<Integer, MappedClass>();
	//
	private int fieldIndex, methodIndex, interfaceIndex;
	private final ClassNode node;
	private MappedClass parent, outer;

	public MappedClass(ClassNode node, String nameNew) {
		super("CLASS", node.name, nameNew);
		this.node = node;
	}

	public Map<Integer, MappedMember> getFieldMap() {
		return fieldMap;
	}

	public Map<Integer, MappedMember> getMethodMap() {
		return methodMap;
	}

	public Collection<MappedMember> getFields() {
		return fieldMap.values();
	}

	public Collection<MappedMember> getMethods() {
		return methodMap.values();
	}

	/**
	 * Finds a field given a name.
	 * 
	 * @param name
	 * @param useOriginalName
	 * @return
	 */
	public int findFieldByName(String name, boolean useOriginalName) {
		for (int key : fieldMap.keySet()) {
			MappedMember mm = fieldMap.get(key);
			if (useOriginalName ? mm.getOriginalName().equals(name) : mm.getNewName().equals(name)) {
				return key;
			}
		}
		return -1;
	}

	/**
	 * Finds a method given a name.
	 * 
	 * @param name
	 * @param useOriginalName
	 * @return
	 */
	public int findMethodByName(String name, boolean useOriginalName) {
		for (int key : methodMap.keySet()) {
			MappedMember mm = methodMap.get(key);
			if (useOriginalName ? mm.getOriginalName().equals(name) : mm.getNewName().equals(name)) {
				return key;
			}
		}
		return -1;
	}

	/**
	 * Finds a method given a name and description.
	 * 
	 * @param name
	 * @param useOriginalName
	 * @return
	 */
	public MappedMember findMethodByNameAndDesc(String name, String desc, boolean useOriginalName) {
		for (MappedMember mm : getMethods()) {
			if (mm.getDesc().equals(desc) && useOriginalName ? mm.getOriginalName().equals(name) : mm.getNewName().equals(name)) {
				return mm;
			}
		}
		return null;
	}
	
	/**
	 * Finds a field given a name and description.
	 * 
	 * @param name
	 * @param useOriginalName
	 * @return
	 */
	public MappedMember findFieldByNameAndDesc(String name, String desc, boolean useOriginalName) {
		for (MappedMember mm : getFields()) {
			if (mm.getDesc().equals(desc) && useOriginalName ? mm.getOriginalName().equals(name) : mm.getNewName().equals(name)) {
				return mm;
			}
		}
		return null;
	}

	/**
	 * Finds a field given a descriptor.
	 * 
	 * @param desc
	 * @return
	 */
	public List<Integer> findFieldsByDesc(String desc) {
		List<Integer> indecies = new ArrayList<Integer>();
		for (int key : fieldMap.keySet()) {
			MappedMember mm = fieldMap.get(key);
			if (mm.getDesc().equals(desc)) {
				indecies.add(key);
			}
		}
		return indecies;
	}

	/**
	 * Finds a method given a descriptor.
	 * 
	 * @param desc
	 * @return
	 */
	public List<Integer> findMethodsByDesc(String desc) {
		List<Integer> indecies = new ArrayList<Integer>();
		for (int key : methodMap.keySet()) {
			MappedMember mm = methodMap.get(key);
			if (mm.getDesc().equals(desc)) {
				indecies.add(key);
			}
		}
		return indecies;
	}

	/**
	 * Finds a field given an index.
	 * 
	 * @param key
	 * @return
	 */
	public MappedMember findFieldByIndex(int key) {
		return fieldMap.get(key);
	}

	/**
	 * Finds a field given an index.
	 * 
	 * @param key
	 * @return
	 */
	public MappedMember findMethodByIndex(int key) {
		return methodMap.get(key);
	}

	/**
	 * Adds a child instance to the class.
	 * 
	 * @param child
	 */
	public void addChild(MappedClass child) {
		child.setParent(this);
		children.put(child.getOriginalName(), child);
	}

	/**
	 * Adds a child instance to the class as an interface.
	 * 
	 * @param child
	 */
	public void addInterfaceImplementation(MappedClass child) {
		child.addInterface(this);
		children.put(child.getOriginalName(), child);
	}

	/**
	 * Adds an interface to the class.
	 * @param interfaze
	 */
	private void addInterface(MappedClass interfaze) {
		interfaces.put(interfaceIndex, interfaze);
		interfaceIndex++;
	}

	/**
	 * Adds an inner class to this class.
	 * 
	 * @param child
	 */
	public void addInnerClass(MappedClass child) {
		child.setOuterClass(this);
		inners.put(child.getOriginalName(), child);
	}

	/**
	 * Add a field to the class. Returns the field's index.
	 * 
	 * @param mm
	 * @return
	 */
	public int addField(MappedMember mm) {
		fieldMap.put(fieldIndex, mm);
		fieldIndex++;
		return fieldIndex - 1;
	}

	/**
	 * Returns the current method index.
	 * 
	 * @return
	 */
	public int getMethodIndex() {
		return methodIndex;
	}

	/**
	 * Returns the current field index.
	 * 
	 * @return
	 */
	public int getFieldIndex() {
		return fieldIndex;
	}

	/**
	 * Add a method to the class. Returns the method's index.
	 * 
	 * @param mm
	 * @return
	 */
	public int addMethod(MappedMember mm) {
		methodMap.put(methodIndex, mm);
		methodIndex++;
		return methodIndex - 1;
	}

	/**
	 * Returns true if the given name is a child of this class.
	 * 
	 * @param childName
	 * @return
	 */
	public boolean hasChild(String childName) {
		return children.containsKey(childName);
	}

	/**
	 * Returns the map of child instances the class has.
	 * 
	 * @return
	 */
	public Map<String, MappedClass> getChildrenMap() {
		return children;
	}
	
	public Map<Integer, MappedClass> getInterfacesMap() {
		return interfaces;
	}

	/**
	 * Get's the ClassNode associated with the mapping.
	 * 
	 * @return
	 */
	public ClassNode getNode() {
		return node;
	}

	/**
	 * Get's the parent's mapped instance.
	 * 
	 * @return
	 */
	public MappedClass getParent() {
		return parent;
	}

	/**
	 * Set's the parent mapped instance.
	 * 
	 * @param parent
	 */
	public void setParent(MappedClass parent) {
		this.parent = parent;
	}

	/**
	 * Returns a map of inner classes.
	 * 
	 * @return
	 */
	public Map<String, MappedClass> getInnerClassMap() {
		return inners;
	}

	/**
	 * Returns the outer class.
	 * 
	 * @return
	 */
	public MappedClass getOuterClass() {
		return outer;
	}

	/**
	 * Returns true if this is an inner class.
	 * 
	 * @return
	 */
	public boolean isInnerClass() {
		return outer != null;
	}

	/**
	 * Set's the class's outer class.
	 * 
	 * @param outer
	 */
	public void setOuterClass(MappedClass outer) {
		this.outer = outer;
	}

	/**
	 * Obvious.
	 * 
	 * @return
	 */
	public boolean hasParent() {
		return parent != null;
	}

	public boolean hasParent(MappedClass parent) {
		if (parent.equals(this)){
			return true;
		}
		for (MappedClass interfaze : interfaces.values()){
			if (interfaze.equals(parent)){
				return true;
			}
		}
		if (hasParent()){
			return this.parent.hasParent(parent);
		}
		return false;
	}
}
