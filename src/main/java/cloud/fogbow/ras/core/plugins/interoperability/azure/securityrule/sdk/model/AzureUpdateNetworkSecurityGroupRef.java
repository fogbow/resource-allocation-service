package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.GenericBuilder;

import java.util.Objects;
import java.util.function.Supplier;

public class AzureUpdateNetworkSecurityGroupRef {

    private String networkSecurityGroupId;
    private String ruleResourceName;
    private String cidr;
    private int portFrom;
    private int portTo;
    private SecurityRule.Direction direction;
    private SecurityRule.Protocol protocol;

    public static AzureUpdateNetworkSecurityGroupRef.Builder builder() {
        return new AzureUpdateNetworkSecurityGroupRef.Builder(AzureUpdateNetworkSecurityGroupRef::new);
    }

    public String getNetworkSecurityGroupId() {
        return networkSecurityGroupId;
    }

    public String getRuleResourceName() {
        return ruleResourceName;
    }

    public void setRuleResourceName(String ruleResourceName) {
        this.ruleResourceName = ruleResourceName;
    }

    public void setNetworkSecurityGroupId(String networkSecurityGroupId) {
        this.networkSecurityGroupId = networkSecurityGroupId;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public int getPortFrom() {
        return portFrom;
    }

    public void setPortFrom(int portFrom) {
        this.portFrom = portFrom;
    }

    public int getPortTo() {
        return portTo;
    }

    public void setPortTo(int portTo) {
        this.portTo = portTo;
    }

    public SecurityRule.Direction getDirection() {
        return direction;
    }

    public void setDirection(SecurityRule.Direction direction) {
        this.direction = direction;
    }

    public SecurityRule.Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(SecurityRule.Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureUpdateNetworkSecurityGroupRef that = (AzureUpdateNetworkSecurityGroupRef) o;
        return portFrom == that.portFrom &&
                portTo == that.portTo &&
                Objects.equals(networkSecurityGroupId, that.networkSecurityGroupId) &&
                Objects.equals(cidr, that.cidr) &&
                direction == that.direction &&
                protocol == that.protocol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkSecurityGroupId, cidr, portFrom, portTo, direction, protocol);
    }

    public static class Builder extends GenericBuilder<AzureUpdateNetworkSecurityGroupRef> {

        protected Builder(Supplier<AzureUpdateNetworkSecurityGroupRef> instantiator) {
            super(instantiator);
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder cidr(String cidr) {
            with(AzureUpdateNetworkSecurityGroupRef::setCidr, cidr);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder protocol(SecurityRule.Protocol protocol) {
            with(AzureUpdateNetworkSecurityGroupRef::setProtocol, protocol);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder direction(SecurityRule.Direction direction) {
            with(AzureUpdateNetworkSecurityGroupRef::setDirection, direction);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder portFrom(int portFrom) {
            with(AzureUpdateNetworkSecurityGroupRef::setPortFrom, portFrom);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder portTo(int portTo) {
            with(AzureUpdateNetworkSecurityGroupRef::setPortTo, portTo);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder networkSecurityGroupId(String networkSecurityGroupId) {
            with(AzureUpdateNetworkSecurityGroupRef::setNetworkSecurityGroupId, networkSecurityGroupId);
            return this;
        }

        public AzureUpdateNetworkSecurityGroupRef.Builder ruleResourceName(String ruleResourceName) {
            with(AzureUpdateNetworkSecurityGroupRef::setRuleResourceName, ruleResourceName);
            return this;
        }

    }

}
