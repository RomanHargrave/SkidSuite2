package me.lpk.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class WindowMappingConverter {

	private JFrame frame;

	
	
	
	// TODO: Ability to export to any mapping type
	// 		 Add more supported mapping types
	//		  - DashO
	//		  - Stringer
	//		  - Allatori
	//		  - ZKM
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowMappingConverter window = new WindowMappingConverter();
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
	public WindowMappingConverter() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
