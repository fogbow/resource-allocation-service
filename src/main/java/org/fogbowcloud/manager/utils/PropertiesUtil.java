package org.fogbowcloud.manager.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

public class PropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    public static Properties readProperties(String configFileName) {
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(configFileName);
        return readProperties(configFilesNames);
    }

    public static Properties readProperties(List<String> configFilesNames) {

        Properties properties = new Properties();

        for (String fileName : configFilesNames) {
            Properties mProperties = loadProperties(fileName);
            properties.putAll(mProperties);
        }

        return properties;
    }

    private static Properties loadProperties(String fileName) {

        Properties prop = new Properties();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(fileName);
            prop.load(fileInputStream);
        } catch (FileNotFoundException e) {
            LOGGER.fatal("No " + fileName + " file was found at resources", e);
            System.exit(EXIT_ERROR_CODE);
        } catch (IOException e) {
            LOGGER.fatal(e.getMessage(), e);
            System.exit(EXIT_ERROR_CODE);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                   LOGGER.error("Could not cloud file " + fileName, e);
                }
            }
        }

        return prop;

    }
}

