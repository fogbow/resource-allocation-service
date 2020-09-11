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

    public static Object unmarshal(String xml, Class classType) {
        Object object = null;
        try {
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            JAXBContext jaxbContext = JAXBContext.newInstance(classType);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            object = unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_UNMARSHALL_XML_S, xml), e);
        }
        return object;
    }

}
