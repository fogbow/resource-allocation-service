package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.*;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.io.File;
import java.util.Properties;

public class InteroperabilityPluginInstantiator {
    private Properties properties;
    private String cloudConfPath;
    private String mapperConfPath;
    private PluginFactory pluginFactory;

    public InteroperabilityPluginInstantiator(String cloudName) {
        String path = HomeDir.getPath();
        this.properties = PropertiesUtil.readProperties(path +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName + File.separator +
                SystemConstants.INTEROPERABILITY_CONF_FILE_NAME);
        this.cloudConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName +
                File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName +
                File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;
        this.pluginFactory = new PluginFactory();
    }

    public AttachmentPlugin getAttachmentPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.ATTACHMENT_PLUGIN_CLASS_KEY);
        return (AttachmentPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ComputePlugin getComputePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.COMPUTE_PLUGIN_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.COMPUTE_QUOTA_PLUGIN_CLASS_KEY);
        return (ComputeQuotaPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public NetworkPlugin getNetworkPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.NETWORK_PLUGIN_CLASS_KEY);
        return (NetworkPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public VolumePlugin getVolumePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.VOLUME_PLUGIN_CLASS_KEY);
        return (VolumePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ImagePlugin getImagePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.IMAGE_PLUGIN_CLASS_KEY);
        return (ImagePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public PublicIpPlugin getPublicIpPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.PUBLIC_IP_PLUGIN_CLASS_KEY);
        return (PublicIpPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.
                getProperty(ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath,
                this.mapperConfPath);
    }

    /**
     * Used only for tests
     */
    protected Properties getProperties() {
        return this.properties;
    }
}
