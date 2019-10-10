package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.IP;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NETWORK_ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.PROTOCOL;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RANGE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RULE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SIZE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RULE_TYPE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = RULE)
public final class Rule extends OpenNebulaMarshaller {
    
    private String protocol;
    private String ip;
    private String size;
    private String range;
    private String type;
    private String networkId;
    private String groupId;

    public Rule() {}
    
    public Rule(String protocol, String ip, String size, String range, String type, String networkId, String groupId) {
        this.protocol = protocol;
        this.ip = ip;
        this.size = size;
        this.range = range;
        this.type = type;
        this.networkId = networkId;
        this.groupId = groupId;
    }

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

    @XmlElement(name = RULE_TYPE)
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
        return new Rule.Builder();
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
        
        public Rule build() {
            return new Rule(this);
        }
        
    }
    
    private Rule(Builder builder) {
        this.protocol = builder.protocol;
        this.ip = builder.ip;
        this.size = builder.size;
        this.range = builder.range;
        this.type = builder.type;
        this.networkId = builder.networkId;
        this.groupId = builder.groupId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Rule other = (Rule) obj;
        if (ip == null) {
            if (other.ip != null)
                return false;
        } else if (!ip.equals(other.ip))
            return false;
        if (networkId == null) {
            if (other.networkId != null)
                return false;
        } else if (!networkId.equals(other.networkId))
            return false;
        if (protocol == null) {
            if (other.protocol != null)
                return false;
        } else if (!protocol.equals(other.protocol))
            return false;
        if (range == null) {
            if (other.range != null)
                return false;
        } else if (!range.equals(other.range))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }
    
}
