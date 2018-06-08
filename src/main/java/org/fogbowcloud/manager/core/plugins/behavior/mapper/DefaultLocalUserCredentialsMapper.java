package org.fogbowcloud.manager.core.plugins.behavior.mapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.utils.PropertiesUtil;

public class DefaultLocalUserCredentialsMapper implements LocalUserCredentialsMapperPlugin {
	
    private static final Logger LOGGER = Logger.getLogger(DefaultLocalUserCredentialsMapper.class);
    
    private static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentials_";
	private static final String DEFAULT_MAPPER_CONF = "default_mapper.conf";
	
	private Properties properties;
	
	public DefaultLocalUserCredentialsMapper() {
	    this.properties = new Properties();
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() +
                File.separator + DEFAULT_MAPPER_CONF);
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
            throw new PropertyNotSpecifiedException("Empty property map.");
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
            throw new PropertyNotSpecifiedException("Default localidentity token credentials not found.");
        } else {
            return localDefaultTokenCredentials;
        }
    }

    private static String normalizeKeyProperties(String keyPropertiesStr) {
        return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
    }	

}
