package org.fogbowcloud.manager.core.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.PluginFactory;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.utils.PropertiesUtil;

public class InstantiationInitService {

    private PluginFactory pluginFactory;
    private Properties properties;
    private static InstantiationInitService instance;

    private static final Logger LOGGER = Logger.getLogger(InstantiationInitService.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    private InstantiationInitService() {
        this.properties = new Properties();
        this.pluginFactory = new PluginFactory();

        this.setUpProperties();
    }

    public static synchronized InstantiationInitService getInstance() {
        if (instance == null) {
            instance = new InstantiationInitService();
        }
        return instance;
    }

    private void setUpProperties() {
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(DefaultConfigurationConstants.MANAGER_CONF_FILE_FULL_PATH);
        configFilesNames.add(DefaultConfigurationConstants.INTERCOMPONENT_CONF_FILE_FULL_PATH);
        configFilesNames.add(DefaultConfigurationConstants.REVERSE_TUNNEL_CONF_FILE_FULL_PATH);

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

    public AttachmentPlugin getAttachmentPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.ATTACHMENT_PLUGIN_CLASS_KEY);
        return (AttachmentPlugin) this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public ComputePlugin getComputePlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.COMPUTE_PLUGIN_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.COMPUTE_QUOTA_PLUGIN_CLASS_KEY);
        return (ComputeQuotaPlugin)
                this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public LocalIdentityPlugin getLocalIdentityPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.LOCAL_IDENTITY_PLUGIN_CLASS_KEY);
        return (LocalIdentityPlugin)
                this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public NetworkPlugin getNetworkPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.NETWORK_PLUGIN_CLASS_KEY);
        return (NetworkPlugin) this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public VolumePlugin getVolumePlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.VOLUME_PLUGIN_CLASS_KEY);
        return (VolumePlugin) this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.AUTHORIZATION_PLUGIN_CLASS_KEY);
        return (AuthorizationPlugin)
                this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.FEDERATION_IDENTITY_PLUGIN_CLASS_KEY);
        return (FederationIdentityPlugin)
                this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public LocalUserCredentialsMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (LocalUserCredentialsMapperPlugin)
                this.pluginFactory.createPluginInstance(className, this.properties);
    }

    public String getPropertyValue(String propertyId) {
        return this.properties.getProperty(propertyId);
    }

    public Properties getProperties() {
        return this.properties;
    }
}
