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
    private PluginFactory pluginFactory;
    private Properties properties;
    private static AaaPluginInstantiator instance;

    /**
     * THERE ARE ONLY TWO CALLEES ALLOWED TO CALL THIS METHOD:
     * i) test code; and
     * ii) this.getInstance method.
     */
    protected AaaPluginInstantiator() {
        String aaaConfFilePath = HomeDir.getPath() + SystemConstants.AAA_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(aaaConfFilePath);
        this.pluginFactory = new PluginFactory();
    }

    public static synchronized AaaPluginInstantiator getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new AaaPluginInstantiator();
        }
        return instance;
    }

    public TokenGeneratorPlugin getTokenGeneratorPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.TOKEN_GENERATOR_PLUGIN_CLASS);
        String pluginConfFile = this.properties.getProperty(ConfigurationConstants.TOKEN_GENERATOR_CONF_FILE);
        return new TokenGeneratorPluginProtectionWrapper((TokenGeneratorPlugin)
                this.pluginFactory.createPluginInstance(className, (HomeDir.getPath() + pluginConfFile)));
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
        if (className.equals(org.fogbowcloud.ras.core.plugins.aaa.authorization.
                ComposedAuthorizationPlugin.class.getName())) {
            String filename = this.properties.
                    getProperty(ConfigurationConstants.COMPOSED_AUTHORIZATION_PLUGIN_CONF_FILE);
            ComposedAuthorizationPlugin composedAuthorizationPlugin =
                    new ComposedAuthorizationPlugin(HomeDir.getPath() + filename);
            return composedAuthorizationPlugin;
        } else {
            return (AuthorizationPlugin) this.pluginFactory.createPluginInstance(className);
        }
    }

    public GenericRequestPlugin getGenericPlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.GENERIC_PLUGIN_CLASS_KEY);
        return (GenericRequestPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public FederationToLocalMapperPlugin getLocalUserCredentialsMapperPlugin() {
        String className = this.properties.
                getProperty(ConfigurationConstants.LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY);
        return (FederationToLocalMapperPlugin) this.pluginFactory.createPluginInstance(className);
    }

    public SecurityRulePlugin getSecurityRulePlugin() {
        String className = this.properties.getProperty(ConfigurationConstants.SECURITY_RULE_PLUGIN_CLASS_KEY);
        return (SecurityRulePlugin) this.pluginFactory.createPluginInstance(className);
    }

    /**
     * Used only for tests
     */
    protected Properties getProperties() {
        return this.properties;
    }
}
