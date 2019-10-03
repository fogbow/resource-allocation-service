package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RULE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = TEMPLATE)
public class CreateSecurityGroupRequest extends OpenNebulaMarshaller {

    private String id;
    private String name;
    private List<Rule> rules;
    
    public CreateSecurityGroupRequest() {}
    
    @XmlElement(name = ID)
    public String getId() {
        return id;
    }

    @XmlElement(name = NAME)
    public String getName() {
        return name;
    }
    
    @XmlElement(name = RULE)
    public List<Rule> getRules() {
        return rules;
    }
    
    public static Builder builder() {
        return new CreateSecurityGroupRequest.Builder();
    }
    
    public static class Builder {
        
        private String id;
        private String name;
        private List<Rule> rules;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder rules(List<Rule> rules) {
            this.rules = rules;
            return this;
        }
        
        public CreateSecurityGroupRequest build() {
            return new CreateSecurityGroupRequest(this);
        }
        
    }
    
    private CreateSecurityGroupRequest(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.rules = builder.rules;
    }
    
}
