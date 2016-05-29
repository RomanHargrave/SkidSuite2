package me.lpk.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

public class SwingUtils {
	/**
	 * Method by Adrian: [
	 * <a href="http://stackoverflow.com/a/15704264/5620200">StackOverflow</a> ]
	 * & Mike: [ <a href=
	 * "http://stackoverflow.com/questions/1542170/arranging-nodes-in-a-jtree">
	 * StackOverflow</a> ]
	 * 
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {
		List<DefaultMutableTreeNode> children = Collections.list(node.children());
		List<String> orgCnames = new ArrayList<String>();
		List<String> cNames = new ArrayList<String>();
		DefaultMutableTreeNode temParent = new DefaultMutableTreeNode();
		for (DefaultMutableTreeNode child : children) {
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode) child;
			temParent.insert(ch, 0);
			cNames.add(ch.toString().toUpperCase());
			orgCnames.add(ch.toString().toUpperCase());
			if (!child.isLeaf()) {
				sort(child);
			}
		}
		Collections.sort(cNames);
		for (String name : cNames) {
			int indx = orgCnames.indexOf(name);
			node.insert(children.get(indx), node.getChildCount());
		}
		// Fixing folder placement
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
