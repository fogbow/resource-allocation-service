package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.constants.Messages;

public class OpenNebulaUnmarshaller {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaUnmarshaller.class);
	
	public static UnmarshallerResponse unmarshal(String xml) {
		UnmarshallerResponse response = null;
		try {
			InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
			JAXBContext jaxbContext = JAXBContext.newInstance(UnmarshallerResponse.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			response = (UnmarshallerResponse) unmarshaller.unmarshal(inputStream);
		} catch (JAXBException e) {
			LOGGER.error(String.format(Messages.Error.UNABLE_TO_UNMARSHALL_XML_S, xml), e);
		}
		return response;
	}
	
	public interface UnmarshallerResponse {}

}
