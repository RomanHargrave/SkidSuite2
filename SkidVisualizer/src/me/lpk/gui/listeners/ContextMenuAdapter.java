package me.lpk.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import me.lpk.MainWindow;
import me.lpk.gui.component.DecompileSelection;
import me.lpk.gui.component.MethodSimulatorPanel;
import me.lpk.gui.component.SearchResultEntry;
import me.lpk.util.SearchUtil;

public class ContextMenuAdapter extends MouseAdapter {
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			return;
		}
		DecompileSelection selection = MainWindow.instance.getASMPanel().getSelection();
		if (selection == null) {
			return;
		}
		JPopupMenu context = new JPopupMenu();
		if (selection.getNode() == null) {
			JMenuItem contextError = new JMenuItem("<html>Could not find the owner class for the selection: <i>" + selection.getSelection() + "</i></html>");
			context.add(contextError);
			JScrollPane scroll = MainWindow.instance.getASMPanel().getTextScroll();
			context.show(MainWindow.instance.getASMPanel(), e.getX() + scroll.getX(), e.getY() - scroll.getVerticalScrollBar().getValue());
			return;
		}
		String typeData = selection.isClass() ? selection.getNode().name
				: selection.isField() ? selection.getField().name : selection.isMethod() ? selection.getMethod().name : selection.getType().name();
		JMenuItem contextType = new JMenuItem("Selected Type[" + selection.getType().name() + "]: " + typeData);
		JMenuItem contextParent = new JMenuItem("Parent: " + (selection.getNode().superName == null ? "null" : selection.getNode().superName));
		contextType.setEnabled(false);
		contextParent.setEnabled(false);
		context.add(contextType);
		context.add(contextParent);
		String outer = SearchUtil.getOuter(selection.getNode());
		if (selection.getNode().superName == null) {
			contextParent.setToolTipText("Could not locate the parent class in the loaded Jar file.");
		} else if (outer != null) {
			JMenuItem contextOuter = new JMenuItem("Outer Class: " + selection.getNode().outerClass);
			context.add(contextOuter);
		}
		if (selection.isClass()) {
			JMenuItem searchParent = new JMenuItem("Navigate to parent class");
			searchParent.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					MainWindow.instance.getASMPanel().decompile(selection.getNode().superName);
				}
			});
			JMenuItem searchChildren = new JMenuItem("Find children of " + (selection.getNode().name));
			searchChildren.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<SearchResultEntry> results = SearchUtil.findChildren(selection.getNode());
					MainWindow.instance.getResultPanel().clearResults();
					for (SearchResultEntry result : results) {
						MainWindow.instance.getResultPanel().addResult(result);
					}
				}
			});
			JMenuItem searchReferences = new JMenuItem("Find references to " + selection.getNode().name);
			searchReferences.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<SearchResultEntry> results = SearchUtil.findReferences(selection.getNode());
					MainWindow.instance.getResultPanel().clearResults();
					for (SearchResultEntry result : results) {
						MainWindow.instance.getResultPanel().addResult(result);
					}
				}
			});
			if (selection.getNode().superName == null || (selection.getNode().superName != null && selection.getNode().superName.equals("java/lang/Object"))) {
				searchParent.setToolTipText("The parent of '" + selection.getNode().name + "' could not be found.");
				searchParent.setEnabled(false);

			}
			context.add(searchParent);
			if (outer != null) {
				JMenuItem searchOuter = new JMenuItem("Navigate to outer class");
				searchOuter.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						MainWindow.instance.getASMPanel().decompile(outer);
					}
				});
				context.add(searchOuter);
			}
			context.add(searchChildren);
			context.add(searchReferences);
		} else if (selection.isField()) {
			if (selection.getField() != null) {
				JMenuItem searchReferences = new JMenuItem("Find references to " + selection.getField().name);
				searchReferences.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						List<SearchResultEntry> results = SearchUtil.findReferences(selection.getNode(), selection.getField());
						MainWindow.instance.getResultPanel().clearResults();
						for (SearchResultEntry result : results) {
							MainWindow.instance.getResultPanel().addResult(result);
						}
					}
				});
				context.add(searchReferences);
			}
		} else if (selection.isMethod()) {
			if (selection.getMethod() != null) {
				JMenuItem searchReferences = new JMenuItem("Find references to " + selection.getMethod().name);
				searchReferences.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						List<SearchResultEntry> results = SearchUtil.findReferences(selection.getNode(), selection.getMethod());
						MainWindow.instance.getResultPanel().clearResults();
						for (SearchResultEntry result : results) {
							MainWindow.instance.getResultPanel().addResult(result);
						}
					}
				});
				JMenuItem analyzeStack = new JMenuItem("Analyze stack of " + selection.getMethod().name);
				analyzeStack.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						MethodSimulatorPanel.load(selection.getMethod());
					}
				});
				context.add(searchReferences);
				context.add(analyzeStack);
			}
		} else if (selection.isString()) {
			JMenuItem searchContaining = new JMenuItem("Search strings with '" + selection.getSelection() + "'");
			searchContaining.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<SearchResultEntry> results = SearchUtil.findStringsContaining(selection.getSelection());
					MainWindow.instance.getResultPanel().clearResults();
					for (SearchResultEntry result : results) {
						MainWindow.instance.getResultPanel().addResult(result);
					}
				}
			});
			JMenuItem searchSimiliar = new JMenuItem("Search for similiar strings '" + selection.getSelection() + "'");
			searchSimiliar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<SearchResultEntry> results = SearchUtil.findStringsSimiliar(selection.getSelection());
					MainWindow.instance.getResultPanel().clearResults();
					for (SearchResultEntry result : results) {
						MainWindow.instance.getResultPanel().addResult(result);
					}
				}
			});
			context.add(searchContaining);
			context.add(searchSimiliar);
		}
		JScrollPane scroll = MainWindow.instance.getASMPanel().getTextScroll();
		context.show(MainWindow.instance.getASMPanel(), e.getX() + scroll.getX(), e.getY() - scroll.getVerticalScrollBar().getValue());
	}
}