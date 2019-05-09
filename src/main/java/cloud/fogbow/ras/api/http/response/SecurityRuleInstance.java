package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import java.util.Objects;

public class SecurityRuleInstance extends Instance {
    private String cidr;
    private int portFrom;
    private int portTo;
    private SecurityRule.Direction direction;
    private SecurityRule.EtherType etherType;
    private SecurityRule.Protocol protocol;

    public SecurityRuleInstance(String id, SecurityRule.Direction direction, int portFrom, int portTo, String cidr, SecurityRule.EtherType etherType, SecurityRule.Protocol protocol) {
        super(id);
        this.cidr = cidr;
        this.portFrom = portFrom;
        this.portTo = portTo;
        this.direction = direction;
        this.etherType = etherType;
        this.protocol = protocol;
    }

    public SecurityRule.Direction getDirection() {
        return direction;
    }

    public void setDirection(SecurityRule.Direction direction) {
        this.direction = direction;
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

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public SecurityRule.EtherType getEtherType() {
        return etherType;
    }

    public void setEtherType(SecurityRule.EtherType etherType) {
        this.etherType = etherType;
    }

    public SecurityRule.Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(SecurityRule.Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return direction + ":" + portFrom + ":" + portTo + ":" + cidr + ":" + etherType + ":" + protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityRule that = (SecurityRule) o;
        return getPortFrom() == that.getPortFrom() &&
                getPortTo() == that.getPortTo() &&
                getDirection() == that.getDirection() &&
                Objects.equals(getCidr(), that.getCidr()) &&
                getEtherType() == that.getEtherType() &&
                getProtocol() == that.getProtocol();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDirection(), getPortFrom(), getPortTo(), getCidr(), getEtherType(), getProtocol());
    }
}
