package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
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

	public String getContentOfLastElement(String tag) {
		NodeList list = this.document.getElementsByTagName(tag);
		int lastIndex = list.getLength()-1;
		String lastElement = list.item(lastIndex).getTextContent();
		return lastElement;
	}

	public boolean containsExpressionContext(String expression, String content) {
		NodeList nodes;
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			nodes = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
			String itemContent = nodes.item(0).getFirstChild().getNextSibling().getTextContent();
			if (content.equals(itemContent)) {
				return true;
			}
		} catch (XPathExpressionException e) {
			LOGGER.error(e); // TODO Fix error message...
		}
		return false;
	}
}
