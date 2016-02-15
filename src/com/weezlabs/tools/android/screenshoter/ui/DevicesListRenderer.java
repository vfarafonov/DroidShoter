package com.weezlabs.tools.android.screenshoter.ui;

import com.android.ddmlib.IDevice;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Created by vfarafonov on 15.02.2016.
 */
public class DevicesListRenderer extends JLabel implements ListCellRenderer<IDevice> {
	private boolean isAdbConnected_ = false;

	public DevicesListRenderer() {
		setOpaque(true);
		setHorizontalAlignment(CENTER);
		setVerticalAlignment(CENTER);
	}

	public void setAdbConnected_(boolean isAdbConnected) {
		this.isAdbConnected_ = isAdbConnected;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends IDevice> list, IDevice value, int index, boolean isSelected, boolean cellHasFocus) {
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		if (value != null) {
			String deviceName;
			if (value.isEmulator()) {
				deviceName = value.getAvdName();
			} else {
				deviceName = value.getProperty(IDevice.PROP_DEVICE_MANUFACTURER) + " " + value.getProperty(IDevice.PROP_DEVICE_MODEL);
			}
			setText(deviceName + ": " + value.getSerialNumber());
		} else {
			setText(isAdbConnected_ ? "No devices found" : "Connecting to adb...");
		}

		return this;
	}
}
