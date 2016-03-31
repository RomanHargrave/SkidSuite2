package me.lpk;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;
import me.lpk.mapping.MappingProcessor;
import me.lpk.mapping.remap.ClassRemapper;
import me.lpk.mapping.remap.impl.ModeNone;
import me.lpk.util.JarUtil;

import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.awt.event.ActionEvent;

public class ReProguard {

	private JFrame frame;
	private JTextField txtJarLoc;
	private JTextField txtMapLoc;
	private JTextPane txtLog;
	private JButton btnUndoProguard;

	private JFileChooser chooser;
	private File jar, map;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ReProguard window = new ReProguard();
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
	public ReProguard() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("ReProguard");
		frame.setResizable(false);
		frame.setBounds(100, 100, 481, 285);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		txtJarLoc = new JTextField();
		txtJarLoc.setBounds(140, 11, 325, 23);
		frame.getContentPane().add(txtJarLoc);
		txtJarLoc.setColumns(10);

		JButton btnLoadJar = new JButton("Load Jar");
		btnLoadJar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					jar = fc.getSelectedFile();
					txtJarLoc.setText(jar.getAbsolutePath());
					if (map != null) {
						btnUndoProguard.setEnabled(true);
					}
				}
			}
		});
		btnLoadJar.setBounds(10, 11, 120, 23);
		frame.getContentPane().add(btnLoadJar);

		JButton btnLoadMappings = new JButton("Load Mappings");
		btnLoadMappings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getFileChooser();
				int val = fc.showOpenDialog(null);
				if (val == JFileChooser.APPROVE_OPTION) {
					map = fc.getSelectedFile();
					txtMapLoc.setText(map.getAbsolutePath());
					if (jar != null) {
						btnUndoProguard.setEnabled(true);
					}
				}
			}
		});
		btnLoadMappings.setBounds(10, 45, 120, 23);
		frame.getContentPane().add(btnLoadMappings);

		txtMapLoc = new JTextField();
		txtMapLoc.setColumns(10);
		txtMapLoc.setBounds(140, 45, 325, 23);
		frame.getContentPane().add(txtMapLoc);

		btnUndoProguard = new JButton("Undo Proguard");
		btnUndoProguard.addActionListener(new ActionListener() {
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

		});
		btnUndoProguard.setBounds(10, 79, 120, 26);
		btnUndoProguard.setEnabled(false);
		frame.getContentPane().add(btnUndoProguard);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setBounds(10, 116, 455, 131);
		frame.getContentPane().add(scrollPane);
		txtLog = new JTextPane();
		scrollPane.setViewportView(txtLog);
	}

	private void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses, String name) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses);
		try {
			out.putAll(JarUtil.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int renamed = 0;
		for (MappedClass mc : mappedClasses.values()) {
			if (mc.isTruelyRenamed()) {
				renamed++;
			}
		}
		JarUtil.saveAsJar(out, name);
	}

	private void log(String s) {
		txtLog.setText(txtLog.getText() + "\n" + s);
	}

	public JFileChooser getFileChooser() {
		if (chooser == null) {
			chooser = new JFileChooser();
			final String dir = System.getProperty("user.dir");
			final File fileDir = new File(dir);
			chooser.setCurrentDirectory(fileDir);
		}
		return chooser;
	}
}
