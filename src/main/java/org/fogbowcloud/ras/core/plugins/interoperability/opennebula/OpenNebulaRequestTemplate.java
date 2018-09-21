package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

public class OpenNebulaRequestTemplate {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaRequestTemplate.class);
	
	public String generateTemplate() {
		StringWriter writer = new StringWriter();
		String xml = null;
	    try {
	        JAXBContext context = JAXBContext.newInstance(this.getClass());
	        Marshaller marshaller = context.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	        marshaller.marshal(this, writer);
	        xml = writer.toString();
	    } catch (JAXBException e) {
	        LOGGER.error(e); // TODO Fix error message...
	    }
	    return xml;
	}
}
