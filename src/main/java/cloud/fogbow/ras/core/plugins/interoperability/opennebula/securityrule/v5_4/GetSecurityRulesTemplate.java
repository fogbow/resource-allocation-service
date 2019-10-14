package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.RULE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = TEMPLATE)
public class GetSecurityRulesTemplate extends OpenNebulaMarshaller {

    private List<Rule> rules;
    
    public GetSecurityRulesTemplate() {}

    public List<Rule> getRules() {
        return rules;
    }
    
    @XmlElement(name = RULE)
    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
    
    public static Builder builder() {
        return new GetSecurityRulesTemplate.Builder();
    }

    public static class Builder {
        
        private List<Rule> rules;
        
        public Builder rules(List<Rule> rules) {
            this.rules = rules;
            return this;
        }
        
        public GetSecurityRulesTemplate build() {
            return new GetSecurityRulesTemplate(this);
        }
    }
    
    private GetSecurityRulesTemplate(Builder builder) {
        this.rules = builder.rules;
    }
    
}
