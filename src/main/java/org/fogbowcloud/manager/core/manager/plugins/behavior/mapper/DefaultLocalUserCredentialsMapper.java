package org.fogbowcloud.manager.core.manager.plugins.behavior.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class DefaultLocalUserCredentialsMapper implements LocalUserCredentialsMapperPlugin {
	
    private static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";
	private static final String DEFAULT_MAPPER_CONF = "default_mapper.conf";	
	private Properties properties;
	
	public DefaultLocalUserCredentialsMapper() throws IOException {
        this.properties = new Properties();
        FileInputStream fileInputStream;
		fileInputStream = new FileInputStream(ConfigurationConstants.FOGBOW_HOME + File.separator + DEFAULT_MAPPER_CONF);
        this.properties.load(fileInputStream);
	}
	
	@Override
	public Map<String, String> getCredentials(FederationUser federationUser) throws PropertyNotSpecifiedException {
		return getDefaultLocalTokenCredentials(this.properties);
	}
	
    /**
     * Gets credentials with prefix in the properties (LOCAL_TOKEN_CREDENTIALS_PREFIX).
     *
     * @param properties
     * @return
     * @throws PropertyNotSpecifiedException
     */
    public Map<String, String> getDefaultLocalTokenCredentials(Properties properties)
            throws PropertyNotSpecifiedException {
        Map<String, String> localDefaultTokenCredentials = new HashMap<String, String>();
        if (properties == null) {
            throw new PropertyNotSpecifiedException("Empty property map");
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
            throw new PropertyNotSpecifiedException("Default local token credentials not found");
        } else {
            return localDefaultTokenCredentials;
        }
    }

    private static String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
    }	

}
