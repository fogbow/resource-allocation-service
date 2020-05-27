package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.apache.log4j.Logger;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;

public interface AzureGeneralUtil {

    @VisibleForTesting Logger LOGGER = Logger.getLogger(AzureGeneralUtil.class);
    @VisibleForTesting String NO_INFORMATION = null;

    // Generate a pattern name to all Resources that it will be created in the cloud
    static String generateResourceName() {
        return SdkContext.randomResourceName(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX, AzureConstants.MAXIMUM_RESOURCE_NAME_LENGTH);
    }

    // Define the Fogbow instance-id as Azure resource name
    static String defineInstanceId(String resourceName) {
        return resourceName;
    }

    // Define the Azure resource name as Fogbow instance-id
    static String defineResourceName(String instanceId) {
        return instanceId;
    }

    // Define the resource group name if created one with the resource name or
    // return the default resource group name
    static String defineResourceGroupName(Azure azure, String regionName, String resourceName, String defaultResourceGroupName) {
        try {
            return AzureResourceGroupOperationUtil.createResourceGroup(azure, regionName, resourceName);
        } catch (QuotaExceededException e) {
            LOGGER.warn(String.format(Messages.Warn.RESOURCE_CREATION_FAILED_S, e));
        }
        LOGGER.info(Messages.Info.CHANGE_TO_DEFAULT_RESOURCE_GROUP);
        return defaultResourceGroupName;
    }

    // Select the resource group name if exists a resource group with the resource name or
    // return the default resource group name
    static String selectResourceGroupName(Azure azure, String resourceName, String defaultResourceGroupName) {
        return AzureResourceGroupOperationUtil.existsResourceGroup(azure, resourceName) ? resourceName : defaultResourceGroupName;
    }

}
