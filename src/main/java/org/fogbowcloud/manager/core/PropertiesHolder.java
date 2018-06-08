package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.fogbowcloud.manager.utils.PropertiesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesHolder {

    private Properties properties;

    private static PropertiesHolder instance;

    private static final Logger LOGGER = Logger.getLogger(PropertiesHolder.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    private PropertiesHolder() {
        HomeDir homeDir = HomeDir.getInstance();
        String path = homeDir.getPath() + File.separator;
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(path+DefaultConfigurationConstants.MANAGER_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.INTERCOMPONENT_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.REVERSE_TUNNEL_CONF_FILE_NAME);
        this.properties = PropertiesUtil.readProperties(configFilesNames);    }

    public static synchronized PropertiesHolder getInstance() {
        if (instance == null) {
            instance = new PropertiesHolder();
        }
        return instance;
    }

    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public String getProperty(String propertyName, String defaultPropertyValue) {
        return properties.getProperty(propertyName, defaultPropertyValue);
    }
    
    public Properties getProperties() {
        return this.properties;
    }
}
