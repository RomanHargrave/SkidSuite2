package me.lpk;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.objectweb.asm.tree.ClassNode;
import me.lpk.gui.component.ASMDecompilePanel;
import me.lpk.gui.component.SearchResultPanel;
import me.lpk.gui.listeners.ASMMouseAdapter;
import me.lpk.gui.listeners.SearchKeyListener;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappingGen;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final String TITLE = "SkidASM";
	private static final int INIT_WIDTH = 1080, INIT_HEIGHT = 800;
	private Map<String, ClassNode> nodes = new HashMap<String, ClassNode>();
	private Map<String, MappedClass> mappings;
	private final JMenuBar menuBar;
	private final JTextField txtMenuSearch;
	private final JComboBox<String> searchType;
	private final ASMDecompilePanel asmPanel;
	private final SearchResultPanel searchResults;
	public static MainWindow instance;

	public static void main(String[] args) {
		new MainWindow().setVisible(true);
	}

	public MainWindow() {
		instance = this;
		menuBar = new JMenuBar();
		txtMenuSearch = new JTextField();
		searchType = new JComboBox<String>();
		asmPanel = new ASMDecompilePanel();
		searchResults = new SearchResultPanel();
		setup();
	}

	private void setup() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle(TITLE);
		String searchingTooltip = "<html>Searching:<br><br>" + 
				"Class:  package/subpackage/ClassName<br>" + 
				"Method: MethodName or ASM Description<br>"+
				"Field:  FieldName or ASM Description<br>"+
				"LDC: String/Constants</html>";
		setSize(INIT_WIDTH, INIT_HEIGHT);
		txtMenuSearch.setToolTipText(searchingTooltip );
		searchType.setToolTipText(searchingTooltip);
		JLabel searchLbl = new JLabel(" Search:  ");
		searchType.addItem("LDC");
		searchType.addItem("Class");
		searchType.addItem("Field");
		searchType.addItem("Method");
		txtMenuSearch.addKeyListener(new SearchKeyListener());
		menuBar.add(searchLbl);
		menuBar.add(txtMenuSearch);
		menuBar.add(searchType);
		asmPanel.setJarListener(new JarLoadListener());
		asmPanel.setMouseListener(new ASMMouseAdapter());
		searchResults.setup();

		// asmPanel // searchResults
		JSplitPane splitPane = new JSplitPane();
		splitPane.setLeftComponent(asmPanel);
		splitPane.setRightComponent(searchResults);
		splitPane.setDividerLocation(INIT_WIDTH - 250);
		add(menuBar, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
		
		// Hardcoding because fuck doing this on my own every time:
		asmPanel.openJar(new File("Base.jar"));
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

	public String getSearchText() {
		return txtMenuSearch.getText();
	}

	public String getSearchType() {
		return searchType.getSelectedItem().toString();
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
