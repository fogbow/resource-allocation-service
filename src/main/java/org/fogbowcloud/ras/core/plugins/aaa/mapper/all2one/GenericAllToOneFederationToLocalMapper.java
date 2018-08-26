package org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GenericAllToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private static final Logger LOGGER = Logger.getLogger(GenericAllToOneFederationToLocalMapper.class);

    private static final String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";
    private Map<String, String> credentials;
    private TokenGeneratorPlugin tokenGeneratorPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;

    public GenericAllToOneFederationToLocalMapper(TokenGeneratorPlugin tokenGeneratorPlugin,
                                                  FederationIdentityPlugin federationIdentityPlugin,
                                                  String configurationFileName) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + configurationFileName);
        this.credentials = getDefaultLocalTokenCredentials(properties);
        this.tokenGeneratorPlugin = tokenGeneratorPlugin;
        this.federationIdentityPlugin = federationIdentityPlugin;
    }

    @Override
    public Token map(FederationUserToken user) throws UnexpectedException, FogbowRasException {
        String tokenString = this.tokenGeneratorPlugin.createTokenValue(this.credentials);
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
            throw new FatalErrorException("Default localidentity tokens credentials not found.");
        } else {
            return localDefaultTokenCredentials;
        }
    }

    private String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
    }
}
