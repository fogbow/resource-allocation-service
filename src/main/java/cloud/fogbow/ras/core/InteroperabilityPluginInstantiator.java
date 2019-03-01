package cloud.fogbow.ras.core;

import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PluginFactory;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class InteroperabilityPluginInstantiator {
    private PluginFactory pluginFactory;
    private Map<String, Properties> cloudPropertiesCache;

    public InteroperabilityPluginInstantiator() {
        this.pluginFactory = new PluginFactory();
        this.cloudPropertiesCache = new HashMap<>();
    }

    private String getCloudProperty(String cloudName, String propertyKey) {
        if (!cloudPropertiesCache.containsKey(cloudName)) {
            Properties cloudProperties = readCloudProperties(cloudName);
            cloudPropertiesCache.put(cloudName, cloudProperties);
        }
        return cloudPropertiesCache.get(cloudName).getProperty(propertyKey);
    }

    private String getCloudConfPath(String cloudName) {
        String path = HomeDir.getPath();
        return path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName +
                File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
    }

    private Properties readCloudProperties(String cloudName) {
        String path = HomeDir.getPath();
        return PropertiesUtil.readProperties(path +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName + File.separator +
                SystemConstants.INTEROPERABILITY_CONF_FILE_NAME);
    }

    public AttachmentPlugin getAttachmentPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.ATTACHMENT_PLUGIN_CLASS_KEY);
        return (AttachmentPlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public ComputePlugin getComputePlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.COMPUTE_PLUGIN_CLASS_KEY);
        return (ComputePlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public ComputeQuotaPlugin getComputeQuotaPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.COMPUTE_QUOTA_PLUGIN_CLASS_KEY);
        return (ComputeQuotaPlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public NetworkPlugin getNetworkPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.NETWORK_PLUGIN_CLASS_KEY);
        return (NetworkPlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public VolumePlugin getVolumePlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.VOLUME_PLUGIN_CLASS_KEY);
        return (VolumePlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public ImagePlugin getImagePlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.IMAGE_PLUGIN_CLASS_KEY);
        return (ImagePlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public PublicIpPlugin getPublicIpPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.PUBLIC_IP_PLUGIN_CLASS_KEY);
        return (PublicIpPlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public GenericRequestPlugin getGenericRequestPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.GENERIC_PLUGIN_CLASS_KEY);
        // This plugin does not require a configuration file, since all required information to interact with the
        // cloud will come in the request itself.
        return (GenericRequestPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public SecurityRulePlugin getSecurityRulePlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.SECURITY_RULE_PLUGIN_CLASS_KEY);
        return (SecurityRulePlugin) this.pluginFactory.createPluginInstance(className, getCloudConfPath(cloudName));
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin(String cloudName) {
        String className = getCloudProperty(cloudName, ConfigurationPropertyKeys.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);

        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator + cloudName +
                File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className, mapperConfPath);
    }
}
