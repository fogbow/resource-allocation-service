package org.fogbowcloud.manager.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;

// TODO change the name. Follow AAAController's name
public class AuthenticationControllerUtil {

    protected static String LOCAL_TOKEN_CREDENTIALS_PREFIX = "local_token_credentails_";

    /**
     * Gets credentials with prefix in the properties (LOCAL_TOKEN_CREDENTIALS_PREFIX).
     *
     * @param properties
     * @return
     * @throws PropertyNotSpecifiedException
     */
    public static Map<String, String> getDefaultLocalTokenCredentials(Properties properties)
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

    // TODO change to other class util
    public static boolean isOrderProvidingLocally(
            String orderProvadingMember, Properties properties) {
        String localMember = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
        if (orderProvadingMember == null || orderProvadingMember.equals(localMember)) {
            return true;
        }
        return false;
    }
}
