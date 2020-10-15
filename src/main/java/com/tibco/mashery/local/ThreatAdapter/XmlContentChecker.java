package com.tibco.mashery.local.ThreatAdapter;

import org.slf4j.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.LoggerFactory;

import net.sf.saxon.expr.LastItemExpression;

import java.util.Stack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class XmlContentChecker {

	private InputStream is;
	private XMLInputFactory factory;
	private XMLStreamReader reader;

	private int currentDepth = 0;
	private int lastDepth = 0;
	private int elementSiblingCount = 0;

	private static final Logger LOGGER = LoggerFactory.getLogger(XmlContentChecker.class);

	/**
	 * Specifies a limit on the maximum number of characters permitted in any element name in the XML document.
	 */
	private int nameLimitElement = -1;

	/**
	 * Specifies a limit on the maximum number of characters permitted in any attribute name in the XML document.
	 */
	private int nameLimitAttribute = -1;

	/**
	 * Specifies a limit on the maximum number of characters permitted in the namespace prefix in the XML document.
	 */
	private int nameLimitNamespacePrefix = -1;

	/**
	 * Specifies a limit on the maximum number of characters permitted in the target of any processing instructions in the XML document.
	 */
	private int nameLimitProcessingInstructionTarget = -1;

	/**
	 * Specifies the maximum node depth allowed in the XML.
	 */
	private int structureNodeDepth = -1;

	/**
	 * Specifies the maximum number of attributes allowed for any element.
	 */
	private int structureAttributeCountPerElement = -1;

	/**
	 * Specifies the maximum number of namespace definitions allowed for any element.
	 */
	private int structureNamespaceCountPerElement = -1;

	/**
	 * Specifies the maximum number of child elements allowed for any element.
	 */
	private int structureChildCount = -1;

	/**
	 * Specifies a character limit for any text nodes present in the XML document.
	 */
	private int valueTextLength = -1;

	/**
	 * Specifies a character limit for any attribute values present in the XML document.
	 */
	private int valueAttributeLength = -1;

	/**
	 * Specifies a character limit for any namespace URIs present in the XML document.
	 */
	private int valueNamespaceUriLength = -1;

	/**
	 * Specifies a character limit for any comments present in the XML document.
	 */
	private int valueCommentLength = -1;

	/**
	 * Specifies a character limit for any processing instruction text present in the XML document.
	 */
	private int valueProcessingInstructionsLength = -1;

	/**
	 * Specifies the maximum size of xml
	 */
	private int xmlSize = -1;

	public XmlContentChecker() {
	}

	public void setNameLimitElement(int nameLimitElement) {
		this.nameLimitElement = nameLimitElement;
	}

	public void setNameLimitAttribute(int nameLimitAttribute) {
		this.nameLimitAttribute = nameLimitAttribute;
	}

	public void setNameLimitNamespacePrefix(int nameLimitNamespacePrefix) {
		this.nameLimitNamespacePrefix = nameLimitNamespacePrefix;
	}

	public void setNameLimitProcessingInstructionTarget(int nameLimitProcessingInstructionTarget) {
		this.nameLimitProcessingInstructionTarget = nameLimitProcessingInstructionTarget;
	}

	public void setStructureNodeDepth(int structureNodeDepth) {
		this.structureNodeDepth = structureNodeDepth;
	}

	public void setStructureAttributeCountPerElement(int structureAttributeCountPerElement) {
		this.structureAttributeCountPerElement = structureAttributeCountPerElement;
	}

	public void setStructureNamespaceCountPerElement(int structureNamespaceCountPerElement) {
		this.structureNamespaceCountPerElement = structureNamespaceCountPerElement;
	}

	public void setStructureChildCount(int structureChildCount) {
		this.structureChildCount = structureChildCount;
	}

	public void setValueTextLength(int valueTextLength) {
		this.valueTextLength = valueTextLength;
	}

	public void setValueAttributeLength(int valueAttributeLength) {
		this.valueAttributeLength = valueAttributeLength;
	}

	public void setValueNamespaceUriLength(int valueNamespaceUriLength) {
		this.valueNamespaceUriLength = valueNamespaceUriLength;
	}

	public void setValueCommentLength(int valueCommentLength) {
		this.valueCommentLength = valueCommentLength;
	}

	public void setValueProcessingInstructionsLength(int valueProcessingInstructionsLength) {
		this.valueProcessingInstructionsLength = valueProcessingInstructionsLength;
	}

	public void setXmlSize(int xmlSize) {
		this.xmlSize = xmlSize;
	}

	@SuppressWarnings("restriction")
	private void checkComment() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkComment");

		if (this.valueCommentLength != -1) {
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("Checking valueCommentLength");
			int result = reader.getText().trim().length();
			if (result > this.valueCommentLength) {
				String msg = "Comment text length exceeded, max length [" + this.valueCommentLength + "] got [" + result
						+ "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkComment");

	}

	@SuppressWarnings("restriction")
	private void checkAttribute() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkAttribute");

		// Check attribute count
		if (this.structureAttributeCountPerElement != -1) {
			int count = reader.getAttributeCount();
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Attribute count [" + count + "]");

			int result = reader.getAttributeCount();
			if (result > this.structureAttributeCountPerElement) {
				String msg = "Attribute count exceeded, max count [" + this.structureAttributeCountPerElement
						+ "] got [" + result + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}

		if (this.nameLimitAttribute != -1 || this.valueAttributeLength != -1) {
			int count = reader.getAttributeCount();
			if (count > 0) {
				for (int i = 0; i < count; i++) {

					if (nameLimitAttribute != -1) {
						
						if(LOGGER.isTraceEnabled())
							LOGGER.trace("Checking nameLimitAttribute");
						
						QName attributeName = reader.getAttributeName(i);
						String attrLocalName = attributeName.getLocalPart();
						int lenAttrLocalName = attrLocalName.length();

						if (LOGGER.isTraceEnabled())
							LOGGER.trace("ATTRIBUTE [" + attrLocalName + "]");
						
						if (lenAttrLocalName > this.nameLimitAttribute) {
							String msg = "Attribute name limit length exceeded, max length [" + this.nameLimitAttribute
									+ "] got [" + lenAttrLocalName + "]";
							LOGGER.warn(msg);
							throw new XmlContentCheckerException(msg);
						}
					}

					if (this.valueAttributeLength != -1) {
						
						if(LOGGER.isTraceEnabled())
							LOGGER.trace("Checking valueAttributeLength");
						
						String attrValue = reader.getAttributeValue(i);
						if (LOGGER.isTraceEnabled())
							LOGGER.trace("Attribute value ["+attrValue+"]");	
						
						int lenAttrValue = attrValue.length();
						if (lenAttrValue > this.valueAttributeLength) {
							String msg = "Attribute value limit length exceeded, max length ["
									+ this.valueAttributeLength + "] got [" + lenAttrValue + "]";
							LOGGER.warn(msg);
							throw new XmlContentCheckerException(msg);
						}
					}



				}
			}
		}
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkAttribute");

	}

	@SuppressWarnings("restriction")
	private void checkProcessingInstructions() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkProcessingInstructions");

		if (nameLimitProcessingInstructionTarget != -1) {
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("Checking nameLimitProcessingInstructionTarget");
			
			int result = reader.getPITarget().length();
			if (result > nameLimitProcessingInstructionTarget) {
				String msg = "Processing Instructions target limit length exceeded, max length ["
						+ this.nameLimitProcessingInstructionTarget + "] got [" + result + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}

		if (valueProcessingInstructionsLength != -1) {
			if(LOGGER.isTraceEnabled())			
				LOGGER.trace("Checking valueProcessingInstructionsLength");
			
			int result = reader.getPIData().length();
			if (result > valueProcessingInstructionsLength) {
				String msg = "Processing Instructions limit length exceeded, max length ["
						+ this.valueProcessingInstructionsLength + "] got [" + result + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkProcessingInstructions");
	}

	@SuppressWarnings("restriction")
	private void checkNamespace() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkNamespace");

		for (int nsIndex = 0; nsIndex < reader.getNamespaceCount(); nsIndex++) {
			if (this.nameLimitNamespacePrefix != -1) {
				String prefix = reader.getNamespacePrefix(nsIndex);
				
				if(LOGGER.isTraceEnabled())
					LOGGER.trace("Namespace Prefix ["+prefix+"]");
				
				if (prefix != null) {
					int result = prefix.length();
					if (result > this.nameLimitNamespacePrefix) {
						String msg = "Namespace prefix limit length exceeded, max length ["
								+ this.nameLimitNamespacePrefix + "] got [" + result + "]";
						LOGGER.warn(msg);
						throw new XmlContentCheckerException(msg);
					}
				}
			}

			if (this.valueNamespaceUriLength != -1) {
				String namespaceUri = reader.getNamespaceURI(nsIndex);
				if (namespaceUri != null) {
					
					if(LOGGER.isTraceEnabled())
						LOGGER.trace("Namespace ["+namespaceUri+"]");
					
					int result = namespaceUri.length();
					if (result > this.valueNamespaceUriLength) {
						String msg = "Namespace uri length exceeded, max length [" + this.valueNamespaceUriLength
								+ "] got [" + result + "]";
						LOGGER.warn(msg);
						throw new XmlContentCheckerException(msg);
					}
				}
			}
		}
		

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkNamespace");

	}

	@SuppressWarnings("restriction")
	private void checkCharacters() throws XmlContentCheckerException {
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkCharacters");

		if (this.valueTextLength != -1) {
			LOGGER.trace("Checking valueTextLength");
			int result = reader.getText().trim().length();
			if (result > this.valueTextLength) {
				String msg = "Value text length exceeded, max length [" + this.valueTextLength + "] got [" + result
						+ "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkCharacters");

	}

	@SuppressWarnings("restriction")
	private void checkElement() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkElement");

		// Check length of element
		if (this.nameLimitElement != -1) {
			LOGGER.trace("Checking nameLimitElement");
			int result = reader.getLocalName().length();
			if (result > this.nameLimitElement) {
				String msg = "Element name limit length exceeded, max length [" + this.nameLimitElement + "] got ["
						+ result + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}

		}

		if (this.structureNamespaceCountPerElement != -1) {
			LOGGER.trace("Checking structureNamespaceCountPerElement");
			int result = reader.getNamespaceCount();
			if (result > this.structureNamespaceCountPerElement) {
				String msg = "Namespace count exceeded, max count [" + this.structureNamespaceCountPerElement
						+ "] got [" + result + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkElement");

	}

	private boolean handleStartElement() throws XmlContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering handleStartElement");


		// Check to see if we've exceeded node depth
		if (this.structureNodeDepth != -1) {
			if (currentDepth > this.structureNodeDepth) {
				String msg = "Container depth exceeded, max depth [" + this.structureNodeDepth + "] got ["
						+ currentDepth + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}

		// Check to see if we've exceeded sibling count
		if (this.structureChildCount != -1) {
			if (elementSiblingCount > this.structureChildCount) {
				String msg = "Element sibling count exceeded, max depth [" + this.structureChildCount + "] got ["
						+ elementSiblingCount + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}

		checkNamespace();
		checkElement();

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving handleStartElement");
		return true;
	}

	@SuppressWarnings("restriction")
	public boolean checkDocument(String xmlString) throws XmlContentCheckerException {

		int startCount = 0;
		int endCount = 0;
		
		int lastIterationCurrentDepth = 0;
		int lastIterationLastDepth = 0;
		
		int dirIndicator = 0;

		String currentEndTerm = "";
		String lastEndTerm = "";
		
		String currentStartTerm = "";
		String lastStartTerm = "";
		
		int currentEvent = 0;
		int lastEvent = -1;
		
        Stack<Integer> stack = new Stack<Integer>();
        
		long begin, current;
		begin = System.currentTimeMillis();
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering checkDocument");

		if (this.xmlSize != -1) {
			int len = xmlString.length();
			if (len > this.xmlSize) {
				String msg = "Xml content size exceeded, max size [" + this.xmlSize + "] got [" + len + "]";
				LOGGER.warn(msg);
				throw new XmlContentCheckerException(msg);
			}
		}

		is = new ByteArrayInputStream(xmlString.getBytes());
		factory = XMLInputFactory.newInstance();

		currentDepth = 0;
		lastDepth = 0;
		elementSiblingCount = 0;		
		
		stack.push(0);

		try {
			reader = factory.createXMLStreamReader(is);

			while (reader.hasNext()) {
				
				LOGGER.trace("Processing next event...");
				
				
				int event = reader.next();
				
				switch (event) {

				case XMLStreamConstants.NAMESPACE:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [NAMESPACE]");
					checkNamespace();
					break;

				case XMLStreamConstants.COMMENT:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [COMMENT]");
					checkComment();
					break;

				case XMLStreamConstants.SPACE:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [START]");
					break;

				case XMLStreamConstants.START_ELEMENT: {
					stack.push(elementSiblingCount);					
					String localName = reader.getLocalName();	

					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [START_ELEMENT] name [" + localName + "]");

					
					startCount++;
					lastDepth = currentDepth;
					currentDepth = startCount - endCount;
					
					LOGGER.debug(currentDepth+" "+lastIterationCurrentDepth);
					
					// Now=START Last=START
					if(lastEvent==XMLStreamConstants.START_ELEMENT) {
						elementSiblingCount=0;
					} else {
						// Now=START Last=END
						elementSiblingCount++;
					}
					
					
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Current Depth [" + currentDepth + "] Last Depth [" + lastDepth + "]");
				
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Sibling Count ["+ elementSiblingCount + "]");
					
					currentStartTerm = localName;
					


					handleStartElement();

					int attrCount = reader.getAttributeCount();
					// Do we have any attributes to take care of
					if (attrCount > 0) {
						checkAttribute();
					}
					
					lastEvent = event;

				}
					break;

				case XMLStreamConstants.END_ELEMENT: {
					elementSiblingCount=stack.pop();
					
					String localName = reader.getLocalName();
					if (LOGGER.isTraceEnabled()) 
						LOGGER.trace("Got Event [END_ELEMENT] name [" + localName + "]");
					
					
					endCount++;

					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Current Depth [" + currentDepth + "] Last Depth [" + lastDepth + "]");


					if(lastEvent==XMLStreamConstants.END_ELEMENT) {
						// Now=END + Last=END
						elementSiblingCount=0;
					} else {
						// Now=END + Last=START
						elementSiblingCount++;					
					}
					
				

					
					
		
					
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Sibling Count ["+ elementSiblingCount + "]");

					lastEvent = event;
					
					
				}
					break;

				case XMLStreamConstants.START_DOCUMENT:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [START_DOCUMENT]");
					break;

				case XMLStreamConstants.ENTITY_REFERENCE:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [ENTITY_REFERENCE]");
					if (reader.hasText())
						if (LOGGER.isTraceEnabled())
							LOGGER.trace(reader.getText());
					break;

				case XMLStreamConstants.ATTRIBUTE:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [ATTRIBUTE]");
					break;

				case XMLStreamConstants.CHARACTERS:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [CHARACTERS]");
					checkCharacters();
					break;

				case XMLStreamConstants.END_DOCUMENT:
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got Event [END_DOCUMENT]");	
					
					break;
				default:
					
					if (LOGGER.isTraceEnabled())
						LOGGER.trace("Got unhandled Event ["+event+"]");						
					break;

				}
				
				lastIterationCurrentDepth = currentDepth;
				lastIterationLastDepth = lastDepth;
				
				
				LOGGER.trace("Finished processing this event");

			}

			reader.close();

		} catch (XMLStreamException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			current = System.currentTimeMillis();
	        LOGGER.debug("Validation performed in [" + (current - begin) + "] ms");			
		}

        
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Leaving checkDocument");
		
		return true;
	}

	public static void main(String args[]) {

		XmlContentChecker xcc = new XmlContentChecker();
		try {

			String content = new String(java.nio.file.Files.readAllBytes(
					java.nio.file.Paths.get("/Users/markmussett/src/ml/adapters/ThreatAdapter/Samples/test3.xml")));

			xcc.setXmlSize(74704);
			xcc.setValueNamespaceUriLength(50);
			xcc.setStructureChildCount(2000);
			xcc.setStructureNodeDepth(20000);

		
			xcc.checkDocument(content);
			

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
