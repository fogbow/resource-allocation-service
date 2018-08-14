package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.util.PropertiesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesHolder {

    private Properties properties;

    private static PropertiesHolder instance;

    private PropertiesHolder() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        String path = homeDir.getPath() + File.separator;
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(path+DefaultConfigurationConstants.MANAGER_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.INTERCOMPONENT_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.REVERSE_TUNNEL_CONF_FILE_NAME);
        this.properties = PropertiesUtil.readProperties(configFilesNames);
    }

    public static synchronized PropertiesHolder getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new PropertiesHolder();
        }
        return instance;
    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public String getProperty(String propertyName, String defaultPropertyValue) {
        String propertyValue = this.properties.getProperty(propertyName, defaultPropertyValue);
        if (propertyValue.trim().isEmpty()) {
            propertyValue = defaultPropertyValue;
        }
        return propertyValue;
    }
    
    public Properties getProperties() {
        return this.properties;
    }
}
