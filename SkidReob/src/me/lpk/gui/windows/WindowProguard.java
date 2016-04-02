package me.lpk.gui.windows;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.util.JarUtil;

public class WindowProguard extends WindowRemappingBase {
	public static void showWindow() {
		WindowProguard window = new WindowProguard();
		window.frame.setVisible(true);
	}

	@Override
	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Map<String, ClassNode> nodes = JarUtil.loadClasses(jar);
					log("Loaded nodes from jar: " + jar.getAbsolutePath());
					Map<String, MappedClass> mappedClasses = MappingGen.mappingsFromProguard(map, nodes);
					log("Loaded mappings from proguard mappings: " + map.getAbsolutePath());
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
		return "Proguard Remapper";
	}

	@Override
	protected String getButtonText() {
		return "Undo Proguard";
	}
}
