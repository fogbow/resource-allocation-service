package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.PluginFactory;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

@Service("instantiationInitService")
@PropertySource({
        "classpath:manager.properties",
        "classpath:federation.properties",
        "classpath:infrastructure.properties",
        "classpath:test.properties"
})
public class InstantiationInitService {


    @Autowired
    private Environment env;

    private PluginFactory pluginFactory;
    private Properties properties;

    private static final Logger LOGGER = Logger.getLogger(InstantiationInitService.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    public InstantiationInitService() {
        this.properties = new Properties();
        this.pluginFactory = new PluginFactory();

        this.setUpProperties();
    }

    private void setUpProperties() {

        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(ConfigurationConstants.MANAGER_CONF_FILE_FULL_PATH);
        configFilesNames.add(ConfigurationConstants.FEDERATION_CONF_FILE_FULL_PATH);
        configFilesNames.add(ConfigurationConstants.INFRA_CONF_FILE_FULL_PATH);

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
            LOGGER.severe(msg);
            System.exit(EXIT_ERROR_CODE);

        } catch (Exception e) {
            // TODO: do something
        }
    }

    public ComputePlugin getComputePlugin() {
        String className = this.getPropertyValue(ConfigurationConstants.COMPUTE_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public IdentityPlugin getLocalIdentityPlugin() {
        return this.getIdentityPlugin(ConfigurationConstants.LOCAL_PREFIX);
    }

    public IdentityPlugin getFederationIdentityPlugin() {
        return this.getIdentityPlugin(ConfigurationConstants.FEDERATION_PREFIX);
    }

    private IdentityPlugin getIdentityPlugin(String prefix) {
        String className =  this.getPropertyValue(prefix + ConfigurationConstants.IDENTITY_CLASS_KEY);
        return (IdentityPlugin) this.pluginFactory.getIdentityPluginByPrefix(prefix, className, this.properties);
    }

    private String getPropertyValue(String propertyId) {
        return env.getProperty(propertyId);
    }

    public Properties getProperties() {
        return this.properties;
    }

    // TODO: getter for AuthorizationPlugin
    // TODO: getter for MapperPlugin
    // TODO: getter for NetworkPlugin
    // TODO: getter for StoragePlugin
    // TODO: criar exceptions para a pessoa configurar algo do tipo errado
}
