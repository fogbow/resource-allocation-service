package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class GenericAllToOneFederationToLocalMapper implements FederationToLocalMapperPlugin {
    private static final Logger LOGGER = Logger.getLogger(GenericAllToOneFederationToLocalMapper.class);

    private static final String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";
    private Map<String, String> credentials;
    private String tokenGeneratorUrl;

    public GenericAllToOneFederationToLocalMapper(String mapperConfFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(mapperConfFilePath);
        this.credentials = getLocalTokenCredentials(properties);
        this.tokenGeneratorUrl = properties.getProperty(ConfigurationPropertyKeys.TOKEN_GENERATOR_URL_KEY);
    }

    @Override
    public CloudToken map(FederationUser federationUser) throws FogbowException {
        return createTokenValue(this.credentials);
    }

    public abstract FederationUser getFederationUser(Map<String, String> credentials) throws FogbowException;

    public abstract CloudToken decorateToken(CloudToken token, FederationUser federationUser) throws UnexpectedException;

    public CloudToken createTokenValue(Map<String, String> credentials) throws FogbowException {
        FederationUser federationUser = getFederationUser(credentials);
        CloudToken token = new CloudToken(federationUser);
        return decorateToken(token, federationUser);
    }

    /**
     * Gets credentials with prefix in the properties (LOCAL_TOKEN_CREDENTIALS_PREFIX).
     *
     * @param properties
     * @return user credentials to generate local tokens
     * @throws FatalErrorException
     */
    private Map<String, String> getLocalTokenCredentials(Properties properties) throws FatalErrorException {
        Map<String, String> localTokenCredentials = new HashMap<String, String>();
        if (properties == null) {
            throw new FatalErrorException(Messages.Fatal.EMPTY_PROPERTY_MAP);
        }

        for (Object keyProperties : properties.keySet()) {
            String keyPropertiesStr = keyProperties.toString();
            if (keyPropertiesStr.startsWith(LOCAL_TOKEN_CREDENTIALS_PREFIX)) {
                String value = properties.getProperty(keyPropertiesStr);
                String key = normalizeKeyProperties(keyPropertiesStr);

                localTokenCredentials.put(key, value);
            }
        }

        if (localTokenCredentials.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.DEFAULT_CREDENTIALS_NOT_FOUND);
        } else {
            return localTokenCredentials;
        }
    }

    private String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
    }

    public String getTokenGeneratorUrl() {
        return tokenGeneratorUrl;
    }
}
