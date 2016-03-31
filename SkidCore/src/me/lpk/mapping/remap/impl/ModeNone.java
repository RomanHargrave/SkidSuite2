package me.lpk.mapping.remap.impl;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.mapping.remap.MappingMode;

public class ModeNone extends MappingMode {
	@Override
	public String getClassName(ClassNode cn) {
		return cn.name;
	}

	@Override
	public String getMethodName(MethodNode mn) {
		return mn.name;
	}

	@Override
	public String getFieldName(FieldNode fn) {
		return fn.name;
	}
}