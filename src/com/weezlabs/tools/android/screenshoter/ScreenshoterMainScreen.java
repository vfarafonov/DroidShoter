package com.weezlabs.tools.android.screenshoter;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.sun.javafx.beans.annotations.NonNull;
import com.weezlabs.libs.screenshoter.ScreenShooterManager;
import com.weezlabs.libs.screenshoter.adb.DeviceShellHelper;
import com.weezlabs.libs.screenshoter.model.Device;
import com.weezlabs.libs.screenshoter.model.Mode;
import com.weezlabs.tools.android.screenshoter.ui.DevicesListRenderer;
import com.weezlabs.tools.android.screenshoter.ui.ModesTableModel;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 * Created by vfarafonov on 12.02.2016.
 */
public class ScreenshoterMainScreen {
	public static final int MAX_DEVICES_ROW_COUNT = 10;
	private static final java.lang.String HELP_LINK = "https://github.com/vfarafonov/DroidShoter";
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
	private JLabel helpLabel;
	private MainScreenStates currentState_;
	private JPanel coverFrame_;
	private Map<String, List<Mode>> excludedModesMap_;

	public ScreenshoterMainScreen() {
		// TODO: add devices listener
		setState(MainScreenStates.CONNECTING_ADB);
		String adbPath = getAdbLocationFromManagerAndProperties();
		if (adbPath != null) {
			instantiateScreenshotManager(adbPath);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					displayAdbPicker();
				}
			});
		}

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
					List<Mode> excludedModes = modesTableModel_.getExcludedModes();
					excludedModesMap_ = PropertiesHelper.getInstance().saveDeviceExcludes(
							((IDevice) devicesComboBox.getSelectedItem()).getSerialNumber(),
							excludedModes
					);
					screenShooterManager_.createScreenshotsForAllResolutionsAsync(
							new File(directoryTextField.getText()),
							prefixTextField.getText(),
							Integer.valueOf(sleepTextField.getText()),
							excludedModes,
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
		helpLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
				if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
					try {
						desktop.browse(new URI(HELP_LINK));
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		});
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("DroidShoter");
		frame.setIconImages(getIconsList(frame.getClass()));
		frame.setContentPane(new ScreenshoterMainScreen().ScreenshoterRootPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static ArrayList<Image> getIconsList(Class<?> aClass) {
		ArrayList<Image> iconsList = new ArrayList<>();
		try {
			iconsList.add(ImageIO.read(aClass.getResource("/res/images/icon_128.png")));
			iconsList.add(ImageIO.read(aClass.getResource("/res/images/icon_64.png")));
			iconsList.add(ImageIO.read(aClass.getResource("/res/images/icon_32.png")));
			iconsList.add(ImageIO.read(aClass.getResource("/res/images/icon_16.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return iconsList;
	}

	/**
	 * Tries to find adb in system environment (ANDROID_HOME) or if it was specified with last launch
	 */
	private String getAdbLocationFromManagerAndProperties() {
		String path = ScreenShooterManager.getSystemAdbLocation();
		if (path != null && ScreenShooterManager.checkForAdbInPath(path)) {
			return path;
		} else {
			path = PropertiesHelper.getInstance().getAdbPath();
			if (path != null) {
				if (ScreenShooterManager.checkForAdbInPath(path)) {
					return path;
				} else {
					PropertiesHelper.getInstance().saveAdbPath(null);
				}
			}
		}
		return null;
	}

	/**
	 * Displays file picker dialog. Shows layout with button to find adb if cancelled
	 */
	private void displayAdbPicker() {
		JFileChooser adbFileChooser = new JFileChooser();
		adbFileChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				String nameWithoutExtension = f.getName();
				int lastDotIndex = nameWithoutExtension.lastIndexOf(".");
				if (lastDotIndex != -1) {
					nameWithoutExtension = nameWithoutExtension.substring(0, lastDotIndex);
				}
				return nameWithoutExtension.equalsIgnoreCase("adb");
			}

			@Override
			public String getDescription() {
				return "Adb executable";
			}
		});
		int returnValue = adbFileChooser.showDialog(ScreenshoterRootPanel, "Pick ADB location");
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			System.out.println("Selected: " + adbFileChooser.getSelectedFile().getAbsolutePath());
			if (coverFrame_ != null) {
				ScreenshoterRootPanel.setVisible(true);
				ScreenshoterRootPanel.getParent().remove(coverFrame_);
				coverFrame_ = null;
			}
			String adbPath = adbFileChooser.getSelectedFile().getAbsolutePath();
			if (ScreenShooterManager.checkForAdbInPath(adbPath)) {
				PropertiesHelper.getInstance().saveAdbPath(adbPath);
				instantiateScreenshotManager(adbPath);
			} else {
				displayAdbPicker();
			}
		} else {
			if (coverFrame_ == null) {
				coverFrame_ = new JPanel();
				coverFrame_.setSize(ScreenshoterRootPanel.getSize());
				coverFrame_.setBackground(ScreenshoterRootPanel.getBackground());
				JButton button = new JButton("Adb not found. Click me to find it");
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						displayAdbPicker();
					}
				});
				coverFrame_.add(button);
				coverFrame_.setVisible(true);
				ScreenshoterRootPanel.getParent().add(coverFrame_);
				ScreenshoterRootPanel.setVisible(false);
			}
		}
	}

	private void instantiateScreenshotManager(String adbPath) {
		ScreenShooterManager.getInstanceAsync(adbPath,
				new ScreenShooterManager.ManagerInitListener() {
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
				}
		);
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

	private void getDeviceInfo(@NonNull final IDevice iDevice) {
		ScreenShooterManager.getDeviceDisplayInfoAsync(iDevice, new DeviceShellHelper.DeviceInfoListener() {
			@Override
			public void onDeviceInfoUpdated(Device device) {
				screenShooterManager_.setDevice(device);
				deviceParamsTextArea.setText(device.getPhysicalResolution().toString() + ", " + device.getPhysicalDpi().toString());
				modesTableModel_.clearRows();

				if (excludedModesMap_ == null) {
					excludedModesMap_ = PropertiesHelper.getInstance().loadDeviceExcludes();
				}
				List<Mode> excludedModes = excludedModesMap_.get(iDevice.getSerialNumber());
				List<Mode> modesQueue = Mode.getModesQueue(device);
				if (excludedModes != null) {
					for (Mode excludedMode : excludedModes) {
						if (modesQueue.contains(excludedMode)) {
							modesQueue.get(modesQueue.indexOf(excludedMode)).setIsActivated(false);
						}
					}
				}

				modesTableModel_.addAll(modesQueue);
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
