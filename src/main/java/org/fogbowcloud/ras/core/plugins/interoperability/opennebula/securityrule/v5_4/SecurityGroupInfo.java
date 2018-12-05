package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.IP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NAME;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RANGE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE_TYPE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SECURITY_GROUP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SIZE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = SECURITY_GROUP)
public class SecurityGroupInfo {

	private String id;
	private String name;
	private Template template;

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

	public Template getTemplate() {
		return template;
	}

	@XmlElement(name = TEMPLATE)
	public void setTemplate(Template template) {
		this.template = template;
	}
	
	public static SecurityGroupInfo unmarshal(String xml) {
		SecurityGroupInfo securityGroupInfo = null;
		try {
			InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
			JAXBContext jaxbContext = JAXBContext.newInstance(SecurityGroupInfo.class);
	    	Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	    	securityGroupInfo = (SecurityGroupInfo) unmarshaller.unmarshal(inputStream);
	    } catch (JAXBException e) {
	    	// TODO Fix error message...
	    }
	    return securityGroupInfo;
	}

	@XmlRootElement(name = TEMPLATE)
	public static class Template {

		private List<Rule> rules;

		public List<Rule> getRules() {
			return rules;
		}

		@XmlElement(name = RULE)
		public void setRules(List<Rule> rules) {
			this.rules = rules;
		}
	}
}
