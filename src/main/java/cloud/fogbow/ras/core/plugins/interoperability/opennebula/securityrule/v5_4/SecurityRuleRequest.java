package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.IP;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NETWORK_ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.PROTOCOL;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RANGE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RULE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SIZE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = RULE)
public final class SecurityRuleRequest extends OpenNebulaMarshaller {
    
    private String protocol;
    private String ip;
    private String size;
    private String range;
    private String type;
    private String networkId;
    private String groupId;

    public SecurityRuleRequest() {}
    
    @XmlElement(name = PROTOCOL)
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @XmlElement(name = IP)
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }

    @XmlElement(name = SIZE)
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }

    @XmlElement(name = RANGE)
    public String getRange() {
        return range;
    }
    
    public void setRange(String range) {
        this.range = range;
    }

    @XmlElement(name = TYPE)
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = NETWORK_ID)
    public String getNetworkId() {
        return networkId;
    }
    
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @XmlTransient
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public static Builder builder() {
        return new SecurityRuleRequest.Builder();
    }
    
    public static class Builder {
        
        private String protocol;
        private String ip;
        private String size;
        private String range;
        private String type;
        private String networkId;
        private String groupId;
        
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        
        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }
        
        public Builder size(String size) {
            this.size = size;
            return this;
        }
        
        public Builder range(String range) {
            this.range = range;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }
        
        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }
        
        public SecurityRuleRequest build() {
            return new SecurityRuleRequest(this);
        }
        
    }
    
    private SecurityRuleRequest(Builder builder) {
        this.protocol = builder.protocol;
        this.ip = builder.ip;
        this.size = builder.size;
        this.range = builder.range;
        this.type = builder.type;
        this.networkId = builder.networkId;
        this.groupId = builder.groupId;
    }
    
}
