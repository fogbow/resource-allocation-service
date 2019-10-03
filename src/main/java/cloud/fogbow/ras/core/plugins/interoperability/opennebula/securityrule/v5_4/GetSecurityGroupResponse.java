package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.NAME;
import static cloud.fogbow.common.constants.OpenNebulaConstants.SECURITY_GROUP;
import static cloud.fogbow.common.constants.OpenNebulaConstants.TEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshaller;

@XmlRootElement(name = SECURITY_GROUP)
public class GetSecurityGroupResponse extends OpenNebulaMarshaller {

    private static final Logger LOGGER = Logger.getLogger(GetSecurityGroupResponse.class);
    
    private String id;
    private String name;
    private GetSecurityRulesResponse template;
    
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

    public GetSecurityRulesResponse getTemplate() {
        return template;
    }
    
    @XmlElement(name = TEMPLATE)
    public void setTemplate(GetSecurityRulesResponse template) {
        this.template = template;
    }
    
    public static Builder builder() {
        return new GetSecurityGroupResponse.Builder();
    }
    
    public static class Builder {
        
        private String id;
        private String name;
        private GetSecurityRulesResponse template;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder template(GetSecurityRulesResponse template) {
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
    
    public static GetSecurityGroupResponse unmarshal(String xml) {
        GetSecurityGroupResponse securityGroup = null;
        try {
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            JAXBContext jaxbContext = JAXBContext.newInstance(GetSecurityGroupResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            securityGroup = (GetSecurityGroupResponse) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_UNMARSHALL_XML_S, xml), e);
        }
        return securityGroup;
    }
    
}
