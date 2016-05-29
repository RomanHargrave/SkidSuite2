package me.lpk.gui.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import me.lpk.MainWindow;

public class SearchResultPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JList<String> list = new JList<String>();
	private final Map<String, SearchResultEntry> results = new HashMap<String, SearchResultEntry>();
	private int lastIndex = -1;

	public void setup() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		setLayout(new BorderLayout());
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		list.setModel(listModel);
		list.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != MouseEvent.BUTTON1) {
					return;
				}
				// Double clicked
				// Once to set the index, and now its clicked again.
				if (list.getSelectedIndex() == lastIndex) {
					String path = getPath();
					if (path != null) {
						if (MainWindow.instance.getDecompilePanel().getCurrentNode() == null || !MainWindow.instance.getDecompilePanel().getCurrentNode().name.equals(path)) {
							MainWindow.instance.getDecompilePanel().decompile(path);
						}
						MainWindow.instance.getDecompilePanel().setIndex(getSelectedSearchEntry());
						// TODO: Have text indexes in the ASMPanel saved where
						// fields/methods are located
						// SearchResultEntry can then find the correct index
						// based on their field/method included
						// Index opcodes too if it doesn't RIP performance
						//
						// Override Textifier to do all this automatically?
					}
				}
				lastIndex = list.getSelectedIndex();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		});
		JScrollPane pane = new JScrollPane(list);
		pane.setPreferredSize(new Dimension(250, MainWindow.instance.getHeight()));
		add(pane, BorderLayout.CENTER);
	}

	private String getPath() {
		SearchResultEntry sre = getSelectedSearchEntry();
		return sre == null ? null : sre.getTarget() == null ? null : sre.getTarget().name;
	}

	private SearchResultEntry getSelectedSearchEntry() {
		return results.get(list.getSelectedValue());
	}

	public void clearResults() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		list.setModel(listModel);
	}

	public void addResult(SearchResultEntry result) {
		results.put(result.toString(), result);
		DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
		listModel.addElement(result.toString());
		list.setModel(listModel);
		list.invalidate();
	}
}
