package me.lpk;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.io.FileUtils;
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

import javax.swing.JPanel;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class Gui {
	private JFrame frame;
	private JTree treeFiles;
	private JTextPane txtpntesttitle;
	private String path, jarName, text;
	private JMenuItem mnSave;

	/**
	 * Entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new Gui();
	}

	/**
	 * Create the application.
	 */
	public Gui() {
		initialize();
		frame.setVisible(true);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 700, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JSplitPane splitPane = new JSplitPane();

		// Right side (Display)
		JScrollPane scrollPane = new JScrollPane();
		splitPane.setRightComponent(scrollPane);
		splitPane.setDividerLocation(150);
		JMenu mnFile = new JMenu("File");
		txtpntesttitle = new JTextPane();
		JMenuBar menuBar = new JMenuBar();
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		txtpntesttitle.setContentType("text/html");
		txtpntesttitle.setEditable(false);
		scrollPane.setViewportView(txtpntesttitle);
		scrollPane.setColumnHeaderView(menuBar);
		menuBar.add(mnFile);
		// Menu
		// TODO: Upload options
		mnSave = new JMenuItem("Save Report");
		mnSave.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				File report = new File(jarName+"-Scan.html");
				try {
					FileUtils.write(report, text);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), "Error saving file!", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		mnSave.setEnabled(false);
		mnFile.add(mnSave);
		// 
		//
		// Left side (JTree)
		JPanel pnlTree = new JPanel();
		File dir = new File(System.getProperty("user.dir"));
		treeFiles = new JTree(sort(getTreeFromDir(dir)));
		treeFiles.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treeFiles.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent event) {
				// Update the path
				path = event.getPath().toString();
				if (path.toLowerCase().endsWith(".jar]")) {
					path = path.substring(path.indexOf(", ") + 2, path.length() - 1);
					path = path.replace(", ", File.separator);
				}
			}
		});
		treeFiles.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				File file = new File(path);
				if (file.exists()) {
					scan(file);
				}
			}
			@Override public void mouseEntered(MouseEvent arg0){}
			@Override public void mouseExited(MouseEvent arg0){}
			@Override public void mousePressed(MouseEvent arg0){}
			@Override public void mouseReleased(MouseEvent arg0){}
		});
		splitPane.setLeftComponent(pnlTree);
		pnlTree.setLayout(new BorderLayout(0, 0));
		pnlTree.add(treeFiles, BorderLayout.CENTER);
	}

	/**
	 * Scan the file.
	 * 
	 * @param file
	 */
	private void scan(File file) {
		Map<String, ClassNode> nodes = null;
		try {
			nodes = JarUtil.loadClasses(file);
		} catch (IOException e1) {
			txtpntesttitle.setText("<html><body><span style=\" color: red;  \">" + e1.toString() + "</span></body></html>");
		}
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
		jarName = file.getName().substring(0, file.getName().length() - 4);
		text = th.toHTML(file.getName().substring(0, file.getName().indexOf(".")));
		txtpntesttitle.setText(text);
		new Thread(){
			// Instantly setting the caret position doesn't work, so delaying it 50 ms is a fair work-around.
			@Override public void run(){ try { sleep(50); } catch (InterruptedException e) { e.printStackTrace(); } txtpntesttitle.setCaretPosition(0);}
		}.start();
		mnSave.setEnabled(true);
	}

	/**
	 * Recursive generation of the JTree file display.
	 * 
	 * @param dir
	 * @return
	 */
	private DefaultMutableTreeNode getTreeFromDir(File dir) {
		// From a directory create a node
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(dir.getName());
		for (File file : dir.listFiles()) {
			// Iterate children
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(file.getName());
			if (file.isDirectory()) {
				// Recurse through the sub-directory
				DefaultMutableTreeNode subdir = getTreeFromDir(file);
				if (nodeModelContains(subdir, ".jar"))
					// Only add the node if the subdirectory contains jars.
					top.add(subdir);
			} else {
				// Add the file node if it's a jar
				if (file.getName().toLowerCase().endsWith(".jar")) {
					top.add(node);
				}
			}
		}
		return top;
	}

	/**
	 * Checks if a node's model contains the given text.
	 * 
	 * @param subdir
	 * @param text
	 * @return
	 */
	private boolean nodeModelContains(DefaultMutableTreeNode node, String text) {
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> e = node.children();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode dmtn = e.nextElement();
			if (dmtn.isLeaf() && dmtn.toString().toLowerCase().contains(text)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method by Adrian: [
	 * <a href="http://stackoverflow.com/a/15704264/5620200">StackOverflow</a> ]
	 * 
	 * @param node
	 * @return
	 */
	public DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {
		// sort alphabetically
		for (int i = 0; i < node.getChildCount() - 1; i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			String nt = child.getUserObject().toString();
			for (int j = i + 1; j <= node.getChildCount() - 1; j++) {
				DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
				String np = prevNode.getUserObject().toString();
				System.out.println(nt + " " + np);
				if (nt.compareToIgnoreCase(np) > 0) {
					node.insert(child, j);
					node.insert(prevNode, i);
				}
			}
			if (child.getChildCount() > 0) {
				sort(child);
			}
		}
		// put folders first - normal on Windows and some flavors of Linux but
		// not on Mac OS X.
		for (int i = 0; i < node.getChildCount() - 1; i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			for (int j = i + 1; j <= node.getChildCount() - 1; j++) {
				DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
				if (!prevNode.isLeaf() && child.isLeaf()) {
					node.insert(child, j);
					node.insert(prevNode, i);
				}
			}
		}
		return node;
	}
}
