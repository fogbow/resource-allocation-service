package org.fogbowcloud.ras.util;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesUtil {
    private static final Logger LOGGER = Logger.getLogger(PropertiesUtil.class.getName());

    public static Properties readProperties(String configFileName) throws FatalErrorException {
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(configFileName);
        return readProperties(configFilesNames);
    }

    public static Properties readProperties(List<String> configFilesNames) throws FatalErrorException {
        Properties properties = new Properties();

        for (String fileName : configFilesNames) {
            Properties mProperties = loadProperties(fileName);
            properties.putAll(mProperties);
        }

        return properties;
    }

    private static Properties loadProperties(String fileName) throws FatalErrorException {
        Properties prop = new Properties();
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(fileName);
            prop.load(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.PROPERTY_FILE_NOT_FOUND, fileName), e);
        } catch (IOException e) {
            throw new FatalErrorException(e.getMessage(), e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LOGGER.error(String.format(Messages.Error.UNABLE_TO_CLOSE_FILE, fileName), e);
                }
            }
        }
        return prop;
    }
}

