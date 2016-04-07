package me.lpk.gui.listeners;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import me.lpk.MainWindow;
import me.lpk.gui.component.SearchResultEntry;
import me.lpk.util.SearchUtil;

public class SearchKeyListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			handle(e);
		}

		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {
			
		}
		
		private void handle(KeyEvent e){
			if (MainWindow.instance.getSearchText().length() > 3 || e.getKeyCode() == KeyEvent.VK_ENTER) {
				List<SearchResultEntry> results = null;
				switch (MainWindow.instance.getSearchType()) {
				case "Class":
					results = SearchUtil.findChildren(MainWindow.instance.getNodes().get(MainWindow.instance.getSearchText()));
					break;
				case "Method":
					results = SearchUtil.findMethods(MainWindow.instance.getSearchText());
					break;
				case "Field":
					results = SearchUtil.findFields(MainWindow.instance.getSearchText());
					break;
				case "String":
					results = SearchUtil.findStringsContaining(MainWindow.instance.getSearchText());
					break;
				}
				MainWindow.instance.getResultPanel().clearResults();
				for (SearchResultEntry result : results) {
					MainWindow.instance.getResultPanel().addResult(result);
				}
			}
		}
	}