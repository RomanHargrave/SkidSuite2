package me.lpk.mapping;

import java.util.Map;

import org.objectweb.asm.commons.Remapper;

import me.lpk.util.ParentUtils;
import me.lpk.util.StringUtil;

public class SkidRemapper extends Remapper {
	private final Map<String, MappedClass> renamed;

	public SkidRemapper(Map<String, MappedClass> renamed) {
		this.renamed = renamed;
	}

	@Override
	public String mapDesc(String desc) {
		return super.mapDesc(StringUtil.fixDesc(desc, renamed));
	}

	@Override
	public String mapType(String type) {
		if (type == null) {
			return null;
		}
		return super.mapType(StringUtil.fixDesc(type, renamed));
	}

	@Override
	public String[] mapTypes(String[] types) {
		for (int i = 0; i < types.length; i++) {
			types[i] = StringUtil.fixDesc(types[i], renamed);
		}
		return super.mapTypes(types);
	}

	@Override
	public String mapMethodDesc(String desc) {
		if ("()V".equals(desc)) {
			return desc;
		}
		return super.mapMethodDesc(StringUtil.fixDesc(desc, renamed));
	}

	@Override
	public Object mapValue(Object value) {
		return super.mapValue(value);
	}

	@Override
	public String mapSignature(String signature, boolean typeSignature) {
		if (signature == null) {
			return null;
		}
		String s = signature;
		try {
			s = super.mapSignature(StringUtil.fixDesc(signature, renamed), typeSignature);
		} catch (java.lang.StringIndexOutOfBoundsException e) {
			e.printStackTrace();
			System.out.println(signature);
		}
		return s;
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		MappedClass mc = renamed.get(owner);
		if (me.lpk.mapping.remap.MappingRenamer.isNameWhitelisted(name)) {
			return super.mapMethodName(owner, name, desc);
		}
		if (mc == null) {
			return super.mapMethodName(owner, name, desc);
		} else {
			MappedMember mm = ParentUtils.findMethod(mc, name, desc);
			if (mm != null) {
				return super.mapMethodName(owner, ParentUtils.findMethodOverride(mm).getNewName(), desc);
			}
		}
		return super.mapMethodName(owner, name, desc);
	}

	@Override
	public String mapInvokeDynamicMethodName(String name, String desc) {
		MappedClass mc = renamed.get(StringUtil.getMappedFromDesc(renamed, desc));
		if (me.lpk.mapping.remap.MappingRenamer.isNameWhitelisted(name)) {
			return super.mapInvokeDynamicMethodName(name, desc);
		}
		if (mc == null) {
			return super.mapInvokeDynamicMethodName(name, desc);
		} else {
			MappedMember mm = ParentUtils.findMethod(mc, name, desc);
			if (mm != null) {
				return super.mapInvokeDynamicMethodName(ParentUtils.findMethodOverride(mm).getNewName(), desc);
			}
		}
		return super.mapInvokeDynamicMethodName(name, desc);
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		MappedClass mc = renamed.get(owner);
		if (mc != null) {
			MappedMember field = ParentUtils.findField(mc, name, desc);
			if (field != null) {
				return super.mapFieldName(owner, field.getNewName(), desc);
			}
		}
		return super.mapFieldName(owner, name, desc);
	}

	@Override
	public String map(String typeName) {
		typeName = StringUtil.fixDesc(typeName, renamed);
		return super.map(typeName);
	}
}