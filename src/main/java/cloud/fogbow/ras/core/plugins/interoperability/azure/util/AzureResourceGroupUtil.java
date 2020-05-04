package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import rx.Completable;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.ras.constants.Messages;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;

public class AzureResourceGroupUtil {

    public static String create(Azure azure, String regionName, String resourceGroupName)
            throws FogbowException {
        try {
            ResourceGroup resourceGroup = azure.resourceGroups()
                    .define(resourceGroupName)
                    .withRegion(regionName)
                    .create();
            
            return resourceGroup.name();
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new QuotaExceededException(message);
        }
    }

    public static boolean exists(Azure azure, String resourceGroupName) {
        return azure.resourceGroups().checkExistence(resourceGroupName);
    }

    public static Completable deleteAsync(Azure azure, String resourceGroupName) {
        return azure.resourceGroups().deleteByNameAsync(resourceGroupName);
    }

}
