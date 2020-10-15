package com.tibco.mashery.local.ThreatAdapter;

import net.sf.saxon.lib.FeatureKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * Translation utility class to handle SOAP to XML/JSON
 * 
 * @author Mark Mussett (mmussett@tibco.com)
 * @version 1.0
 */
public class TranslationUtils {

	// Embedded XSLT used by stripNamespace
	private final static String xslt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"node()\"><xsl:copy><xsl:apply-templates select=\"node()|@*\" /></xsl:copy></xsl:template><xsl:template match=\"*\"><xsl:element name=\"{local-name()}\"><xsl:apply-templates select=\"node()|@*\" /></xsl:element></xsl:template><xsl:template match=\"@*\"><xsl:attribute name=\"{local-name()}\"><xsl:apply-templates select=\"node()|@*\" /></xsl:attribute></xsl:template></xsl:stylesheet>";
	private static final Logger log = LoggerFactory.getLogger(TranslationUtils.class);
	
	/**
	 * Strips all XML Name-space declarations from an XML document using XSLT
	 * processing and returns XML string
	 * 
	 * @param doc
	 *            Document to strip XML namespaces from.
	 * @return XML String devoid of any namespaces.
	 */
	

	private static String stripNamespace(Document doc) throws Exception {
		TransformerFactory tFactory = TransformerFactory.newInstance();
		tFactory.setAttribute(FeatureKeys.RECOVERY_POLICY_NAME, "recoverSilently");
		String result = null;
		try {

			final StreamSource streamSource = new StreamSource();
			streamSource.setInputStream(new ByteArrayInputStream(xslt.getBytes()));

			Transformer transformer = tFactory.newTransformer(streamSource);


			StreamResult streamResult = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, streamResult);
			result = streamResult.getWriter().toString();

		} catch (Exception e) {
			log.error(e.getMessage());
			new Exception("Failed to strip namespace from document.");
		}
		return result;
	}

	/**
	 * Converts an XML string to JSON string
	 * 
	 * @param xmlString
	 *            to convert
	 * @return JSON string representation
	 */
	public static String xmlToJson(String xmlString) {

		return org.json.XML.toJSONObject(xmlString).toString();

	}

	/**
	 * Serialises a Document to a String
	 * 
	 * @param doc
	 *            Document to be serialised
	 * @return a String representation of the document
	 */
	private static String DocumentToString(Document doc) throws Exception {

		String result = null;

		TransformerFactory tfactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tfactory.newTransformer();
			StreamResult streamResult = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, streamResult);
			result = streamResult.getWriter().toString();
		} catch (Exception e) {
			log.error(e.getMessage());
			new Exception("Failed to convert document to string.");
		}

		return result;
	}

	/**
	 * Serialises a XML String to a Document
	 * 
	 * @param xmlString
	 *            to be serialised
	 * @return a Document representation of the XML string
	 */
	private static Document StringtoDocument(String xmlString) throws Exception {

		TransformerFactory tfactory = TransformerFactory.newInstance();

		Document result = null;

		if (tfactory.getFeature(DOMSource.FEATURE)) {

			try {
				DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

				InputStream is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));

				dfactory.setNamespaceAware(false);

				DocumentBuilder docBuilder = dfactory.newDocumentBuilder();

				result = docBuilder.parse(is);
			} catch (Exception e) {
				log.error(e.getMessage());
				throw new Exception("Failed to convert xml string to document.");
			}

		} else {
			// We should never get here. If we do then the world ends!
			throw new Exception("DOM node processing not supported!");
		}
		return result;

	}

	/**
	 * Extracts a sub-element from a Document and returns it as a new Document
	 * 
	 * @param doc
	 *            Source Document containing element wishing to extract
	 * @param elementName
	 *            is the XML element tag name within the XML document to extract
	 * @return a Document containing elementName as the root element
	 * @throws Exception
	 */
	private static Document extractFromDocument(Document doc, String elementName) throws Exception {

		TransformerFactory tfactory = TransformerFactory.newInstance();

		Document result = null;

		if (tfactory.getFeature(DOMSource.FEATURE)) {

			DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

			result = dfactory.newDocumentBuilder().newDocument();
			NodeList nodeList = doc.getElementsByTagName(elementName);
			if (nodeList != null && nodeList.getLength() > 0) {
				Element element = (Element) nodeList.item(0);
				if (element != null) {
					Node copiedNode = result.importNode(element, true);
					result.appendChild(copiedNode);
				} else {
					log.error("Failed to extract element "+elementName+" from nodelist");
					throw new Exception("Failed to extract element from nodelist.");
				}
			} else {
				log.error("Failed to extract element "+elementName+" from nodelist");
				throw new Exception("Failed to get element by name.");
			}

		} else {
			// We should never get here. If we do then the world ends!
			throw new Exception("DOM node processing not supported!");
		}
		return result;

	}

	/**
	 * Extracts sub-document from SOAP message and returns it as a JSON string
	 * representation
	 * 
	 * @param soapMessage
	 *            XML String representation of a valid SOAP document
	 * @param elementName
	 *            is the XML element tag name within the XML document to extract
	 * @return a JSON String representation
	 * @throws Exception
	 */
	public static String extractSoapToJson(String soapMessage, String elementName, boolean stripNamespaces) throws Exception {

		String result = null;

		Document origDoc = StringtoDocument(soapMessage);
		Document newDoc = extractFromDocument(origDoc, elementName);
		if (stripNamespaces) {
			String strippedXml = stripNamespace(newDoc);
			result = xmlToJson(strippedXml);
		} else {
			result = xmlToJson(DocumentToString(newDoc));	
		}
		return result;

	}

	/**
	 * Extracts sub-document from SOAP message and returns it as XML string
	 * representation
	 * 
	 * @param soapMessage
	 *            XML String representation of a valid SOAP document
	 * @param elementName
	 *            is the XML element tag name within the XML document to extract
	 * @return an XML String representation
	 * @throws Exception
	 */
	public static String extractSoapToXml(String soapMessage, String elementName, boolean stripNamespaces) throws Exception {

		String result = null;

		Document origDoc = StringtoDocument(soapMessage);
		Document newDoc = extractFromDocument(origDoc, elementName);
		if (stripNamespaces) {
			result = stripNamespace(newDoc);
			
		} else {
			result = DocumentToString(newDoc);
		}

		return result;

	}

}
