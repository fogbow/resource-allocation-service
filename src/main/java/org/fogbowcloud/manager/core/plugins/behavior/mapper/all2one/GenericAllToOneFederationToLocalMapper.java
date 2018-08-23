package org.fogbowcloud.manager.core.plugins.behavior.mapper.all2one;

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
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;

public class GenericAllToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
	
    private static final Logger LOGGER = Logger.getLogger(GenericAllToOneFederationToLocalMapper.class);
    
    private static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";

    private Map<String, String> credentials;

    private TokenGenerator tokenGenerator;
    private FederationIdentityPlugin federationIdentityPlugin;

	public GenericAllToOneFederationToLocalMapper(TokenGenerator tokenGenerator,
                                                  FederationIdentityPlugin federationIdentityPlugin,
                                                  String configurationFileName)
            throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + configurationFileName);
        this.credentials = getDefaultLocalTokenCredentials(properties);
        this.tokenGenerator = tokenGenerator;
        this.federationIdentityPlugin = federationIdentityPlugin;
    }

    @Override
    public Token map(FederationUserToken user) throws UnexpectedException, FogbowManagerException {
	    String tokenString = this.tokenGenerator.createTokenValue(this.credentials);
	    LOGGER.debug("token string: " + (tokenString == null ? "null" : tokenString));
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
}
