package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OpenNebulaUnmarshallerContents {

	private final static Logger LOGGER = Logger.getLogger(OpenNebulaUnmarshallerContents.class);
	
	private Document document;
	
	public OpenNebulaUnmarshallerContents(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource source = new InputSource(new StringReader(xml));
			this.document = builder.parse(source);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.error(e); // TODO Fix error message...
		}
	}

	public String unmarshalLastItemOf(String tag) {
		NodeList list = this.document.getElementsByTagName(tag);
		String value = null;
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			value = child.getTextContent();
		}
		return value;
	}
}
