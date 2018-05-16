package org.fogbowcloud.manager.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AuthenticationServiceHelper {
	
	public static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentails_";

	public static Map<String, String> getLocalCredentials(Properties properties) {		
		Map<String, String> localDefaultTokenCredentials = new HashMap<String, String>();
		if (properties == null) {
			return localDefaultTokenCredentials;
		}
		for (Object keyProperties : properties.keySet()) {
			String keyPropertiesStr = keyProperties.toString();
			if (keyPropertiesStr.startsWith(LOCAL_TOKEN_CREDENTIALS_PREFIX)) {
				String key = keyPropertiesStr.replace(LOCAL_TOKEN_CREDENTIALS_PREFIX, "");
				String value = properties.getProperty(key);
				
				localDefaultTokenCredentials.put(key, value);
			}
		}
		return localDefaultTokenCredentials;
	}
	
}
