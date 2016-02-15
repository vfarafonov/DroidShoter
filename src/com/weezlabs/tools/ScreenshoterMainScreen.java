package com.weezlabs.tools;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Created by vfarafonov on 12.02.2016.
 */
public class ScreenshoterMainScreen {
	private JPanel ScreenshoterRootPanel;
	private JTextArea titleTextArea;
	private JComboBox devicesComboBox;
	private JTextField screenshotsTextField;
	private JButton startButton;
	private JButton cancelButton;
	private JButton resetButton;
	private JProgressBar deviceinfoProgressBar;
	private JTextField prefixTextField;
	private JTextField sleepTextField;

	public static void main(String[] args) {
		JFrame frame = new JFrame("ScreenshoterMainScreen");
		frame.setContentPane(new ScreenshoterMainScreen().ScreenshoterRootPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
