package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SECURITY_GROUP;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshaller;

@XmlRootElement(name = SECURITY_GROUP)
public class GetSecurityGroupResponse extends OpenNebulaMarshaller {

    private String id;
    private String name;
    private GetSecurityRulesTemplate template;
    
    public GetSecurityGroupResponse() {}
    
    public String getId() {
        return id;
    }
    
    @XmlElement(name = ID)
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    @XmlElement(name = NAME)
    public void setName(String name) {
        this.name = name;
    }

    public GetSecurityRulesTemplate getTemplate() {
        return template;
    }
    
    @XmlElement(name = TEMPLATE)
    public void setTemplate(GetSecurityRulesTemplate template) {
        this.template = template;
    }
    
    public static Builder builder() {
        return new GetSecurityGroupResponse.Builder();
    }
    
    public static class Builder {
        
        private String id;
        private String name;
        private GetSecurityRulesTemplate template;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder template(GetSecurityRulesTemplate template) {
            this.template = template;
            return this;
        }
        
        public GetSecurityGroupResponse build() {
            return new GetSecurityGroupResponse(this);
        }
    }
    
    private GetSecurityGroupResponse(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.template = builder.template;
    }
    
    public static Unmarshaller unmarshaller() {
        return new GetSecurityGroupResponse.Unmarshaller();
    }
    
    public static class Unmarshaller {

        private String response;

        public Unmarshaller response(String response) {
            this.response = response;
            return this;
        }

        public GetSecurityGroupResponse unmarshal() {
            return (GetSecurityGroupResponse) OpenNebulaUnmarshaller
                    .unmarshal(this.response, GetSecurityGroupResponse.class);
        }
    }
    
}
