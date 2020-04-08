package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.Objects;
import java.util.function.Supplier;

public class AzureGetVirtualNetworkRef {

    private String id;
    private String state;
    private String name;
    private String cidr;

    public static AzureGetVirtualNetworkRef.Builder builder() {
        return new AzureGetVirtualNetworkRef.Builder(AzureGetVirtualNetworkRef::new);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        AzureGetVirtualNetworkRef that = (AzureGetVirtualNetworkRef) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(state, that.state) &&
                Objects.equals(name, that.name) &&
                Objects.equals(cidr, that.cidr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state, name, cidr);
    }

    public static class Builder extends GenericBuilder<AzureGetVirtualNetworkRef> {

        Builder(Supplier instantiator) {
            super(instantiator);
        }

        public AzureGetVirtualNetworkRef.Builder name(String name) {
            with(AzureGetVirtualNetworkRef::setName, name);
            return this;
        }

        public AzureGetVirtualNetworkRef.Builder id(String id) {
            with(AzureGetVirtualNetworkRef::setId, id);
            return this;
        }

        public AzureGetVirtualNetworkRef.Builder cidr(String cidr) {
            with(AzureGetVirtualNetworkRef::setCidr, cidr);
            return this;
        }

        public AzureGetVirtualNetworkRef.Builder state(String state) {
            with(AzureGetVirtualNetworkRef::setState, state);
            return this;
        }

    }

}
