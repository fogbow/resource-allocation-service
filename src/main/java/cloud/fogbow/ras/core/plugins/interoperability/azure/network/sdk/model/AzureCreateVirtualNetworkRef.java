package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model;

public class AzureCreateVirtualNetworkRef {

    private String name;
    private String resourceGroupName;
    private String cidr;

    public String getName() {
        return name;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getCidr() {
        return cidr;
    }
}
