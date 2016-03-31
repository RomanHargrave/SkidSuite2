package me.lpk.threat.handlers.methods;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.lpk.threat.handlers.MethodHandler;

import me.lpk.threat.result.ThreatResult;
import me.lpk.util.StringUtil;

public class MNetworkRef extends MethodHandler {

	@Override
	public ThreatResult scanMethod(MethodNode mn) {
		List<String> methods = new ArrayList<String>();
		int opIndex = 0;
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			if (ain.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode min = (MethodInsnNode) ain;
				if (min.owner.contains("java/net/") || min.owner.contains("java/nio/channels/") || min.owner.contains("javax/net/") || min.owner.contains("sun/net/")) {
					methods.add(toLocation(opIndex, mn.name, min));
				}
			} else if (ain.getType() == AbstractInsnNode.LDC_INSN) {
				LdcInsnNode ldc = (LdcInsnNode) ain;
				if (StringUtil.isIP(ldc.cst.toString()) || StringUtil.isLink(ldc.cst.toString())) {
					methods.add(toLocation(opIndex, mn.name, ldc));
				}
			}
			opIndex++;
		}
		if (methods.size() == 0) {
			return null;
		}
		return ThreatResult.fromMethod("Networking", "This class has online interactions.", mn, methods);
	}

}
