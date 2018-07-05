package org.fogbowcloud.manager.util.connectivity;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;

public class SshCommonUserUtil {

    public static String getSshCommonUser() {
        String sshCommonUser = PropertiesHolder.getInstance().getProperty(
                        ConfigurationConstants.SSH_COMMON_USER_KEY,
                        DefaultConfigurationConstants.SSH_COMMON_USER);
        return sshCommonUser;
    }
}
