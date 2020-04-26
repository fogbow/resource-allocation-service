package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class AzureCreateVirtualNetworkRef {

    @GenericBuilder.Required
    private String resourceName;
    @GenericBuilder.Required
    private String cidr;
    @GenericBuilder.Required
    private Map tags;

    public static AzureCreateVirtualNetworkRef.Builder builder() {
        return new AzureCreateVirtualNetworkRef.Builder(AzureCreateVirtualNetworkRef::new);
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public Map getTags() {
        return tags;
    }

    public void setTags(Map tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureCreateVirtualNetworkRef that = (AzureCreateVirtualNetworkRef) o;
        return Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(cidr, that.cidr) &&
                Objects.equals(tags, that.tags);
    }

    public int hashCode() {
        return Objects.hash(resourceName, cidr);
    }

    public static class Builder extends GenericBuilder<AzureCreateVirtualNetworkRef> {

        Builder(Supplier instantiator) {
            super(instantiator);
        }

        public AzureCreateVirtualNetworkRef.Builder resourceName(String resourceName) {
            with(AzureCreateVirtualNetworkRef::setResourceName, resourceName);
            return this;
        }

        public AzureCreateVirtualNetworkRef.Builder cidr(String cidr) {
            with(AzureCreateVirtualNetworkRef::setCidr, cidr);
            return this;
        }

        public AzureCreateVirtualNetworkRef.Builder tags(Map tags) {
            with(AzureCreateVirtualNetworkRef::setTags, tags);
            return this;
        }

    }

}
