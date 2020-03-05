package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import com.sun.istack.Nullable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderName;

public class AzureInstancePolicy {

    private static final int MAXIMUM_ID_LENGTH = 20;

    public static String defineAzureResourceName(
            @NotNull Order order) throws InvalidParameterException {
        
        OrderName orderWithName = (OrderName) order;
        String orderName = orderWithName.getName();
        String resourceId = defineResourceIdBy(order);
        return defineAndCheckResourceName(resourceId, orderName);
    }

    @VisibleForTesting
    static String defineResourceIdBy(
            @NotNull Order order) throws InvalidParameterException {
        
        switch (order.getType()) {
        case COMPUTE:
            return generateResourceId(AzureConstants.VIRTUAL_MACHINE_ID_PREFIX);
        case NETWORK:
            return generateResourceId(AzureConstants.VIRTUAL_NETWORK_ID_PREFIX);
        case VOLUME:
            return generateResourceId(AzureConstants.VOLUME_ID_PREFIX);
        default:
            String message = String.format(
                    Messages.Exception.UNSUPPORTED_ATTRIBUTE_NAME_FROM_ORDER_TYPE_S, order.getType());
            
            throw new InvalidParameterException(message);
        }
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
