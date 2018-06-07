package org.fogbowcloud.manager.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;

public class PropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

	private static Properties properties = null;

	private PropertiesUtil() {
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(DefaultConfigurationConstants.MANAGER_CONF_FILE_FULL_PATH);
        configFilesNames.add(DefaultConfigurationConstants.INTERCOMPONENT_CONF_FILE_FULL_PATH);
        configFilesNames.add(DefaultConfigurationConstants.REVERSE_TUNNEL_CONF_FILE_FULL_PATH);
        this.properties = readProperties(configFilesNames);
    }

    public static synchronized Properties getInstance() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

//    public String getProperty(String propertyName) {
//        return properties.getProperty(propertyName);
//    }
//
//    public String getProperty(String propertyName, String defaultPropertyValue) {
//        return properties.getProperty(propertyName, defaultPropertyValue);
//    }

    public static Properties readProperties(List<String> configFilesNames) {

        Properties properties = new Properties();

        try {
            for (String fileName : configFilesNames) {
                Properties mProperties = new Properties();
                FileInputStream mInput = new FileInputStream(fileName);
                mProperties.load(mInput);
                properties.putAll(mProperties);
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

        return properties;
    }
}

