package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model;

public class AzureCreateVirtualNetworkRef {

    private String securityGroupName;
    private String resourceGroupName;
    private String cidr;

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getCidr() {
        return cidr;
    }
}
