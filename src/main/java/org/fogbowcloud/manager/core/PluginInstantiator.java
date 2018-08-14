package org.fogbowcloud.manager.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;

public class PluginInstantiator {

    private PluginFactory pluginFactory;
    private Properties properties;
    private static PluginInstantiator instance;

    /**
     * THERE ARE ONLY TWO CALLEES ALLOWED TO CALL THIS METHOD: 
     * i) test code; and 
     * ii) this.getInstance method.
     */
    protected PluginInstantiator() {
        HomeDir homeDir = HomeDir.getInstance();
        String path = homeDir.getPath() + File.separator;
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(path+DefaultConfigurationConstants.CLOUD_CONF_FILE_NAME);
        configFilesNames.add(path+DefaultConfigurationConstants.BEHAVIOR_CONF_FILE_NAME);
        this.properties = PropertiesUtil.readProperties(configFilesNames);
        this.pluginFactory = new PluginFactory();
    }

    public static synchronized PluginInstantiator getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new PluginInstantiator();
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
        return (ComputeQuotaPlugin) this.pluginFactory.createPluginInstance(className);
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
        return (AuthorizationPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public AuthenticationPlugin getAuthenticationPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.AUTHENTICATION_PLUGIN_CLASS_KEY);
        return (AuthenticationPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.FEDERATION_IDENTITY_PLUGIN_CLASS_KEY);
        return (FederationIdentityPlugin) this.pluginFactory.createPluginInstance(className);
    }

    /** Used only for tests */
    protected Properties getProperties() {
        return this.properties;
    }
    
}
