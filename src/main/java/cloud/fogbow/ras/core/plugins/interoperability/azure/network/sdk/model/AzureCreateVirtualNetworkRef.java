package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.Objects;
import java.util.function.Supplier;

public class AzureCreateVirtualNetworkRef {

    @GenericBuilder.Required
    private String name;
    @GenericBuilder.Required
    private String resourceGroupName;
    @GenericBuilder.Required
    private String cidr;

    public static AzureCreateVirtualNetworkRef.Builder builder() {
        return new AzureCreateVirtualNetworkRef.Builder(AzureCreateVirtualNetworkRef::new);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureCreateVirtualNetworkRef that = (AzureCreateVirtualNetworkRef) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(resourceGroupName, that.resourceGroupName) &&
                Objects.equals(cidr, that.cidr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resourceGroupName, cidr);
    }

    public static class Builder extends GenericBuilder<AzureCreateVirtualNetworkRef> {

        Builder(Supplier instantiator) {
            super(instantiator);
        }

        public AzureCreateVirtualNetworkRef.Builder name(String name) {
            with(AzureCreateVirtualNetworkRef::setName, name);
            return this;
        }

        public AzureCreateVirtualNetworkRef.Builder resourceGroupName(String resourceGroupName) {
            with(AzureCreateVirtualNetworkRef::setResourceGroupName, resourceGroupName);
            return this;
        }

        public AzureCreateVirtualNetworkRef.Builder cidr(String cidr) {
            with(AzureCreateVirtualNetworkRef::setCidr, cidr);
            return this;
        }

    }

}
