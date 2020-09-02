package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

import java.security.InvalidParameterException;
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
}
