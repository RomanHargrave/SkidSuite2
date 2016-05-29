package me.lpk.util;

import javax.swing.tree.DefaultMutableTreeNode;

public class SwingUtils {
	/**
	 * Method by Adrian: [
	 * <a href="http://stackoverflow.com/a/15704264/5620200">StackOverflow</a> ]
	 * 
	 * @param node
	 * @return
	 */
	public static DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {
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
