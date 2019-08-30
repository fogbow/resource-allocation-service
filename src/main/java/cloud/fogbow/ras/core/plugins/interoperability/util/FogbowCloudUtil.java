package cloud.fogbow.ras.core.plugins.interoperability.util;

import java.util.UUID;

import cloud.fogbow.ras.constants.SystemConstants;

public class FogbowCloudUtil {

    public static String defineInstanceName(String instanceName) {
        return instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
