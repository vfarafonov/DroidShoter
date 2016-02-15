package com.weezlabs.tools.android.screenshoter;

import com.android.ddmlib.IDevice;
import com.sun.javafx.beans.annotations.NonNull;
import com.weezlabs.libs.screenshoter.Model.Device;
import com.weezlabs.libs.screenshoter.ScreenShooterManager;
import com.weezlabs.tools.android.screenshoter.ui.DevicesListRenderer;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Created by vfarafonov on 12.02.2016.
 */
public class ScreenshoterMainScreen {
	public static final int MAX_DEVICES_ROW_COUNT = 10;
	private final DevicesListRenderer devicesListRenderer_;

	private ScreenShooterManager screenShooterManager_;
	private JPanel ScreenshoterRootPanel;
	private JTextArea titleTextArea;
	private JComboBox devicesComboBox;
	private JTextField directoryTextField;
	private JButton startButton;
	private JButton cancelButton;
	private JButton resetButton;
	private JProgressBar deviceInfoProgressBar;
	private JTextField prefixTextField;
	private JTextField sleepTextField;
	private JTextArea deviceParamsTextArea;

	public ScreenshoterMainScreen() {
		// TODO: add devices listener
		ScreenShooterManager.getInstanceAsync(new ScreenShooterManager.ManagerInitListener() {
			@Override
			public void onManagerReady(ScreenShooterManager manager) {
				screenShooterManager_ = manager;
				IDevice[] devices = screenShooterManager_.getDevices();
				devicesComboBox.setModel(new DefaultComboBoxModel<IDevice>(devices));
				devicesListRenderer_.setAdbConnected_(true);
				deviceInfoProgressBar.setVisible(false);
				if (devices.length > 0) {
					getSelectedDeviceInfo();
				}
			}
		});
		devicesListRenderer_ = new DevicesListRenderer();
		devicesComboBox.setRenderer(devicesListRenderer_);
		devicesComboBox.setMaximumRowCount(MAX_DEVICES_ROW_COUNT);
		devicesComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					getDeviceInfo((IDevice) e.getItem());
				}
			}
		});

		directoryTextField.setText(ScreenShooterManager.DEFAULT_SCREENSHOTS_DIR);
		prefixTextField.setText(ScreenShooterManager.DEFAULT_SCREENSHOTS_PREFIX);
		sleepTextField.setText(String.valueOf(ScreenShooterManager.DEFAULT_SLEEP_TIME_MS));
		// TODO: add integer filter to sleep text field
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("ScreenshoterMainScreen");
		frame.setContentPane(new ScreenshoterMainScreen().ScreenshoterRootPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void getSelectedDeviceInfo() {
		IDevice iDevice = (IDevice) devicesComboBox.getSelectedItem();
		if (iDevice != null) {
			getDeviceInfo(iDevice);
		}
	}

	private void getDeviceInfo(@NonNull IDevice iDevice) {
		devicesComboBox.setEnabled(false);
		deviceParamsTextArea.setVisible(false);
		deviceInfoProgressBar.setVisible(true);
		ScreenShooterManager.getDeviceDisplayInfoAsync(iDevice, new ScreenShooterManager.DeviceInfoListener() {
			@Override
			public void onDeviceInfoUpdated(Device device) {
				deviceParamsTextArea.setText(device.getCurrentResolution().toString() + ", " + device.getCurrentDpi().toString());
				deviceInfoProgressBar.setVisible(false);
				deviceParamsTextArea.setVisible(true);

				startButton.setEnabled(true);
				resetButton.setEnabled(true);

				devicesComboBox.setEnabled(true);
			}

			@Override
			public void onDeviceUpdateFailed(IDevice iDevice, Exception e) {
				deviceParamsTextArea.setText("Failed to get device parameters");
				deviceInfoProgressBar.setVisible(false);
				deviceParamsTextArea.setVisible(true);
				devicesComboBox.setEnabled(true);
			}
		});
	}
}
