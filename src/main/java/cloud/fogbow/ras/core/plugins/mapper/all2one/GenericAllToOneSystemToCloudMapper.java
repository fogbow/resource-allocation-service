package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class GenericAllToOneSystemToCloudMapper<T extends CloudUser, S extends SystemUser> implements SystemToCloudMapperPlugin<T, S> {
    private static final Logger LOGGER = Logger.getLogger(GenericAllToOneSystemToCloudMapper.class);

    private static final String CLOUD_USER_CREDENTIALS_PREFIX = "cloud_user_credentials_";
    private String idpUrl;
    private Map<String, String> credentials;

    public GenericAllToOneSystemToCloudMapper(String mapperConfFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(mapperConfFilePath);
        this.idpUrl = properties.getProperty(ConfigurationPropertyKeys.CLOUD_IDENTITY_PROVIDER_URL_KEY);
        this.credentials = getCloudUserCredentials(properties);
    }

    public abstract T getCloudUser(Map<String, String> credentials) throws FogbowException;

    @Override
    public T map(S systemUser) throws FogbowException {
        return getCloudUser(this.credentials);
    }

    public String getIdpUrl() {
        return idpUrl;
    }

    /**
     * Gets credentials with prefix in the properties (CLOUD_USER_CREDENTIALS_PREFIX).
     *
     * @param properties
     * @return a map containing the cloud user credentials used to authenticate it on the cloud identity provider
     * @throws FatalErrorException
     */
    private Map<String, String> getCloudUserCredentials(Properties properties) throws FatalErrorException {
        Map<String, String> localTokenCredentials = new HashMap<String, String>();
        if (properties == null) {
            throw new FatalErrorException(Messages.Exception.EMPTY_PROPERTY_MAP);
        }

        for (Object keyProperties : properties.keySet()) {
            String keyPropertiesStr = keyProperties.toString();
            if (keyPropertiesStr.startsWith(CLOUD_USER_CREDENTIALS_PREFIX)) {
                String value = properties.getProperty(keyPropertiesStr);
                String key = normalizeKeyProperties(keyPropertiesStr);

                localTokenCredentials.put(key, value);
            }
        }

        if (localTokenCredentials.isEmpty()) {
            throw new FatalErrorException(Messages.Exception.DEFAULT_CREDENTIALS_NOT_FOUND);
        } else {
            return localTokenCredentials;
        }
    }

    private String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(CLOUD_USER_CREDENTIALS_PREFIX, "");
    }
}
