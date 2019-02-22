package cloud.fogbow.ras.api.http.response.securityrules;

import java.util.Objects;

public class SecurityRule {

    private String instanceId;
    private String cidr;
    private int portFrom;
    private int portTo;
    private Direction direction;
    private EtherType etherType;
    private Protocol protocol;

    public SecurityRule() {
    }

    public SecurityRule(Direction direction, int portFrom, int portTo, String cidr, EtherType etherType, Protocol protocol) {
        this.direction = direction;
        this.portFrom = portFrom;
        this.portTo = portTo;
        this.cidr = cidr;
        this.etherType = etherType;
        this.protocol = protocol;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
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

    public EtherType getEtherType() {
        return etherType;
    }

    public void setEtherType(EtherType etherType) {
        this.etherType = etherType;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityRule that = (SecurityRule) o;
        return getPortFrom() == that.getPortFrom() &&
                getPortTo() == that.getPortTo() &&
                Objects.equals(getInstanceId(), that.getInstanceId()) &&
                getDirection() == that.getDirection() &&
                Objects.equals(getCidr(), that.getCidr()) &&
                getEtherType() == that.getEtherType() &&
                getProtocol() == that.getProtocol();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceId(), getDirection(), getPortFrom(), getPortTo(), getCidr(),
                getEtherType(), getProtocol());
    }

}
