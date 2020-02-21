package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class AzureInstancePolicy {

    /**
     * Generate the Azure Resource Name and check if It's in accordance with the policy.
     */
    private static String generateAzureResourceNameBy(@Nullable String orderName, String orderId,
                                                      AzureUser azureCloudUser)
            throws InvalidParameterException {

        if (orderName == null) {
            orderName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + orderId;
        }

        AzureIdBuilder.configure(azureCloudUser).checkIdSizePolicy(orderName);
        return orderName;
    }

    public static String generateAzureResourceNameBy(
            ComputeOrder computeOrder, AzureUser azureCloudUser) throws InvalidParameterException {

        return generateAzureResourceNameBy(computeOrder.getName(), computeOrder.getId(), azureCloudUser);
    }

    public static String generateAzureResourceNameBy(String orderId, AzureUser azureUser)
            throws InvalidParameterException {

        return generateAzureResourceNameBy(null, orderId, azureUser);
    }

    public static String generateFogbowInstanceIdBy(String orderId, AzureUser azureCloudUser,
                                                    BiFunction<String, AzureUser, String> builderId)
            throws InvalidParameterException {

        String resourceName = generateAzureResourceNameBy(orderId, azureCloudUser);
        return generateFogbowIstanceId(resourceName, azureCloudUser, builderId);
    }

    public static String generateFogbowInstanceIdBy(ComputeOrder computeOrder, AzureUser azureUser)
            throws InvalidParameterException {

        String resourceName = generateAzureResourceNameBy(computeOrder, azureUser);
        return AzureIdBuilder
                .configure(azureUser)
                .structure(AzureIdBuilder.VIRTUAL_MACHINE_STRUCTURE)
                .resourceName(resourceName)
                .build();
    }

    private static String generateFogbowIstanceId(String resourceName, AzureUser azureUser,
                                                  BiFunction<String, AzureUser, String> builderId) {

        return builderId.apply(resourceName, azureUser);
    }


}
