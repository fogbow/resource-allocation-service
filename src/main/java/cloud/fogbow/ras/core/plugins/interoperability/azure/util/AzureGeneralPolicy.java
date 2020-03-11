package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import org.apache.commons.lang3.RandomStringUtils;

public class AzureGeneralPolicy {

    public static final int MAXIMUM_NETWORK_PER_VIRTUAL_MACHINE = 1;
    static final int MINIMUM_DISK = 30;
    static final String PASSWORD_PREFIX = "P4ss@";

    /**
     * Azure Password Policy:
     * 1) Contains an uppercase character
     * 2) Contains a lowercase character
     * 3) Contains a numeric digit
     * 4) Contains a special character
     * 5) Control characters are not allowed
     */
    public static String generatePassword() {
        return PASSWORD_PREFIX + RandomStringUtils.randomAlphabetic(PASSWORD_PREFIX.length());
    }

    /**
     * Azure Disk Policy
     * 1) Greater than 30GB
     */
    public static int getDisk(ComputeOrder computeOrder) throws InvalidParameterException {
        int disk = computeOrder.getDisk();
        if (disk < MINIMUM_DISK) {
            throw new InvalidParameterException(
                    String.format(Messages.Error.ERROR_DISK_PARAMETER_AZURE_POLICY, MINIMUM_DISK));
        }

        return disk;
    }


    public static void checkRegionName(String regionName) {
        if (Region.findByLabelOrName(regionName) == null) {
            throw new FatalErrorException(
                    String.format(Messages.Exception.INVALID_REGION_NAME, regionName));
        }
    }
}
