package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.common.annotations.VisibleForTesting;

public class AzureIdBuilder {

    // /subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Compute/virtualMachines/{virtualMachineName}
    @VisibleForTesting
    public static String VIRTUAL_MACHINE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";

    // /subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Network/networkInterfaces/{networkInterfaceName}
    @VisibleForTesting
    public static String NETWORK_INTERFACE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s";

    // /subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Compute/disks/{diskName}
    @VisibleForTesting
    public static final String BIGGER_STRUCTURE = NETWORK_INTERFACE_STRUCTURE;

    public static AzureIdBuilderConfigured configure(AzureUser azureCloudUser) {
        return new AzureIdBuilderConfigured(azureCloudUser);
    }

    public static class AzureIdBuilderConfigured {
        private AzureUser azureUser;
        private String resourceGroupName;
        private String resourceName;
        private String structure;

        public AzureIdBuilderConfigured(AzureUser azureCloudUser) {
            this.azureUser = azureCloudUser;
        }
        
        public AzureIdBuilderConfigured resourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzureIdBuilderConfigured resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public AzureIdBuilderConfigured structure(String structure) {
            this.structure = structure;
            return this;
        }

        public String build() {
            String subscriptionId = this.azureUser.getSubscriptionId();
            String resourceGroupName = this.resourceGroupName;
            return String.format(this.structure, subscriptionId, resourceGroupName, this.resourceName);
        }

        /**
         * It checks the resource name size in relation to Database Instance Order Id Maximum Size.
         * It happens because the resource name makes up the instance Id. Also, it might happen due
         * to the fact that the user choose some values such as resourceName and resourceGroupName.
         */
        public void checkIdSizePolicy(String resourceName) throws InvalidParameterException {
            this.resourceName(resourceName);
            this.structure(BIGGER_STRUCTURE);
            String idBuilt = this.build();
            int sizeExceeded = idBuilt.length() - Order.FIELDS_MAX_SIZE;
            if (sizeExceeded > 0) {
                throw new InvalidParameterException(
                        String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, sizeExceeded));
            }
        }

    }

}
