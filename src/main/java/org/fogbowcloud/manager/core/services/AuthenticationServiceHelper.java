package org.fogbowcloud.manager.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//TODO change the name. Follow AuthenticationService's name
public class AuthenticationServiceHelper {
	
	protected static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentails_";

	/*
	 * Catch credentials with prefix in the properties (LOCAL_TOKEN_CREDENTIALS_PREFIX) 
	 */
	public static Map<String, String> getDefaultLocalTokenCredentials(Properties properties) {		
		Map<String, String> localDefaultTokenCredentials = new HashMap<String, String>();
		if (properties == null) {
			return localDefaultTokenCredentials;
		}
		for (Object keyProperties : properties.keySet()) {
			String keyPropertiesStr = keyProperties.toString();
			if (keyPropertiesStr.startsWith(LOCAL_TOKEN_CREDENTIALS_PREFIX)) {
				String value = properties.getProperty(keyPropertiesStr);
				String key = normalizeKeyProperties(keyPropertiesStr);
				
				localDefaultTokenCredentials.put(key, value);
			}
		}
		return localDefaultTokenCredentials;
	}

	private static String normalizeKeyProperties(String keyPropertiesStr) {
		return keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
	}
	
}
