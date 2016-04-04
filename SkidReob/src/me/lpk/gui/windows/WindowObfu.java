package me.lpk.gui.windows;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JFileChooser;

import java.awt.BorderLayout;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.MappingRenamer;
import me.lpk.mapping.remap.MappingMode;
import me.lpk.mapping.remap.impl.*;
import me.lpk.util.JarUtil;

import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

public class WindowObfu {

	private JFrame frame;
	private File file;
	private JFileChooser chooser;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowObfu window = new WindowObfu();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WindowObfu() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 787, 529);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JSplitPane splitPaneMain = new JSplitPane();
		frame.getContentPane().add(splitPaneMain, BorderLayout.CENTER);

		JScrollPane scrollPaneOptions = new JScrollPane();
		splitPaneMain.setLeftComponent(scrollPaneOptions);

		JPanel panel = new JPanel();
		scrollPaneOptions.setViewportView(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JSplitPane splitpaneOptions = new JSplitPane();
		splitpaneOptions.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panel.add(splitpaneOptions);

		JPanel pnlNamingConventions = new JPanel();
		splitpaneOptions.setLeftComponent(pnlNamingConventions);
		pnlNamingConventions.setLayout(new BoxLayout(pnlNamingConventions, BoxLayout.Y_AXIS));

		JLabel lblNamingConventions = new JLabel("Naming Conventions");
		pnlNamingConventions.add(lblNamingConventions);
		JRadioButton rdNamesSimplified = new JRadioButton("Simplified");
		JRadioButton rdNamesRandom = new JRadioButton("Random");
		JRadioButton rdNamesUnicode = new JRadioButton("Unicode");
		rdNamesSimplified.setSelected(true);
		pnlNamingConventions.add(rdNamesSimplified);
		pnlNamingConventions.add(rdNamesRandom);
		pnlNamingConventions.add(rdNamesUnicode);

		JPanel panel_3 = new JPanel();
		JPanel pnlMain = new JPanel();
		splitpaneOptions.setRightComponent(panel_3);

		splitPaneMain.setRightComponent(pnlMain);
		pnlMain.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JButton btnLoadJar = new JButton("Load Jar");
		JButton btnObfuscate = new JButton("Obfuscate");
		btnLoadJar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
				}

			}
		});
		btnObfuscate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (file == null || !file.exists()) {
					return;
				}
				System.out.println("Starting");
				Map<String, ClassNode> libNodes = new HashMap<String, ClassNode>();
				Map<String, ClassNode> nodes = null;
				try {
					for (File lib : getLibraries()) {
						libNodes.putAll(JarUtil.loadClasses(lib));
					}
					//
					nodes = JarUtil.loadClasses(file);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
				System.out.println("Loaded nodes");
				Map<String, MappedClass> libMappings = MappingGen.mappingsFromNodesNoLinking(libNodes);
				for (MappedClass mc : libMappings.values()) {
					mc.setIsLibrary(true);
					for (MappedMember mm : mc.getFields()) {
						mm.setIsLibrary(true);
					}
					for (MappedMember mm : mc.getMethods()) {
						mm.setIsLibrary(true);
					}
				}
				Map<String, MappedClass> mappings = MappingGen.mappingsFromNodesNoLinking(nodes);
				System.out.println("Made mappings");
				mappings.putAll(libMappings);
				for (MappedClass mc : mappings.values()) {
					MappingGen.linkMappings(mc, mappings);
				}
				System.out.println("Linked mappings");
				MappingMode mode = modeByRadiobutton();
				mappings = MappingRenamer.remapClasses(mappings, mode);
				for (MappedClass mc : mappings.values()) {
					if (mc.getOriginalName().contains("realms")) {
						mc.setNewName(mc.getOriginalName());
					}
				}
				mappings.get("net/minecraft/client/main/Main").setNewName("Main");
				try {
					Map<String, byte[]> out = JarUtil.loadNonClassEntries(file);
					out.putAll(MappingProcessor.process(nodes, mappings));
					JarUtil.saveAsJar(out, file.getName() + "-Obf.jar");
					String data = "";
					for (MappedClass mc : mappings.values()) {
						data += mc.getOriginalName() + " -> " + mc.getNewName() + "\n";
					}
					org.apache.commons.io.FileUtils.write(new File("fuck.txt"), data);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}

			private MappingMode modeByRadiobutton() {
				if (rdNamesSimplified.isSelected()) {
					return new ModeSimple();
				} else if (rdNamesRandom.isSelected()) {
					return new ModeRandom(5);
				} else if (rdNamesUnicode.isSelected()) {
					return new ModeUnicodeEvil();
				}
				return new ModeNone();
			}
		});
		pnlMain.add(btnLoadJar);
		pnlMain.add(btnObfuscate);
	}

	protected JFileChooser getFileChooser() {
		if (chooser == null) {
			chooser = new JFileChooser();
			final String dir = System.getProperty("user.dir");
			final File fileDir = new File(dir);
			chooser.setCurrentDirectory(fileDir);
		}
		return chooser;
	}

	protected List<File> getLibraries() {
		List<File> files = new ArrayList<File>();
		files.add(new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
		//
		File libDir = new File("libraries");
		libDir.mkdirs();
		for (File lib : FileUtils.listFiles(libDir, new String[] {"jar"}, true)) {
			files.add(lib);
		}
		return files;
	}
}
