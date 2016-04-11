package me.lpk.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import me.lpk.MainWindow;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.MappedMember;
import me.lpk.util.ASMUtil;
import me.lpk.util.JarUtil;
import me.lpk.util.OpUtil;
import me.lpk.util.ParentUtils;
import me.lpk.util.StringUtil;

public class ASMDecompilePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTree tree;
	private JScrollPane scrollFiles = new JScrollPane();
	private StyledDocument doc = new DefaultStyledDocument();
	private JTextPane txtEdit = new JTextPane(doc);
	private JScrollPane scrollEdit = new JScrollPane();
	private Map<String, ClassNode> nodes = new HashMap<String, ClassNode>();
	private Map<String, Map<String, Integer>> methodIndecies = new HashMap<String, Map<String, Integer>>();
	private Map<String, Map<String, Integer>> fieldIndecies = new HashMap<String, Map<String, Integer>>();
	private ClassNode currNode;
	private ActionListener jarOpenListner;
	private MouseAdapter mouseListener;

	public ASMDecompilePanel() {
		setup(true);
	}

	public ASMDecompilePanel(boolean supportDrag) {
		setup(supportDrag);
	}

	private void setup(boolean supportDrag) {
		JSplitPane splitPane = new JSplitPane();
		splitPane.setDividerLocation(260);
		this.setLayout(new BorderLayout());
		this.add(splitPane, BorderLayout.CENTER);
		if (supportDrag) {
			this.setDropTarget(new DropTarget() {
				private static final long serialVersionUID = 1L;

				@Override
				public final void drop(final DropTargetDropEvent event) {
					try {
						event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
						final Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if (transferData == null) {
							return;
						}
						@SuppressWarnings("unchecked")
						final Iterator<File> iterator = ((List<File>) transferData).iterator();
						while (iterator.hasNext()) {
							final File file;
							if ((file = iterator.next()).getName().endsWith("jar")) {
								openJar(new File(file.getAbsolutePath()));
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}

			});
		}
		scrollEdit.setViewportView(txtEdit);
		txtEdit.setEditable(false);
		splitPane.setRightComponent(scrollEdit);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(supportDrag ? "Drag and drop a file!" : "Open a file!");
		tree = new JTree(root);
		tree.setEditable(true);
		scrollFiles.setViewportView(tree);
		splitPane.setLeftComponent(scrollFiles);
	}

	/**
	 * Loads a jar file into the file navigator and updates the map of
	 * ClassNodes.
	 * 
	 * @param file
	 */
	public void openJar(File file) {
		// Skidded from JByteEdit
		// <3 you Quux
		// final ArrayList<JarEntry> entries = Collections.list(file.entries());
		// final Iterator<JarEntry> iterator = (Iterator<JarEntry>)
		// entries.iterator();
		final StringTreeNode root = new StringTreeNode(file.getName(), "");
		try {
			nodes = JarUtil.loadClasses(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (nodes == null) {
			return;
		}
		for (ClassNode classNode : nodes.values()) {
			final ArrayList<String> dirPath = new ArrayList<String>(Arrays.asList(classNode.name.split("/")));
			StringTreeNode parent = root;
			while (dirPath.size() > 0) {
				final String section = dirPath.get(0);
				StringTreeNode node;
				if ((node = parent.getChild(section)) == null) {
					final StringTreeNode newDir = new StringTreeNode(section, classNode.name);
					parent.addChild(section, newDir);
					parent.add(newDir);
					node = newDir;
				}
				parent = node;
				dirPath.remove(0);
			}
		}
		tree = new JTree(root);
		tree.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tree.getSelectionPath() != null) {
					String path = tree.getSelectionPath().toString();
					path = path.substring(1, path.length() - 1);
					while (path.contains(", ")) {
						path = path.replace(", ", "/");
					}
					path = path.substring(path.indexOf("/") + 1);
					if (nodes.containsKey(path)) {
						decompile(path);
					}
				}
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
		if (jarOpenListner != null) {
			jarOpenListner.actionPerformed(null);
		}
		scrollFiles.setViewportView(tree);
		scrollFiles.repaint();
	}

	/**
	 * Converts the selected path into syntax-highlighted bytecode.
	 * 
	 * @param path
	 */
	public void decompile(String path) {
		ClassNode cn = nodes.get(path);
		if (cn == null) {
			System.err.println(path + " IS NOT A CLASSNODE!");
			return;
		}
		currNode = cn;
		ClassReader cr = new ClassReader(ASMUtil.getNodeBytes(cn));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		cr.accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(ps)), 0);
		String output;
		try {
			output = os.toString("UTF8");

			txtEdit.setText(output);
			SimpleAttributeSet attribClean = new SimpleAttributeSet();
			StyleConstants.setForeground(attribClean, Color.black);
			doc.setCharacterAttributes(0, output.length(), attribClean, true);
			//
			SimpleAttributeSet attribKeyword = new SimpleAttributeSet();
			SimpleAttributeSet attribComment = new SimpleAttributeSet();
			SimpleAttributeSet attribQuote = new SimpleAttributeSet();
			SimpleAttributeSet attribOpcode = new SimpleAttributeSet();
			SimpleAttributeSet attribSig = new SimpleAttributeSet();
			StyleConstants.setForeground(attribKeyword, new Color(110, 70, 160));
			StyleConstants.setForeground(attribComment, new Color(20, 100, 20));
			StyleConstants.setForeground(attribQuote, new Color(80, 80, 80));
			StyleConstants.setForeground(attribOpcode, new Color(50, 90, 120));
			StyleConstants.setForeground(attribSig, new Color(120, 30, 10));
			// StyleConstants.setItalic(set, false);
			String[] keywords = new String[] { "class", "public", "private", "protected", "static", "final", "volatile", "abstract", "synthetic", "bridge", "implements",
					"extends", "enum", "interface", "OUTERCLASS", "INNERCLASS" };
			for (String target : keywords) {
				int index = output.indexOf(target);
				while (index >= 0) {
					doc.setCharacterAttributes(index, target.length(), attribKeyword, true);
					index = output.indexOf(target, index + 1);
				}
			}
			String[] signatures = new String[] { "[", "(", "L" };
			for (String target : signatures) {
				int index = output.indexOf(target);
				while (index >= 0) {
					if (target.equals("L") && (StringUtil.isNumeric(output.substring(index + 1, index + 2)) || output.substring(index - 1, index).equals("/"))) {
						index = output.indexOf(target, index + 1);
						continue;
					}
					int newline = output.substring(index).indexOf(" ");
					if (newline == -1) {
						newline = output.substring(index).indexOf("\n");
					}
					doc.setCharacterAttributes(index, newline, attribSig, true);
					index = output.indexOf(target, index + 1);
				}
			}
			Set<String> extraOpcodes = new HashSet<String>();
			extraOpcodes.addAll(Arrays.asList("LINENUMBER", "MAXSTACK", "MAXLOCALS", "FRAME", "LOCALVARIABLE", "TRYCATCHBLOCK"));
			extraOpcodes.addAll(OpUtil.getOpcodes());
			for (String opcode : extraOpcodes) {
				int i = output.indexOf(opcode);
				while (i >= 0) {
					doc.setCharacterAttributes(i, opcode.length(), attribOpcode, true);
					i = output.indexOf(opcode, i + 1);
				}
			}
			String quote = "\"";
			int i = output.indexOf(quote);
			while (i >= 0) {
				int quoteLen = output.substring(i + 1).indexOf("\"") + 1;
				if (quoteLen > 0) {
					doc.setCharacterAttributes(i, quoteLen, attribQuote, true);
				} else {
					quoteLen = 1;
				}
				i = output.indexOf(quote, i + quoteLen + 1);
			}

			String comment = "//";
			i = output.indexOf(comment);
			while (i >= 0) {
				int newline = output.substring(i).indexOf("\n");
				doc.setCharacterAttributes(i, newline, attribComment, true);
				i = output.indexOf(comment, i + 1);
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		// TODO: Setup method/field indecies based on 'output'
		methodIndecies.clear();
		fieldIndecies.clear();

		txtEdit.setCaretPosition(0);
	}

	/**
	 * Returns a selection object containing data about the selected text.
	 * 
	 * @return
	 */
	public ASMDecompileSelection getSelection() {
		String text = txtEdit.getSelectedText();
		if (text == null || text.length() <= 0) {
			return null;
		}
		int start = txtEdit.getSelectionStart(), end = txtEdit.getSelectionEnd(), i = 0, ix = 0;
		String tmp = text, tmp2 = text;
		while (!tmp.contains("\n")) {
			try {
				tmp += txtEdit.getText(end + i, 1);
				i += 1;
			} catch (BadLocationException e) {
				e.printStackTrace();
				break;
			}
		}
		boolean forwards = true;
		while (!tmp2.contains(" ")) {
			try {
				if (forwards) {
					String next = txtEdit.getText(end + ix, 1);
					if (!next.equals(" ")) {
						tmp2 += next;
					} else {
						forwards = false;
						ix = 0;
					}
				} else {
					String next = txtEdit.getText(start - ix, 1);
					if (next.equals(" ")) {
						break;
					} else {
						tmp2 = next + tmp2;
					}
				}
				ix += 1;
			} catch (BadLocationException e) {
				e.printStackTrace();
				break;
			}
		}
		int i2 = 1;
		while (!tmp.substring(0, tmp.length() - 1).contains("\n")) {
			try {
				tmp = txtEdit.getText(start - i2, 1) + tmp;
				i2 += 1;
			} catch (BadLocationException e) {
				e.printStackTrace();
				break;
			}
		}
		boolean containsTemp = nodes.containsKey(tmp2);
		boolean containsExact = nodes.containsKey(text);
		if (tmp.contains(" class ") || tmp.contains(" interface ") || containsExact || containsTemp) {
			if (containsTemp) {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Class, text, nodes.get(tmp2));
			} else if (containsExact) {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Class, text, nodes.get(text));
			} else {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Class, text, currNode);
			}
		}
		if (tmp.contains("(")) {
			if (tmp.contains("INVOKESTATIC") || tmp.contains("INVOKEINTERFACE") || tmp.contains("INVOKEVIRTUAL") || tmp.contains("INVOKESPECIAL")) {
				// Get method's owner from tmp
				ClassNode owner = classFromLine(tmp);
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Method, text, owner, methodFromLine(owner, tmp));
			} else {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Method, text, currNode, methodFromLine(tmp));
			}
		} else {
			if (tmp.contains("\"")) {
				if (!tmp.endsWith("\"")) {
					// When the entire string is selected it just ignroes the
					// last character. Ugly fix.
					try {
						tmp += txtEdit.getText(end, 1);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
				// Fixing indexes for ease of searching.
				start = tmp.indexOf(text);
				end = start + text.length() + 1;
				if (tmp.indexOf("\"") <= start && end <= tmp.lastIndexOf("\"")) {
					return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.String, text, currNode);
				}
			}
			if (tmp.contains("GETSTATIC") || tmp.contains("PUTSTATIC") || tmp.contains("GETFIELD") || tmp.contains("PUTFIELD")) {
				// Get method's owner from tmp
				ClassNode owner = classFromLine(tmp);
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Field, text, classFromLine(tmp), fieldFromLine(owner, tmp));
			} else if (tmp.contains("NEW")) {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Class, text, classFromLine(tmp));
			} else {
				return new ASMDecompileSelection(ASMDecompileSelection.SelectionType.Field, text, currNode, fieldFromLine(tmp));
			}
		}
	}

	private MethodNode methodFromLine(ClassNode owner, String tmp) {
		// Clean the input and split by spaces
		String[] split = tmp.trim().split(" ");
		if (split.length < 3 || owner == null) {
			return null;
		}
		String name = split[1];
		String desc = split[2];

		// should be className.object
		// So cut everything before '.'
		String methodStr = name.substring(name.indexOf(".") + 1);
		Map<String, MappedClass> mappings = MainWindow.instance.getMappings();
		MappedMember member = ParentUtils.findMethod(mappings.get(owner.name), methodStr, desc);
		if (member != null) {
			return member.getMethodNode();
		}
		return null;
	}

	private FieldNode fieldFromLine(ClassNode owner, String tmp) {
		// Clean the input and split by spaces
		String[] split = tmp.trim().split(" ");
		if (split.length < 3 || owner == null) {
			return null;
		}
		String name = split[split.length - 3];
		String desc = split[split.length - 1];
		// should be className.object
		// So cut everything before '.'
		name = name.substring(name.indexOf(".") + 1);
		Map<String, MappedClass> mappings = MainWindow.instance.getMappings();
		MappedMember member = ParentUtils.findField(mappings.get(owner.name), name, desc);
		if (member != null) {
			return member.getFieldNode();
		}
		return null;
	}

	private MethodNode methodFromLine(String tmp) {
		// Clean the input and split by spaces
		String[] split = tmp.trim().split(" ");
		if (split.length < 2) {
			return null;
		}
		String data = split[split.length - 1];
		String name = data.substring(0, data.indexOf("("));
		String desc = data.substring(data.indexOf("("));
		Map<String, MappedClass> mappings = MainWindow.instance.getMappings();
		MappedMember member = ParentUtils.findMethod(mappings.get(currNode.name), name, desc);
		if (member != null) {
			return member.getMethodNode();
		}
		return null;
	}

	private FieldNode fieldFromLine(String tmp) {
		// Clean the input and split by spaces
		String[] split = tmp.trim().split(" ");
		if (split.length < 3) {
			return null;
		}
		boolean hasValue = tmp.contains("=");
		String name = split[split.length - (hasValue ? 3 : 1)];
		String desc = split[split.length - (hasValue ? 4 : 2)];
		// should be className.object
		// So cut everything before '.'
		Map<String, MappedClass> mappings = MainWindow.instance.getMappings();
		MappedMember member = ParentUtils.findField(mappings.get(currNode.name), name, desc);
		if (member != null) {
			return member.getFieldNode();
		}
		return null;
	}

	/**
	 * Gets the owner in the given line. Returns null if the owner has not been
	 * loaded as a ClassNode.
	 * 
	 * @param line
	 * @return
	 */
	public ClassNode classFromLine(String line) {
		// Clean the input and split by spaces
		String[] split = line.trim().split(" ");
		String s = split[1];
		// should be className.object
		// So cut up to '.'
		int dotIndex = s.indexOf(".");
		String classStr = s;
		if (dotIndex != -1) {
			classStr = s.substring(0, dotIndex);
		}
		return nodes.get(classStr);
	}

	public void setIndex(SearchResultEntry result) {
		if (result.getMethod() != null) {
			System.out.println(methodIndecies.size());
			Map<String, Integer> map = methodIndecies.get(result.getMethod().desc);
			if (map != null) {
				System.out.println("WEW");

				String name = result.getMethod().name;
				if (map.containsKey(name)) {
					int index = map.get(name);
					System.out.println(index);
					txtEdit.grabFocus();
					txtEdit.setCaretPosition(index);
					txtEdit.setSelectionStart(index);
					txtEdit.setSelectionEnd(index + name.length());
				}
			}
		}
	}

	public void setJarListener(ActionListener listener) {
		this.jarOpenListner = listener;
	}

	public Map<String, ClassNode> getNodes() {
		return nodes;
	}

	public ClassNode getCurrentNode() {
		return currNode;
	}

	public void setNode(String className, ClassNode node) {
		nodes.put(className, node);
	}

	public void setNodes(Map<String, ClassNode> nodes) {
		this.nodes = nodes;
	}

	public MouseAdapter getMouseListener() {
		return mouseListener;
	}

	public void setMouseListener(MouseAdapter newListener) {
		MouseAdapter oldListener = getMouseListener();
		if (oldListener != null) {
			txtEdit.removeMouseListener(oldListener);
		}
		mouseListener = newListener;
		txtEdit.addMouseListener(newListener);
	}

	public JScrollPane getTextScroll() {
		return scrollEdit;
	}
}
