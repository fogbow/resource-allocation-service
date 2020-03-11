package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.constants.AzureConstants;

public class AzureResourceIdBuilder {

    public static AzureResourceIdConfigured virtualMachineId() {
        return new AzureResourceIdConfigured(AzureConstants.VIRTUAL_MACHINE_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured networkInterfaceId() {
        return new AzureResourceIdConfigured(AzureConstants.NETWORK_INTERFACE_STRUCTURE);
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
