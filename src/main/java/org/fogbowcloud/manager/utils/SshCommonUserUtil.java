package org.fogbowcloud.manager.utils;

import java.util.Properties;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;

public class SshCommonUserUtil {

    private static Properties
            properties; // TODO create class Properties, currently using java.util.Properties...

    public static void setProperties(Properties properties) {
        SshCommonUserUtil.properties = properties;
    }

    public static String getSshCommonUser() {
        String sshCommonUser =
                properties.getProperty(
                        ConfigurationConstants.SSH_COMMON_USER_KEY,
                        DefaultConfigurationConstants.SSH_COMMON_USER);
        return sshCommonUser;
    }
}
