package com.weezlabs.tools;

import com.android.ddmlib.IDevice;
import com.weezlabs.libs.screenshoter.*;
import com.weezlabs.libs.screenshoter.Model.Device;

/**
 * Created by vfarafonov on 12.02.2016.
 */
public class ScreenshoterJar {
	public static void main(String[] args){
		final ScreenShooterManager screenShooterManager = ScreenShooterManager.getInstance();
		IDevice[] devices = screenShooterManager.getDevices();
		if (devices.length == 0) {
			System.out.println("No device connected");
			return;
		}
		ScreenShooterManager.getDeviceDisplayInfo(devices[0], new ScreenShooterManager.DeviceInfoListener() {
			@Override
			public void onDeviceInfoUpdated(Device device) {
				System.out.println("Success. Density: " + device.getPhysicalDpi().getDensity() + " Resolution: " + device.getPhysicalResolution());
				screenShooterManager.setDevice(device);
				screenShooterManager.createScreenshotsForAllResolutions(null,
						null,
						null,
						new ScreenShooterManager.ScreenShotJobProgressListener() {
							@Override
							public void onScreenshotJobFinished() {
								System.out.println("IT WORKS!!!!");
								screenShooterManager.resetDeviceDisplay(new ScreenShooterManager.CommandStatusListener() {
									@Override
									public void onCommandSentToDevice() {
										System.out.println("Display params were reset");
									}

									@Override
									public void onCommandExecutionFailed() {
										System.out.println("Display params reset failed");
									}
								});
							}

							@Override
							public void onScreenshotJobFailed() {
								System.out.println("Screenshot job failed");
							}

							@Override
							public void onScreenshotJobCancelled() {

							}
						}
				);
			}

			@Override
			public void onDeviceUpdateFailed(IDevice iDevice, Exception e) {
				System.out.println("Device update failed: " + e.getMessage());
			}
		});
	}
}
