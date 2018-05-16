package org.fogbowcloud.manager.core.utils;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;

public class SshCommonUserUtil {
	
	private static Properties properties; // TODO create class Properties, currently using java.util.Properties...
	
	public static String getSshCommonUser() {		
		String sshCommonUser = properties.getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY, DefaultConfigurationConstants.SSH_COMMON_USER);	
		return sshCommonUser;
	}
	
}
