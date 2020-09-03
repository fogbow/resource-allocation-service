package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;
import org.apache.commons.lang.StringUtils;

import java.security.InvalidParameterException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class EmulatedCloudUtils {

    public static String getName(String name) {
        return (name == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + EmulatedCloudUtils.getRandomUUID() : name);
    }

    public static String getNetworkSecurityGroupId(String instanceId) {
        return SystemConstants.PN_SECURITY_GROUP_PREFIX + instanceId;
    }

    public static String getPublicIpSecurityGroupId(String instanceId) {
        return SystemConstants.PIP_SECURITY_GROUP_PREFIX + instanceId;
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public static boolean validateInstanceId(String instanceId) {
        return instanceId != null && !instanceId.trim().isEmpty();
    }

    public static void validateEmulatedResource(EmulatedResource resource) {
        if (resource == null) {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.EMULATED_RESOURCE_UNDEFINED);
        }

        if (resource.getInstanceId() == null || resource.getInstanceId().trim().equals("")) {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.INVALID_INSTANCE_ID);
        }
    }

    public static String generateRandomIP() {
        Random rand = new Random();
        String[] octets = new String[4];

        for (int i = 0; i < 4; ++i) {
            int newOctet = rand.nextInt(250) + 3;
            octets[i] = String.valueOf(newOctet);
        }

        return String.join(".", octets);
    }

    public static String generateMac() {
        char[] hexas = "0123456789abcdef".toCharArray();
        String newMac = "";
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && (i & 1) == 0) {
                newMac += ':';
            }

            int index = random.nextInt(16);

            newMac += hexas[index];

        }
        return newMac;
    }

    public static void checkQuotaProperties(Properties properties) {
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_INSTANCES_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_RAM_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_VCPU_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_VOLUMES_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_STORAGE_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_PUBLIC_IP_KEY);
        checkQuotaProperty(properties, EmulatedCloudConstants.Conf.QUOTA_NETWORKS_KEY);
    }

    private static void checkQuotaProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            String message = String.format(EmulatedCloudConstants.Exception.THE_REQUIRED_PROPERTY_S_WAS_NOT_SPECIFIED, key);
            throw new FatalErrorException(message);
        } else if (!StringUtils.isNumeric(value)) {
            String message = String.format(EmulatedCloudConstants.Exception.THE_PROPERTY_S_MUST_BE_AN_INTEGER, key);
            throw new FatalErrorException(message);
        }
    }
}
