package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

public class AzureResourceIdBuilder {

    public static final String VIRTUAL_MACHINE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s";
    public static final String NETWORK_INTERFACE_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s";
    public static final String DISK_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/disks/%s";
    public static final String NETWORK_SECURITY_GROUP_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkSecurityGroups/%s";
    public static final String NETWORK_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networks/%s";
    public static final String PUBLIC_IP_ADDRESS_STRUCTURE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses/%s";

    public static AzureResourceIdConfigured virtualMachineId() {
        return new AzureResourceIdConfigured(VIRTUAL_MACHINE_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured networkInterfaceId() {
        return new AzureResourceIdConfigured(NETWORK_INTERFACE_STRUCTURE);
    }

    public static AzureResourceIdConfigured networkSecurityGroupId() {
        return new AzureResourceIdConfigured(NETWORK_SECURITY_GROUP_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured diskId() {
        return new AzureResourceIdConfigured(DISK_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured networkId() {
        return new AzureResourceIdConfigured(NETWORK_STRUCTURE);
    }
    
    public static AzureResourceIdConfigured publicIpAddressId() {
        return new AzureResourceIdConfigured(PUBLIC_IP_ADDRESS_STRUCTURE);
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
