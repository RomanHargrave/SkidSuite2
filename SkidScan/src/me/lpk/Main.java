package me.lpk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.threat.ThreatScanner;
import me.lpk.threat.handlers.classes.CClassLoader;
import me.lpk.threat.handlers.classes.CSuspiciousSynth;
import me.lpk.threat.handlers.classes.CWinRegHandler;
import me.lpk.threat.handlers.methods.MClassLoader;
import me.lpk.threat.handlers.methods.MFileIO;
import me.lpk.threat.handlers.methods.MNativeInterface;
import me.lpk.threat.handlers.methods.MNetworkRef;
import me.lpk.threat.handlers.methods.MRuntime;
import me.lpk.threat.handlers.methods.MWebcam;
import me.lpk.util.JarUtil;

public class Main {
	public static void main(String[] args) {
		String currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
		File curDir = new File(System.getProperty("user.dir"));
		for (File file : curDir.listFiles()){
			if (!file.getAbsolutePath().endsWith(".jar")){
				continue;
			}
			if (file.getName().contains(currentJar)){
				continue;
			}
			Map<String, ClassNode> nodes = loadNodes(file);
			ThreatScanner th = new ThreatScanner();
			th.registerThreat(new CSuspiciousSynth());
			th.registerThreat(new CWinRegHandler());
			th.registerThreat(new CClassLoader());
			th.registerThreat(new MClassLoader());
			th.registerThreat(new MFileIO());
			th.registerThreat(new MWebcam());
			th.registerThreat(new MRuntime());
			th.registerThreat(new MNetworkRef());
			th.registerThreat(new MNativeInterface());
			for (ClassNode cn : nodes.values()) {
				th.scan(cn);
			}
			try {
				save(th.toHTML(file.getName()), file.getName().substring(0, file.getName().indexOf(".")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			//
			// TODO: GUI with the ability to click threats.
			// Would open the class in SkidEdit(SkidViewer)
		}
		
	}

	private static void save(String html, String name) throws IOException {
		FileWriter fw = new FileWriter(new File(name + "-scan.html"));
		fw.write(html);
		fw.close();
	}

	private static Map<String, ClassNode> loadNodes(File night) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtil.loadClasses(night);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (nodes == null) {
			System.err.println("COULD NOT READ CLASSES FROM " + night.getAbsolutePath());
			return null;
		}
		return nodes;
	}
}
