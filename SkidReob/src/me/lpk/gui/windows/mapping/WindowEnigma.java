package me.lpk.gui.windows.mapping;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.util.JarUtil;

public class WindowEnigma extends WindowRemappingBase {
	public static void showWindow() {
		WindowEnigma window = new WindowEnigma();
		window.frame.setVisible(true);
	}

	@Override
	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Map<String, ClassNode> nodes = JarUtil.loadClasses(jar);
					log("Loaded nodes from jar: " + jar.getAbsolutePath());
					Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromEnigma(map, nodes);
					log("Loaded mappings from engima mappings: " + map.getAbsolutePath());
					saveJar(jar, nodes, mappedClasses, jar.getName() + "-re.jar");
					log("Saved modified file!");

				} catch (IOException e1) {
					log(e1.getMessage());
				}
			}
		};
	}

	@Override
	protected String getTitle() {
		return "Enigma Remapper";
	}

	@Override
	protected String getButtonText() {
		return "Remap";
	}
}
