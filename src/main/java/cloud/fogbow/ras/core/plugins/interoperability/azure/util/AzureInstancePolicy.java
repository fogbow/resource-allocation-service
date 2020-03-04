package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.sun.istack.Nullable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;

public class AzureInstancePolicy {

    private static final int MAXIMUM_ID_LENGTH = 20;

    public static String defineAzureResourceName(
            @NotNull ComputeOrder computeOrder) throws InvalidParameterException {
        
        String orderName = computeOrder.getName();
        String resourceId = generateResourceId(AzureConstants.VIRTUAL_MACHINE_ID_PREFIX);
        return defineAndCheckResourceName(resourceId, orderName);
    }
    
    public static String generateResourceId(@NotBlank String prefix) {
        return SdkContext.randomResourceName(prefix, MAXIMUM_ID_LENGTH);
    }
    
    /**
     * Define the Azure Resource Name and check if It's in accordance with the policy.
     */
    @VisibleForTesting
    static String defineAndCheckResourceName(
            @NotBlank String resourceId,
            @Nullable String orderName) throws InvalidParameterException {

        String resourceName = resourceId 
                + AzureConstants.RESOURCE_NAME_SEPARATOR 
                + orderName;        

        AzureResourceIdBuilder.configure()
                .withResourceName(resourceName)
                .checkSizePolicy();

        return resourceName;
    }

}
