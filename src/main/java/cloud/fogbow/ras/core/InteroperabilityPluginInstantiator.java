package cloud.fogbow.ras.core;

import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

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
        String className = this.properties.getProperty(ConfigurationPropertyKeys.ATTACHMENT_PLUGIN_CLASS_KEY);
        return (AttachmentPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ComputePlugin getComputePlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.COMPUTE_PLUGIN_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.COMPUTE_QUOTA_PLUGIN_CLASS_KEY);
        return (ComputeQuotaPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public NetworkPlugin getNetworkPlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.NETWORK_PLUGIN_CLASS_KEY);
        return (NetworkPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public VolumePlugin getVolumePlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.VOLUME_PLUGIN_CLASS_KEY);
        return (VolumePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public ImagePlugin getImagePlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.IMAGE_PLUGIN_CLASS_KEY);
        return (ImagePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public PublicIpPlugin getPublicIpPlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.PUBLIC_IP_PLUGIN_CLASS_KEY);
        return (PublicIpPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public GenericRequestPlugin getGenericRequestPlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.GENERIC_PLUGIN_CLASS_KEY);
        return (GenericRequestPlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public SecurityRulePlugin getSecurityRulePlugin() {
        String className = this.properties.getProperty(ConfigurationPropertyKeys.SECURITY_RULE_PLUGIN_CLASS_KEY);
        return (SecurityRulePlugin) this.pluginFactory.createPluginInstance(className, this.cloudConfPath);
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.
                getProperty(ConfigurationPropertyKeys.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className, this.mapperConfPath);
    }

    /**
     * Used only for tests
     */
    protected Properties getProperties() {
        return this.properties;
    }
}
