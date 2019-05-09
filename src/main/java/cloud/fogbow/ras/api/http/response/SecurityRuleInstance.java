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

    public SecurityRuleInstance(String id, SecurityRule rule) {
        super(id);
        this.cidr = rule.getCidr();
        this.portFrom = rule.getPortFrom();
        this.portTo = rule.getPortTo();
        this.direction = rule.getDirection();
        this.etherType = rule.getEtherType();
        this.protocol = rule.getProtocol();
    }

    public SecurityRule getRule() {
        return new SecurityRule(this.direction, this.portFrom, this.portTo, this.cidr, this.etherType, this.protocol);
    }

    @Override
    public String toString() {
        return getId() + ">" + this.getRule().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityRuleInstance that = (SecurityRuleInstance) o;
        return getId() == that.getId() &&
                getRule() == that.getRule();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getRule());
    }
}
