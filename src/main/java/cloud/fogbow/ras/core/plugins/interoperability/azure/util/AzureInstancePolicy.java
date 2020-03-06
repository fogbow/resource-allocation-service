package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public class AzureInstancePolicy {

    private static final int MAXIMUM_ID_LENGTH = 20;

    public static String defineAzureResourceName(
            @NotNull Order order) throws InvalidParameterException {
        
        switch (order.getType()) {
        case COMPUTE:
            return defineVirtualMachineIdBy(order);
        case NETWORK:
            return defineVirtualNetworkIdBy(order);
        case VOLUME:
            return defineVolumeIdBy(order);
        case PUBLIC_IP:
            return generateResourceId(AzureConstants.PUBLIC_IP_ID_PREFIX);
        default:
			String message = String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE, order.getType());
        	throw new InvalidParameterException(message);
        }
    }
    
    @VisibleForTesting
	static String defineVolumeIdBy(
			@NotNull Order order) throws InvalidParameterException {
		
    	String resourceId = generateResourceId(AzureConstants.VOLUME_ID_PREFIX);
		VolumeOrder volumeOrder = (VolumeOrder) order;
		String orderName = volumeOrder.getName();
		return defineAndCheckResourceName(resourceId, orderName);
	}

    @VisibleForTesting
	static String defineVirtualNetworkIdBy(
			@NotNull Order order) throws InvalidParameterException {
		
    	String resourceId = generateResourceId(AzureConstants.VIRTUAL_NETWORK_ID_PREFIX);
		NetworkOrder networkOrder = (NetworkOrder) order;
		String orderName = networkOrder.getName();
		return defineAndCheckResourceName(resourceId, orderName);
	}

    @VisibleForTesting
	static String defineVirtualMachineIdBy(
			@NotNull Order order) throws InvalidParameterException {
		
    	String resourceId = generateResourceId(AzureConstants.VIRTUAL_MACHINE_ID_PREFIX);
		ComputeOrder computeOrder = (ComputeOrder) order;
		String orderName = computeOrder.getName();
		return defineAndCheckResourceName(resourceId, orderName);
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

    public static String generateResourceId(@NotBlank String prefix) {
    	return SdkContext.randomResourceName(prefix, MAXIMUM_ID_LENGTH);
    }
    
}
