package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.models.AzureUser;
import com.google.common.annotations.VisibleForTesting;

public class AzureIdBuilder {

    // /subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Compute/virtualMachines/{virtualMachineName}
    @VisibleForTesting
    static String VIRTUAL_MACHINE_STRUCTURE =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";

    public static AzureIdBuilderConfigured configure(AzureUser azureCloudUser) {
        return new AzureIdBuilderConfigured(azureCloudUser);
    }

    public static class AzureIdBuilderConfigured {

        private AzureUser azureUser;

        public AzureIdBuilderConfigured(AzureUser azureCloudUser) {
            this.azureUser = azureCloudUser;
        }

        public String buildVirtualMachineId(String virtualMachineName) {
            return buildId(VIRTUAL_MACHINE_STRUCTURE, virtualMachineName);
        }

        @VisibleForTesting
        String buildId(String structure, String name) {
            String subscriptionId = this.azureUser.getSubscriptionId();
            String resourceGroupName = this.azureUser.getResourceGroupName();
            return String.format(structure, subscriptionId, resourceGroupName, name);
        }

    }

}
