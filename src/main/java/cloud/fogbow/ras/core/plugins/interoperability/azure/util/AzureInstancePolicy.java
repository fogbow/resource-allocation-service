package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;

public class AzureInstancePolicy {

    public static String defineAzureResourceName(
            @NotNull ComputeOrder computeOrder,
            @NotNull AzureUser azureCloudUser, 
            @NotBlank String resourceGroupName) throws InvalidParameterException {

        String subscriptionId = azureCloudUser.getSubscriptionId();
        return defineAzureResourceName(computeOrder.getId(), resourceGroupName, subscriptionId);
    }

    /**
     * Define the Azure Resource Name and check if It's in accordance with the policy.
     */
    private static String defineAzureResourceName(
            @NotBlank String orderId,
            @NotBlank String resourceGroupName, 
            @NotBlank String subscriptionId) throws InvalidParameterException {

        String resourceName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + orderId;

        AzureResourceIdBuilder.configure()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .checkIdSizePolicy();

        return resourceName;
    }

    public static String generateFogbowInstanceId(
            @NotNull ComputeOrder computeOrder, 
            @NotNull AzureUser azureUser,
            @NotBlank String resourceGroupName) throws InvalidParameterException {

        String resourceName = defineAzureResourceName(computeOrder, azureUser, resourceGroupName);
        return AzureResourceIdBuilder.configure(AzureConstants.VIRTUAL_MACHINE_STRUCTURE)
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .buildResourceId();
    }

}
