package me.lpk;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;

import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.optimizer.ClassOptimizer;
import org.objectweb.asm.optimizer.JarOptimizer;
import org.objectweb.asm.optimizer.MethodOptimizer;
import org.objectweb.asm.tree.ClassNode;
import me.lpk.log.Logger;
import me.lpk.mapping.MappedClass;
import me.lpk.mapping.SkidRemapper;
import me.lpk.util.Classpather;
import me.lpk.util.JarUtil;
import me.lpk.util.LazySetupMaker;

import javax.swing.ListSelectionModel;

public class Shrinker {
	private JList<String> list;
	private JCheckBox chkMaxs, chkNoRemoval;
	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Shrinker window = new Shrinker();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Shrinker() {
		initialize();
	}

	private void shrink(File jar) {
		long origSize = jar.length();
		LazySetupMaker lsm = LazySetupMaker.get(jar.getAbsolutePath(), false);
		Map<String, byte[]> out = new HashMap<String, byte[]>();
		try {
			out.putAll(JarUtil.loadNonClassEntries(jar));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String mainClass = JarUtil.getManifestMainClass(jar);
		boolean isLibrary = chkNoRemoval.isSelected() || mainClass == null;
		Logger.logLow("Compting classes to ignore... (Processing time scales exponentially with file size)");
		Set<String> keep = isLibrary ? null : Remover.evaluate(mainClass, lsm.getNodes());
		isLibrary = isLibrary || keep.size() == 0;
		Logger.logLow("Optimizing classes...");
		if (!chkMaxs.isSelected()) {
			try {
				Classpather.addFile(jar);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		Remapper m = new SkidRemapper(new HashMap<String, MappedClass>());
		for (ClassNode cn : lsm.getNodes().values()) {
			try {
				if (isLibrary || keep.contains(cn.name)) {
					ClassWriter cw = new ClassWriter(chkMaxs.isSelected() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES);
					ClassVisitor remapper = new MyClassOptimizer(cw, m);
					cn.accept(remapper);
					out.put(cn.name, cw.toByteArray());
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Please export with 'Extract required libraries into Jar' and try again.\nOr click the checkbox 'Use Maxs'.",
						"Error: " + e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		Logger.logLow("Saving...");
		File newJar = new File(jar.getName().substring(0, jar.getName().indexOf(".")) + "-opti.jar");
		JarUtil.saveAsJar(out, newJar.getName());
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			JarOptimizer.optimize(newJar);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long newSize = newJar.length();
		DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
		double l = (0.0 + newSize) / (0.0 + origSize);
		int i = Integer.parseInt((l + "").substring(2, 4));
		String x = (100 - i) + "%";
		model.addElement(jar.getName() + ": " + x);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 549, 381);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel lbloptimizedJarsWill = new JLabel("*Optimized jars will be placed ajacent to this program. Optimization only affects file size, not performance.");
		lbloptimizedJarsWill.setFont(new Font("Tahoma", Font.ITALIC, 11));
		lbloptimizedJarsWill.setBackground(SystemColor.desktop);
		frame.getContentPane().add(lbloptimizedJarsWill, BorderLayout.SOUTH);

		JPanel panelx = new JPanel();
		JPanel panelxx = new JPanel();
		panelx.setLayout(new BorderLayout());
		panelxx.setLayout(new BoxLayout(panelxx, BoxLayout.Y_AXIS));
		list = new JList<String>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBackground(SystemColor.control);
		DefaultListModel<String> model = new DefaultListModel<String>();
		model.addElement("File: Percent Shrink        ");
		list.setModel(model);
		panelx.add(list, BorderLayout.CENTER);
		chkNoRemoval = new JCheckBox("No Removal");
		chkMaxs = new JCheckBox("Use Maxs");
		chkNoRemoval.setToolTipText("This may be needed when 'Use Maxs' is included");
		chkMaxs.setToolTipText("This may require you to start shrunk programs with '-noverify'.");
		panelxx.add(chkMaxs);
		panelxx.add(chkNoRemoval);
		panelx.add(panelxx, BorderLayout.SOUTH);
		frame.getContentPane().add(panelx, BorderLayout.EAST);

		JPanel panel = new JPanel();
		panel.setBackground(SystemColor.controlHighlight);
		panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblDragAJar = new JLabel("Drag a jar here for optimization");
		panel.add(lblDragAJar);
		lblDragAJar.setBackground(new Color(240, 240, 240));
		lblDragAJar.setHorizontalAlignment(SwingConstants.CENTER);

		TransferHandler handler = new TransferHandler() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport info) {
				return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean importData(TransferHandler.TransferSupport info) {
				if (!info.isDrop())
					return false;
				Transferable t = info.getTransferable();
				List<File> data = null;
				try {
					data = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				} catch (Exception e) {
					return false;
				}
				for (File jar : data)
					if (jar.getName().toLowerCase().endsWith(".jar"))
						shrink(jar);
				return true;
			}
		};
		lblDragAJar.setTransferHandler(handler);
	}

	class MyClassOptimizer extends ClassOptimizer {
		public MyClassOptimizer(ClassVisitor cv, Remapper remapper) {
			super(cv, remapper);
		}

		@Override
		protected MethodVisitor createMethodRemapper(MethodVisitor mv) {
			return new MyMethodOptimizer(this, mv, remapper);
		}
	}

	class MyMethodOptimizer extends MethodOptimizer {
		public MyMethodOptimizer(ClassOptimizer classOptimizer, MethodVisitor mv, Remapper remapper) {
			super(classOptimizer, mv, remapper);
		}

		@Override
		public void visitLdcInsn(Object cst) {
			if (mv != null) {
				mv.visitLdcInsn(remapper.mapValue(cst));
			}
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (mv != null) {
				mv.visitMethodInsn(opcode, remapper.mapType(owner), remapper.mapMethodName(owner, name, desc), remapper.mapMethodDesc(desc), itf);
			}
		}
	}
}
