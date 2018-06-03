package org.fogbowcloud.manager.utils;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;

public class PropertiesUtil {

	private static Properties properties = null;

    public static synchronized Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }
        return properties;
    }

    public static String getLocalMemberId() {
    	Properties properties = getProperties();
    	return properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
    }
	
}
