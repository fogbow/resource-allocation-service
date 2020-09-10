package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedSecurityRule extends EmulatedResource {
    private String securityGroupId;
    private String direction;
    private int portFrom;
    private int portTo;
    private String cidr;
    private String etherType;
    private String protocol;

    private EmulatedSecurityRule(String instanceId, String securityGroupId, String direction, int portFrom, int portTo,
                                 String cidr, String etherType, String protocol) {
        super(instanceId);
        this.securityGroupId = securityGroupId;
        this.cidr = cidr;
        this.portFrom = portFrom;
        this.portTo = portTo;
        this.direction = direction;
        this.etherType = etherType;
        this.protocol = protocol;
    }

    public static class Builder {
        private String instanceId;
        private String securityGroupId;
        private String direction;
        private int portFrom;
        private int portTo;
        private String cidr;
        private String etherType;
        private String protocol;

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder securityGroupId(String securityGroupId) {
            this.securityGroupId = securityGroupId;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder portFrom(int portFrom) {
            this.portFrom = portFrom;
            return this;
        }

        public Builder portTo(int portTo) {
            this.portTo = portTo;
            return this;
        }

        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public Builder etherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public EmulatedSecurityRule build(){
            return new EmulatedSecurityRule(instanceId, securityGroupId, direction, portFrom, portTo, cidr, etherType, protocol);
        }
    }

    public String getSecurityGroupId() { return securityGroupId; }

    public String getDirection() {
        return direction;
    }

    public int getPortFrom() {
        return portFrom;
    }

    public int getPortTo() {
        return portTo;
    }

    public String getCidr() {
        return cidr;
    }

    public String getEtherType() {
        return etherType;
    }

    public String getProtocol() {
        return protocol;
    }
}
