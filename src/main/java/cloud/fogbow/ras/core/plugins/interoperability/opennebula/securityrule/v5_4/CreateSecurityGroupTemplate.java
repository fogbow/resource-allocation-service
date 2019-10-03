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
public class CreateSecurityGroupTemplate extends OpenNebulaMarshaller {

    private String id;
    private String name;
    private List<SecurityRuleRequest> rules;
    
    public CreateSecurityGroupTemplate() {}
    
    @XmlElement(name = ID)
    public String getId() {
        return id;
    }

    @XmlElement(name = NAME)
    public String getName() {
        return name;
    }
    
    @XmlElement(name = RULE)
    public List<SecurityRuleRequest> getRules() {
        return rules;
    }
    
    public static Builder builder() {
        return new CreateSecurityGroupTemplate.Builder();
    }
    
    public static class Builder {
        
        private String id;
        private String name;
        private List<SecurityRuleRequest> rules;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder rules(List<SecurityRuleRequest> rules) {
            this.rules = rules;
            return this;
        }
        
        public CreateSecurityGroupTemplate build() {
            return new CreateSecurityGroupTemplate(this);
        }
        
    }
    
    private CreateSecurityGroupTemplate(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.rules = builder.rules;
    }
    
}
