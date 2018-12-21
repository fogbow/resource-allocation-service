package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.ComposedAuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper;
import org.fogbowcloud.ras.core.plugins.interoperability.*;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestPlugin;
import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.Properties;

public class AaaPluginInstantiator {
    private static PluginFactory pluginFactory = new PluginFactory();

    public static TokenGeneratorPlugin getTokenGeneratorPlugin(String aaaConfFilePath) {
        Properties properties = PropertiesUtil.readProperties(aaaConfFilePath);
        String className = properties.getProperty(ConfigurationConstants.TOKEN_GENERATOR_PLUGIN_CLASS);
        String pluginConfFile = properties.getProperty(ConfigurationConstants.TOKEN_GENERATOR_CONF_FILE);
        return new TokenGeneratorPluginProtectionWrapper((TokenGeneratorPlugin)
                AaaPluginInstantiator.pluginFactory.createPluginInstance(className, (HomeDir.getPath() + pluginConfFile)));
    }

    public static FederationIdentityPlugin getFederationIdentityPlugin(String aaaConfFilePath) {
        Properties properties = PropertiesUtil.readProperties(aaaConfFilePath);
        String className = properties.getProperty(ConfigurationConstants.FEDERATION_IDENTITY_PLUGIN_CLASS_KEY);
        return new FederationIdentityPluginProtectionWrapper((FederationIdentityPlugin)
                AaaPluginInstantiator.pluginFactory.createPluginInstance(className));
    }

    public static AuthenticationPlugin getAuthenticationPlugin(String aaaConfFilePath) {
        Properties properties = PropertiesUtil.readProperties(aaaConfFilePath);
        String className = properties.getProperty(ConfigurationConstants.AUTHENTICATION_PLUGIN_CLASS_KEY);
        return (AuthenticationPlugin) AaaPluginInstantiator.pluginFactory.createPluginInstance(className);
    }

    public static AuthorizationPlugin getAuthorizationPlugin(String aaaConfFilePath) {
        Properties properties = PropertiesUtil.readProperties(aaaConfFilePath);
        String className = properties.getProperty(ConfigurationConstants.AUTHORIZATION_PLUGIN_CLASS_KEY);
        if (className.equals(org.fogbowcloud.ras.core.plugins.aaa.authorization.
                ComposedAuthorizationPlugin.class.getName())) {
            String filename = properties.getProperty(ConfigurationConstants.COMPOSED_AUTHORIZATION_PLUGIN_CONF_FILE);
            ComposedAuthorizationPlugin composedAuthorizationPlugin =
                    new ComposedAuthorizationPlugin(HomeDir.getPath() + filename);
            return composedAuthorizationPlugin;
        } else {
            return (AuthorizationPlugin) AaaPluginInstantiator.pluginFactory.createPluginInstance(className);
        }
    }
}
