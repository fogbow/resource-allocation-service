package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

public class OpenNebulaMarshallerTemplate {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaMarshallerTemplate.class);
	
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
	        LOGGER.error(e); // TODO Fix error message...
	    }
	    return xml;
	}
}
