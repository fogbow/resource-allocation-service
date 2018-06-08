package org.fogbowcloud.manager.core.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.plugins.PluginFactory;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.image.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.utils.PropertiesUtil;

public class PluginInstantiationService {

    private PluginFactory pluginFactory;
    private Properties properties;
    private static PluginInstantiationService instance;

    private static final Logger LOGGER = Logger.getLogger(PluginInstantiationService.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    private PluginInstantiationService() {
        HomeDir homeDir = HomeDir.getInstance();
        String path = homeDir.getPath() + File.separator;
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(path+DefaultConfigurationConstants.CLOUD_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.BEHAVIOR_CONF_FILE_NAME);
        this.properties = PropertiesUtil.readProperties(configFilesNames);
        this.pluginFactory = new PluginFactory();
    }

    public static synchronized PluginInstantiationService getInstance() {
        if (instance == null) {
            instance = new PluginInstantiationService();
        }
        return instance;
    }

    public AttachmentPlugin getAttachmentPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.ATTACHMENT_PLUGIN_CLASS_KEY);
        return (AttachmentPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public ComputePlugin getComputePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.COMPUTE_PLUGIN_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className);
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.COMPUTE_QUOTA_PLUGIN_CLASS_KEY);
        return (ComputeQuotaPlugin)
                this.pluginFactory.createPluginInstance(className);
    }

    public LocalIdentityPlugin getLocalIdentityPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.LOCAL_IDENTITY_PLUGIN_CLASS_KEY);
        return (LocalIdentityPlugin)
                this.pluginFactory.createPluginInstance(className);
    }

    public NetworkPlugin getNetworkPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.NETWORK_PLUGIN_CLASS_KEY);
        return (NetworkPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public VolumePlugin getVolumePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.VOLUME_PLUGIN_CLASS_KEY);
        return (VolumePlugin) this.pluginFactory.createPluginInstance(className);
    }

    public ImagePlugin getImagePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.IMAGE_PLUGIN_CLASS_KEY);
        return (ImagePlugin) this.pluginFactory.createPluginInstance(className);
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.AUTHORIZATION_PLUGIN_CLASS_KEY);
        return (AuthorizationPlugin)
                this.pluginFactory.createPluginInstance(className);
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.FEDERATION_IDENTITY_PLUGIN_CLASS_KEY);
        return (FederationIdentityPlugin)
                this.pluginFactory.createPluginInstance(className);
    }

    public LocalUserCredentialsMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.getProperty(
                ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (LocalUserCredentialsMapperPlugin)
                this.pluginFactory.createPluginInstance(className);
    }

    // Used only for tests
    protected Properties getProperties() {
        return this.properties;
    }
}
