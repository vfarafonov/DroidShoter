package com.weezlabs.tools.android.screenshoter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weezlabs.libs.screenshoter.model.Mode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by vfarafonov on 16.02.2016.
 */
public class PropertiesHelper {
	private final static String PROPERTIES_FILENAME = "config.properties";
	private static final String KEY_ADB_PATH = "KEY_ADB_PATH";
	private static final String PROPERTIES_DESCRIPTION = "ScreenShoter properties";
	private static final String KEY_EXCLUDES = "Devices excludes";

	private static volatile PropertiesHelper instance_;

	private Properties properties_;

	private PropertiesHelper() {
		File propsFile = new File(PROPERTIES_FILENAME);
		try {
			if (!propsFile.exists()) {
				propsFile.createNewFile();
			}
			properties_ = new Properties();
			loadProperties(properties_);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static PropertiesHelper getInstance() {
		if (instance_ == null) {
			synchronized (PropertiesHelper.class) {
				if (instance_ == null) {
					instance_ = new PropertiesHelper();
				}
			}
		}
		return instance_;
	}

	public void saveAdbPath(String adbPath) {
		if (adbPath != null) {
			properties_.setProperty(KEY_ADB_PATH, adbPath);
		} else {
			properties_.remove(KEY_ADB_PATH);
		}
		saveProperties(properties_);
	}

	private void saveProperties(Properties properties) {
		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(PROPERTIES_FILENAME);
			properties.store(fileOutputStream, PROPERTIES_DESCRIPTION);
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadProperties(Properties properties) {
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(PROPERTIES_FILENAME);
			properties.load(fileInputStream);
			fileInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<String, List<Mode>> saveDeviceExcludes(String deviceSerialNumber, List<Mode> modes) {
		Map<String, List<Mode>> profilesMap = loadDeviceExcludes();
		Gson gson = new Gson();
		profilesMap.put(deviceSerialNumber, modes);
		properties_.setProperty(KEY_EXCLUDES, gson.toJson(profilesMap));
		saveProperties(properties_);
		return profilesMap;
	}

	public Map<String, List<Mode>> loadDeviceExcludes() {
		String profilesJson = properties_.getProperty(KEY_EXCLUDES);
		Gson gson = new Gson();
		Map<String, List<Mode>> excludesMap;
		if (profilesJson != null) {
			excludesMap = gson.fromJson(profilesJson, new TypeToken<Map<String, List<Mode>>>() {
			}.getType());
		} else {
			excludesMap = new HashMap<>();
		}
		return excludesMap;
	}

	public String getAdbPath() {
		return properties_.getProperty(KEY_ADB_PATH);
	}
}
