package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import com.google.common.annotations.VisibleForTesting;

public class AzureResourceIdBuilder {

    @VisibleForTesting
    static final String VIRTUAL_MACHINE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";
    
    @VisibleForTesting
    static final String NETWORK_INTERFACE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s";
    
    @VisibleForTesting
    static final String PUBLIC_IP_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses/%s";

    public static AzureResourceIdConfigured virtualMachineId() {
        return new AzureResourceIdConfigured(VIRTUAL_MACHINE_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured networkInterfaceId() {
        return new AzureResourceIdConfigured(NETWORK_INTERFACE_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured publicIpId() {
        return new AzureResourceIdConfigured(PUBLIC_IP_STRUCTURE);
    }

    public static class AzureResourceIdConfigured {
        
        private String structure;
        private String subscriptionId;
        private String resourceGroupName;
        private String resourceName;
        
        public AzureResourceIdConfigured(String structure) {
            this.structure = structure;
        }
        
        public AzureResourceIdConfigured withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public AzureResourceIdConfigured withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzureResourceIdConfigured withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public String build() {
            return String.format(this.structure, this.subscriptionId, this.resourceGroupName, this.resourceName);
        }

    }

}
