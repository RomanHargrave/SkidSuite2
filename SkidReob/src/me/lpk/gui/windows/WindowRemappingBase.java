package me.lpk.gui.windows;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingProcessor;
import me.lpk.util.JarUtil;

import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.awt.event.ActionEvent;

public abstract class WindowRemappingBase {
	protected JFrame frame;
	protected JTextField txtJarLoc;
	protected JTextField txtMapLoc;
	protected JTextPane txtLog;
	protected JButton btnUndoProguard;
	protected JFileChooser chooser;
	protected File jar, map;


	/**
	 * Create the application.
	 */
	public WindowRemappingBase() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle(getTitle());
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

		btnUndoProguard = new JButton(getButtonText());
		btnUndoProguard.addActionListener(getButtonAction());
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

	protected abstract String getTitle() ;

	protected abstract ActionListener getButtonAction();

	protected abstract String getButtonText();

	protected void saveJar(File nonEntriesJar, Map<String, ClassNode> nodes, Map<String, MappedClass> mappedClasses, String name) {
		Map<String, byte[]> out = null;
		out = MappingProcessor.process(nodes, mappedClasses);
		try {
			out.putAll(JarUtil.loadNonClassEntries(nonEntriesJar));
		} catch (IOException e) {
			e.printStackTrace();
		}
		JarUtil.saveAsJar(out, name);
	}

	protected void log(String s) {
		txtLog.setText(txtLog.getText() + "\n" + s);
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
}