package me.lpk.gui.component;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

public class MethodSimulatorPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public MethodSimulatorPanel() {
		setup();
	}

	private void setup() {
		setLayout(new BorderLayout());
		JSplitPane splitPane = new JSplitPane();
		add(splitPane);
		
		JPanel panelButtons = new JPanel();
		splitPane.setLeftComponent(panelButtons);
		panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.Y_AXIS));
		
		JButton btnNext = new JButton("Next");
		panelButtons.add(btnNext);
		
		JButton btnPrev = new JButton("Prev");
		panelButtons.add(btnPrev);
		
		JButton btnRsc = new JButton("RSC");
		panelButtons.add(btnRsc);
		
		JButton btnEdit = new JButton("Edit");
		panelButtons.add(btnEdit);
		
		JPanel panelDisplay = new JPanel();
		splitPane.setRightComponent(panelDisplay);
		panelDisplay.setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPaneDisplay = new JSplitPane();
		splitPaneDisplay.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panelDisplay.add(splitPaneDisplay);
		
		JScrollPane scrollOpcodes = new JScrollPane();
		splitPaneDisplay.setLeftComponent(scrollOpcodes);
		
		JList listOpcodes = new JList();
		scrollOpcodes.setViewportView(listOpcodes);
		
		JScrollPane scrollStackValues = new JScrollPane();
		splitPaneDisplay.setRightComponent(scrollStackValues);
		
		JList listStackValues = new JList();
		scrollStackValues.setViewportView(listStackValues);
	}
}
