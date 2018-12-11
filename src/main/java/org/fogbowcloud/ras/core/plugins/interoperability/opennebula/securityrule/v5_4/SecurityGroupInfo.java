package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.ID;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NAME;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.RULE;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.SECURITY_GROUP;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

@XmlRootElement(name = SECURITY_GROUP)
public class SecurityGroupInfo {

	public static final Logger LOGGER = Logger.getLogger(SecurityGroupInfo.class);
	
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
	    	LOGGER.warn("Is not possible unmarshal", e);
			LOGGER.debug(String.format("Is not possible unmarshal %s", xml));
	    }
	    return securityGroupInfo;
	}

	public String marshalTemplate() {
		StringWriter writer = new StringWriter();
		String xml = null;
		try {
			JAXBContext context = JAXBContext.newInstance(this.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(this, writer);
			xml = writer.toString();
		} catch (JAXBException e) {
			LOGGER.error("Is not possible marshal the template", e);
		}
		return xml;
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
