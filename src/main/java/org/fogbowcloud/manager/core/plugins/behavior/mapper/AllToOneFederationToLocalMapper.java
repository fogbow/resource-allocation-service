package org.fogbowcloud.manager.core.plugins.behavior.mapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.TokenGenerator;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;

public class AllToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
	
    private static final Logger LOGGER = Logger.getLogger(AllToOneFederationToLocalMapper.class);
    
    private static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";
	private static final String DEFAULT_MAPPER_CONF = "default_mapper.conf";

	private static final String TOKEN_GENERATOR_CLASS_NAME_KEY = "local_token_generator_class";
	private static final String FEDERATION_IDENTITY_PLUGIN_CLASS_NAME_KEY = "federation_identity_plugin_class";
	
    private Map<String, String> credentials;

    private TokenGenerator tokenGenerator;
    private FederationIdentityPlugin federationIdentityPlugin;
	
	public AllToOneFederationToLocalMapper() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() +
                File.separator + DEFAULT_MAPPER_CONF);
        this.credentials = getDefaultLocalTokenCredentials(properties);
        this.tokenGenerator = getTokenGenerator(properties);
        this.federationIdentityPlugin = getFederationIdentityPlugin(properties);
    }

    @Override
    public Token map(FederationUserToken user) throws UnexpectedException, FogbowManagerException {
	    String tokenString = this.tokenGenerator.createTokenValue(this.credentials);
	    return this.federationIdentityPlugin.createToken(tokenString);
    }

    /**
     * Gets credentials with prefix in the properties (LOCAL_TOKEN_CREDENTIALS_PREFIX).
     *
     * @param properties
     * @return user credentials to generate local tokens
     * @throws FatalErrorException
     */
    private Map<String, String> getDefaultLocalTokenCredentials(Properties properties) throws FatalErrorException {
        Map<String, String> localDefaultTokenCredentials = new HashMap<String, String>();
        if (properties == null) {
            LOGGER.debug("Empty property map.");
            throw new FatalErrorException("Empty property map.");
        }

        for (Object keyProperties : properties.keySet()) {
            String keyPropertiesStr = keyProperties.toString();
            if (keyPropertiesStr.startsWith(LOCAL_TOKEN_CREDENTIALS_PREFIX)) {
                String value = properties.getProperty(keyPropertiesStr);
                String key = normalizeKeyProperties(keyPropertiesStr);

                localDefaultTokenCredentials.put(key, value);
            }
        }

        if (localDefaultTokenCredentials.isEmpty()) {
            LOGGER.debug("Credentials not found.");
            throw new FatalErrorException("Default localidentity tokens credentials not found.");
        } else {
            return localDefaultTokenCredentials;
        }
    }

    private String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
    }

    // ToDo: This method needs to get a property that defines which TokenGenerator should be used
    // For the time being, we set KeystoneV3 statically.
    private TokenGenerator getTokenGenerator(Properties properties) {
//        PluginFactory pluginFactory = new PluginFactory();
//        String className = properties.getProperty(TOKEN_GENERATOR_CLASS_NAME_KEY);
//        if (className  == null) {
//            throw new FatalErrorException("No FederationIdentityPlugin class speciefied.");
//        }
//        return (FederationIdentityPlugin) pluginFactory.createPluginInstance(className);
        return new KeystoneV3TokenGenerator();
    }

    // ToDo: This method needs to get a property that defines which FederationIdentityPlugin should be used
    // For the time being, we set KeystoneV3 statically.
    private FederationIdentityPlugin getFederationIdentityPlugin(Properties properties) {
//        PluginFactory pluginFactory = new PluginFactory();
//        String className = properties.getProperty(TOKEN_GENERATOR_CLASS_NAME_KEY);
//        if (className  == null) {
//            throw new FatalErrorException("No FederationIdentityPlugin class speciefied.");
//        }
//        return (FederationIdentityPlugin) pluginFactory.createPluginInstance(className);
        return new KeystoneV3IdentityPlugin();
    }
}
