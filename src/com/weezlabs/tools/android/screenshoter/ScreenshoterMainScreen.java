package com.weezlabs.tools.android.screenshoter;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.sun.javafx.beans.annotations.NonNull;
import com.weezlabs.libs.screenshoter.ScreenShooterManager;
import com.weezlabs.libs.screenshoter.model.Device;
import com.weezlabs.libs.screenshoter.model.Mode;
import com.weezlabs.tools.android.screenshoter.ui.DevicesListRenderer;
import com.weezlabs.tools.android.screenshoter.ui.ModesTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Created by vfarafonov on 12.02.2016.
 */
public class ScreenshoterMainScreen {
	public static final int MAX_DEVICES_ROW_COUNT = 10;
	private final DevicesListRenderer devicesListRenderer_;
	private final ModesTableModel modesTableModel_;

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
	private JTable modesTable;
	private JProgressBar jobProgressBar;
	private MainScreenStates currentState_;

	public ScreenshoterMainScreen() {
		// TODO: add devices listener
		setState(MainScreenStates.CONNECTING_ADB);
		ScreenShooterManager.getInstanceAsync(new ScreenShooterManager.ManagerInitListener() {
			@Override
			public void onManagerReady(ScreenShooterManager manager) {
				screenShooterManager_ = manager;
				IDevice[] devices = screenShooterManager_.getDevices();
				final DefaultComboBoxModel<IDevice> devicesComboBoxModel = new DefaultComboBoxModel<IDevice>(devices);
				devicesComboBox.setModel(devicesComboBoxModel);
				devicesListRenderer_.setAdbConnected_(true);
				deviceInfoProgressBar.setVisible(false);
				if (devices.length > 0) {
					setState(MainScreenStates.REQUESTING_INFO);
					getSelectedDeviceInfo();
				} else {
					setState(MainScreenStates.DEVICE_NOT_FOUND);
				}
				screenShooterManager_.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
					@Override
					public void deviceConnected(IDevice iDevice) {
					}

					@Override
					public void deviceDisconnected(IDevice iDevice) {
						devicesComboBoxModel.removeElement(iDevice);
						setState(checkGlobalState());
					}

					@Override
					public void deviceChanged(IDevice iDevice, int i) {
						if (iDevice.getState() == IDevice.DeviceState.ONLINE) {
							devicesComboBoxModel.addElement(iDevice);
						} else {
							devicesComboBoxModel.removeElement(iDevice);
						}
						setState(checkGlobalState());
					}
				});
			}
		});
		devicesListRenderer_ = new DevicesListRenderer();
		devicesComboBox.setRenderer(devicesListRenderer_);
		devicesComboBox.setMaximumRowCount(MAX_DEVICES_ROW_COUNT);
		devicesComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					deviceParamsTextArea.setText(null);
					setState(MainScreenStates.REQUESTING_INFO);
					getDeviceInfo((IDevice) e.getItem());
				}
			}
		});

		directoryTextField.setText(ScreenShooterManager.DEFAULT_SCREENSHOTS_DIR);
		prefixTextField.setText(ScreenShooterManager.DEFAULT_SCREENSHOTS_PREFIX);
		sleepTextField.setText(String.valueOf(ScreenShooterManager.DEFAULT_SLEEP_TIME_MS));
		// TODO: add integer filter to sleep text field

		modesTableModel_ = new ModesTableModel();
		modesTable.setModel(modesTableModel_);

		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (screenShooterManager_ != null) {
					updateProgress(0, 0);
					setState(MainScreenStates.JOB_IN_PROGRESS);
					screenShooterManager_.createScreenshotsForAllResolutionsAsync(
							new File(directoryTextField.getText()),
							prefixTextField.getText(),
							Integer.valueOf(sleepTextField.getText()),
							modesTableModel_.getExcludedModes(),
							new ScreenShooterManager.ScreenShotJobProgressListener() {
								@Override
								public void onScreenshotJobFinished() {
									resetDeviceDisplay();
								}

								@Override
								public void onScreenshotJobFailed() {
									resetDeviceDisplay();
								}

								@Override
								public void onScreenshotJobCancelled() {
									resetDeviceDisplay();
								}

								@Override
								public void onScreenshotJobProgressUpdate(int currentProgress, int totalCount) {
									updateProgress(currentProgress, totalCount);
								}
							}
					);
				}
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setState(MainScreenStates.CANCELLING_JOB);
				screenShooterManager_.stopScreenshotsJob();
			}
		});
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetDeviceDisplay();
			}
		});
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("ScreenshoterMainScreen");
		frame.setContentPane(new ScreenshoterMainScreen().ScreenshoterRootPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Updates progress bar. Resets it if totalCount = 0
	 */
	private void updateProgress(int currentProgress, int totalCount) {
		jobProgressBar.setMaximum(totalCount);
		jobProgressBar.setValue(totalCount != 0 ? currentProgress : jobProgressBar.getMinimum());
		jobProgressBar.setString(totalCount != 0 ? currentProgress + "/" + totalCount : "");
		jobProgressBar.setIndeterminate(totalCount == 0);
	}

	private void resetDeviceDisplay() {
		setState(MainScreenStates.RESETTING);
		screenShooterManager_.resetDeviceDisplayAsync(new ScreenShooterManager.CommandStatusListener() {
			@Override
			public void onCommandSentToDevice() {
				setState(checkGlobalState());
			}

			@Override
			public void onCommandExecutionFailed() {
				setState(checkGlobalState());
			}
		});
	}

	private void getSelectedDeviceInfo() {
		IDevice iDevice = (IDevice) devicesComboBox.getSelectedItem();
		screenShooterManager_.setDevice(new Device(iDevice));
		if (iDevice != null) {
			getDeviceInfo(iDevice);
		}
	}

	private void getDeviceInfo(@NonNull IDevice iDevice) {
		ScreenShooterManager.getDeviceDisplayInfoAsync(iDevice, new ScreenShooterManager.DeviceInfoListener() {
			@Override
			public void onDeviceInfoUpdated(Device device) {
				screenShooterManager_.setDevice(device);
				deviceParamsTextArea.setText(device.getCurrentResolution().toString() + ", " + device.getCurrentDpi().toString());
				modesTableModel_.clearRows();
				modesTableModel_.addAll(Mode.getModesQueue(device));
				setState(checkGlobalState());
			}

			@Override
			public void onDeviceUpdateFailed(IDevice iDevice, Exception e) {
				deviceParamsTextArea.setText("Failed to get device parameters");
				setState(checkGlobalState());
			}
		});
	}

	private MainScreenStates checkGlobalState() {
		if (screenShooterManager_ == null) {
			return MainScreenStates.CONNECTING_ADB;
		} else if (screenShooterManager_.getDevices().length == 0) {
			return MainScreenStates.DEVICE_NOT_FOUND;
		} else if (deviceParamsTextArea.getText().equals("")) {
			return MainScreenStates.REQUESTING_INFO;
		} else {
			return MainScreenStates.READY;
		}
	}

	private void setState(MainScreenStates newState) {
		currentState_ = newState;
		switch (newState) {
			case CONNECTING_ADB:
				startButton.setEnabled(false);
				cancelButton.setEnabled(false);
				resetButton.setEnabled(false);
				jobProgressBar.setVisible(false);
				devicesComboBox.setEnabled(false);
				deviceParamsTextArea.setVisible(false);
				deviceInfoProgressBar.setVisible(true);
				break;
			case DEVICE_NOT_FOUND:
				startButton.setEnabled(false);
				cancelButton.setEnabled(false);
				resetButton.setEnabled(false);
				modesTableModel_.clearRows();
				jobProgressBar.setVisible(false);
				devicesComboBox.setEnabled(false);
				deviceParamsTextArea.setVisible(false);
				deviceInfoProgressBar.setVisible(false);
				break;
			case REQUESTING_INFO:
				startButton.setEnabled(false);
				cancelButton.setEnabled(false);
				resetButton.setEnabled(true);
				modesTableModel_.clearRows();
				jobProgressBar.setVisible(false);
				devicesComboBox.setEnabled(false);
				deviceParamsTextArea.setVisible(false);
				deviceInfoProgressBar.setVisible(true);
				break;
			case READY:
				startButton.setEnabled(true);
				cancelButton.setEnabled(false);
				resetButton.setEnabled(true);
				jobProgressBar.setVisible(false);
				devicesComboBox.setEnabled(true);
				deviceParamsTextArea.setVisible(true);
				deviceInfoProgressBar.setVisible(false);
				break;
			case JOB_IN_PROGRESS:
				startButton.setEnabled(false);
				cancelButton.setEnabled(true);
				resetButton.setEnabled(false);
				jobProgressBar.setVisible(true);
				devicesComboBox.setEnabled(false);
				deviceParamsTextArea.setVisible(true);
				deviceInfoProgressBar.setVisible(false);
				break;
			case CANCELLING_JOB:
			case RESETTING:
				startButton.setEnabled(false);
				cancelButton.setEnabled(false);
				resetButton.setEnabled(false);
				jobProgressBar.setVisible(true);
				updateProgress(0, 0);
				devicesComboBox.setEnabled(false);
				deviceParamsTextArea.setVisible(true);
				deviceInfoProgressBar.setVisible(false);
				break;
		}
	}

	public enum MainScreenStates {
		CONNECTING_ADB, DEVICE_NOT_FOUND, REQUESTING_INFO, JOB_IN_PROGRESS, CANCELLING_JOB, RESETTING, READY
	}
}
