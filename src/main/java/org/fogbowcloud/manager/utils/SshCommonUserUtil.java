package org.fogbowcloud.manager.utils;

import java.util.Properties;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;

public class SshCommonUserUtil {

    public static String getSshCommonUser() {
        String sshCommonUser = PropertiesUtil.getInstance().getProperty(
                        ConfigurationConstants.SSH_COMMON_USER_KEY,
                        DefaultConfigurationConstants.SSH_COMMON_USER);
        return sshCommonUser;
    }
}
