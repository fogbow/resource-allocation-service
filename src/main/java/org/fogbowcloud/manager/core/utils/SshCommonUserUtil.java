package org.fogbowcloud.manager.core.utils;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.CommonConfigurationConstants;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;

public class SshCommonUserUtil {
	
	private static Properties properties; // TODO create class Properties, currently using java.util.Properties...
	
	public static String getSshCommonUser() {		
		String sshCommonUser = properties.getProperty(ConfigurationConstants.SSH_COMMON_USER_KEY);
		return sshCommonUser == null ? CommonConfigurationConstants.DEFAULT_COMMON_SSH_USER : sshCommonUser;
	}
	
}
