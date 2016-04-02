package me.lpk;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.objectweb.asm.tree.ClassNode;

import me.lpk.gui.component.ASMDecompilePanel;
import me.lpk.gui.component.SearchResultPanel;
import me.lpk.gui.listeners.ASMMouseAdapter;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final String TITLE = "SkidASM";
	private static final int INIT_WIDTH = 1080, INIT_HEIGHT = 800;
	private Map<String, ClassNode> nodes = new HashMap<String, ClassNode>();
	private Map<String, MappedClass> mappings;
	private final JMenuBar menuBar;
	private final JMenu menuFile, menuLoad, menuExit;
	private final JTextField txtMenuSearch;
	private final JComboBox<String> searchType;
	private final ASMDecompilePanel asmPanel;
	private final SearchResultPanel searchResults;
	public static MainWindow instance;

	public static void main(String[] args) {
		new MainWindow().setVisible(true);
		//TODO: Method simulation using: asm Analyzer, Frame, BasicInterpreter, BasicValue
	}

	public MainWindow() {
		instance = this;
		menuBar = new JMenuBar();
		menuFile = new JMenu("File");
		menuLoad = new JMenu("Load Jar");
		menuExit = new JMenu("Exit");
		txtMenuSearch = new JTextField();
		searchType = new JComboBox<String>();
		asmPanel = new ASMDecompilePanel();
		searchResults = new SearchResultPanel();
		setup();
	}

	private void setup() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle(TITLE);
		setSize(INIT_WIDTH, INIT_HEIGHT);
		txtMenuSearch.setToolTipText("<html>Searching:<br><br>" + "Class:  package/subpackage/ClassName<br>" + "Method: MethodName or ASM Description<br>"
				+ "Field:  FieldName or ASM Description</html>");
		menuFile.add(menuLoad);
		menuFile.add(menuExit);
		JLabel searchLbl = new JLabel("    Search:");
		searchType.addItem("Class");
		searchType.addItem("Field");
		searchType.addItem("Method");
		menuBar.add(menuFile);
		menuBar.add(searchLbl);
		menuBar.add(txtMenuSearch);
		menuBar.add(searchType);
		asmPanel.setJarListener(new JarLoadListener());
		asmPanel.setMouseListener(new ASMMouseAdapter());
		searchResults.setup();
		
		//asmPanel // searchResults
		JSplitPane splitPane = new JSplitPane();
		splitPane.setLeftComponent(asmPanel);
		splitPane.setRightComponent(searchResults);
		splitPane.setDividerLocation(INIT_WIDTH-250);
		add(menuBar, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
	}

	/**
	 * Keeping the nodes updated when a jar is opened.
	 */
	class JarLoadListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			setTitle("Loading classes...");
			nodes = asmPanel.getNodes();
			setTitle("Generating class connections...");
			mappings = MappingGen.mappingsFromNodes(nodes);
			setTitle(TITLE);
		}
	}

	public ASMDecompilePanel getASMPanel() {
		return asmPanel;
	}

	public SearchResultPanel getResultPanel() {
		return searchResults;
	}

	public Map<String, ClassNode> getNodes() {
		return nodes;
	}

	public Map<String, MappedClass> getMappings() {
		return mappings;
	}

	// Like the original SkidGUI but better.
	// Better size handling w/ scroll bars.
	// More data in shrinkable panes.
	// Better handling of what is related.
	//
	// DATA SHOWN:
	// ClassName, ParentName, Interfaces, Children
	// ---- Added to display when clicked
	// Fields<Name, Type, Value(if static primitive)>
	// ---- Where referenced in other classes
	// Methods<Name, Return, Parameters>
	// ---- Where referenced in other classes
	// ---- Other objects referenced
}
