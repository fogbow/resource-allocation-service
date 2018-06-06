package org.fogbowcloud.manager.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;

public class PropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

	private static Properties properties = null;

	private PropertiesUtil() {
	    this.setUpProperties();
    }

    public static synchronized Properties getInstance() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public String getProperty(String propertyName, String defaultPropertyValue) {
        return properties.getProperty(propertyName, defaultPropertyValue);
    }

    private void setUpProperties() {
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(DefaultConfigurationConstants.CLOUD_CONF_FILE_FULL_PATH);
        configFilesNames.add(DefaultConfigurationConstants.BEHAVIOR_CONF_FILE_FULL_PATH);

        try {
            for (String fileName : configFilesNames) {
                Properties mProperties = new Properties();
                FileInputStream mInput = new FileInputStream(fileName);
                mProperties.load(mInput);
                this.properties.putAll(mProperties);
            }
        } catch (FileNotFoundException e) {
            String[] msgSplitted = e.getMessage().split(" ");
            String fileName = msgSplitted[0];

            String msg = "No " + fileName + " file was found at resources.";
            LOGGER.fatal(msg);
            System.exit(EXIT_ERROR_CODE);
        } catch (IOException e) {
            LOGGER.fatal(e.getMessage());
        }
    }
}

