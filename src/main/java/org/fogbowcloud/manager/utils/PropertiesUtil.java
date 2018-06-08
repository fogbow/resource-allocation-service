package org.fogbowcloud.manager.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;

public class PropertiesUtil {

    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

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

