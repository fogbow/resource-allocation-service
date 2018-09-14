package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.interoperability.*;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
        String path = HomeDir.getPath();
        List<String> configFilesNames = new ArrayList<>();
        configFilesNames.add(path + DefaultConfigurationConstants.INTEROPERABILITY_CONF_FILE_NAME);
        configFilesNames.add(path + DefaultConfigurationConstants.AAA_CONF_FILE_NAME);
        this.properties = PropertiesUtil.readProperties(configFilesNames);
        this.pluginFactory = new PluginFactory();
    }

    public static synchronized PluginInstantiator getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new PluginInstantiator();
        }
        return instance;
    }

    public PublicIpPlugin getPublicIpPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.PUBLIC_IP_PLUGIN_CLASS_KEY);
        return (PublicIpPlugin) this.pluginFactory.createPluginInstance(className);
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

    public TokenGeneratorPlugin getTokenGeneratorPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.TOKEN_GENERATOR_PLUGIN_CLASS);
        return new TokenGeneratorPluginProtectionWrapper((TokenGeneratorPlugin)
                this.pluginFactory.createPluginInstance(className));
    }

    public FederationIdentityPlugin getFederationIdentityPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.FEDERATION_IDENTITY_PLUGIN_CLASS_KEY);
        return new FederationIdentityPluginProtectionWrapper((FederationIdentityPlugin)
                this.pluginFactory.createPluginInstance(className));
    }

    public AuthenticationPlugin getAuthenticationPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.AUTHENTICATION_PLUGIN_CLASS_KEY);
        return (AuthenticationPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public AuthorizationPlugin getAuthorizationPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.AUTHORIZATION_PLUGIN_CLASS_KEY);
        return (AuthorizationPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className);
    }

    /**
     * Used only for tests
     */
    protected Properties getProperties() {
        return this.properties;
    }
}
