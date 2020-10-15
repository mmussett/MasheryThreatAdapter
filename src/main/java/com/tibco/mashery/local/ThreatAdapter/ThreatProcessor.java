package com.tibco.mashery.local.ThreatAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.json.JsonSanitizer;
import com.mashery.http.MutableHTTPHeaders;
import com.mashery.http.client.HTTPClientRequest;
import com.mashery.http.server.HTTPServerResponse;
import com.mashery.trafficmanager.event.listener.TrafficEventListener;
import com.mashery.trafficmanager.event.model.TrafficEvent;
import com.mashery.trafficmanager.event.processor.model.PostProcessEvent;
import com.mashery.trafficmanager.event.processor.model.PreProcessEvent;
import com.mashery.trafficmanager.model.core.Endpoint;
import com.mashery.trafficmanager.model.core.Processor;
import com.mashery.trafficmanager.model.core.TrafficManagerResponse;
import com.mashery.trafficmanager.processor.ProcessorBean;

@ProcessorBean(enabled = true, name = "com.tibco.mashery.local.ThreatAdapter.ThreatProcessor", immediate = true)
public class ThreatProcessor implements TrafficEventListener {

	private static final Logger log = LoggerFactory.getLogger(ThreatProcessor.class);
	private static Map<String, String> preInputs;
	// private static Map<String, String> postInputs;

	private long preProcessDuration = -1L;

	public void handleEvent(TrafficEvent event) {
		if (log.isDebugEnabled())
			log.debug("In handleEvent method");

		if (event instanceof PreProcessEvent) {
			preProcess((PreProcessEvent) event);
		} else if (event instanceof PostProcessEvent) {
			postProcess((PostProcessEvent) event);
		}

		if (log.isDebugEnabled())
			log.debug("Leaving handleEvent method");

	}

	private void preProcess(PreProcessEvent event) {

		
		if (log.isDebugEnabled())
			log.debug("In preProcess method");

		Endpoint ep = event.getEndpoint();
		Processor processor = ep.getProcessor();
		


		if (processor.isPreProcessingEnabled()) {

			long startTime = System.currentTimeMillis();

			TrafficManagerResponse tmResp = event.getCallContext().getResponse();
			HTTPServerResponse httpResp = tmResp.getHTTPResponse();			
			HTTPClientRequest httpReq = event.getClientRequest();
			MutableHTTPHeaders headers = httpReq.getHeaders();
			

			preInputs = processor.getPreProcessorParameters();
			String checkSqlInjection = preInputs.get("check_sql_injection");
			String checkJsonContent = preInputs.get("check_json_content");
			String checkXmlContnet = preInputs.get("check_xml_content");
			String sanitiseJsonContent = preInputs.get("sanitise_json_content");
			String sanitiseXmlContent = preInputs.get("sanitise_xml_content");			

			// Handle SQL Injection
			if (checkSqlInjection.equalsIgnoreCase("true")) {

				if (log.isDebugEnabled())
					log.debug("Check SQL injection enabled");

				URLCodec codec = new URLCodec();

				try {
					String uri = codec.decode(httpReq.getURI());

					Map<String, String> map = Utils.getQueryParamsFromUri(uri);
					Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
					
					
					while (entries.hasNext()) {
						Map.Entry<String, String> entry = entries.next();
						
						if(log.isDebugEnabled())
							log.debug("Key = " + entry.getKey() + ", Value = " + entry.getValue());

						if (SQLInjectionChecker.detect(entry.getValue()) == true) {

							if (log.isWarnEnabled())
								log.warn("Detected SQL Injection in URI: " + uri);

							httpResp.setStatusCode(400);
							httpResp.setStatusMessage( "SQL Injection detected" );
							event.getCallContext().getResponse().setComplete(true);
							return;
							
						}
					}
				} catch (Exception e) {
					httpResp.setStatusCode(500);
					httpResp.setStatusMessage( e.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				}

			} // end-if-checkSqlInjection==true
			
			if (checkJsonContent.equalsIgnoreCase("true")) {
				if (log.isDebugEnabled())
					log.debug("Check JSON content enabled");
				
				int arrayElementCount = -1;
				int containerDepth = -1;
				int jsonSize = -1;
				int objectEntryCount = -1;
				int objectEntryNameLength = -1;
				int stringValueLength = -1;
				
				String argString = "";
				
				argString = preInputs.get("json_array_element_count");
				try {
					arrayElementCount = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_array_element_count is NaN");
				}

				argString = preInputs.get("json_container_depth");
				try {
					containerDepth = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_container_depth is NaN");
				}
				
				argString = preInputs.get("json_size");
				try {
					jsonSize = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_size is NaN");
				}
				
				argString = preInputs.get("json_object_entry_count");
				try {
					objectEntryCount = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_object_entry_count is NaN");
				}
				
				argString = preInputs.get("json_object_entry_name_length");
				try {
					objectEntryNameLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_object_entry_count is NaN");
				}
				
				argString = preInputs.get("json_string_value_length");
				try {
					stringValueLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument json_string_value_length is NaN");
				}
				
				JsonContentChecker jcc = new JsonContentChecker();
				jcc.setArrayElementCount(arrayElementCount);
				jcc.setContainerDepth(containerDepth);
				jcc.setJsonSize(jsonSize);
				jcc.setObjectEntryCount(objectEntryCount);
				jcc.setObjectEntryNameLength(objectEntryNameLength);
				jcc.setStringValueLength(stringValueLength);
				
			
				try {
					InputStream inputStream = event.getServerRequest().getBody().getInputStream();
					String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
					jcc.checkDocument(content);
				} catch(JsonContentCheckerException jcce) {
					httpResp.setStatusCode(400);
					httpResp.setStatusMessage( jcce.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				} catch (Exception e) {
					httpResp.setStatusCode(500);
					httpResp.setStatusMessage( e.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				} 
				
				
			} // end-if-checkJsonContent==true
			
			
			if (checkXmlContnet.equalsIgnoreCase("true")) {
				if (log.isDebugEnabled())
					log.debug("Check XML content enabled");		
				
				int nameLimitAttribute = -1;
				int nameLimitElement = -1;
				int nameLimitNamespacePrefix = -1;
				int nameLimitProcessingInstructionTarget = -1;
				int structureAttributeCountPerElement = -1;
				int structureChildCount = -1;
				int structureNamespaceCountPerElement = -1;
				int structureNodeDepth = -1;
				int valueAttributeLength = -1;
				int valueCommentLength = -1;
				int valueNamespaceUriLength = -1;
				int valueProcessingInstructionsLength = -1;
				int valueTextLength = -1;
				int xmlSize = -1;
				
				String argString = "";
				
				argString = preInputs.get("xml_name_Limit_attribute");
				try {
					nameLimitAttribute = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_name_Limit_attribute is NaN");
				}

				argString = preInputs.get("xml_name_limit_element");
				try {
					nameLimitElement = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_name_limit_element is NaN");
				}
				
				argString = preInputs.get("xml_name_limit_namespace_prefix");
				try {
					nameLimitNamespacePrefix = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_name_limit_namespace_prefix is NaN");
				}
				
				argString = preInputs.get("xml_name_limit_processing_instruction_target");
				try {
					nameLimitProcessingInstructionTarget = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_name_limit_processing_instruction_target is NaN");
				}
				
				argString = preInputs.get("xml_structure_attribute_count_per_element");
				try {
					structureAttributeCountPerElement = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_structure_attribute_count_per_element is NaN");
				}
				
				argString = preInputs.get("xml_structure_child_count");
				try {
					structureChildCount = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_structure_child_count is NaN");
				}

				argString = preInputs.get("xml_structure_namespace_count_per_element");
				try {
					structureNamespaceCountPerElement = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_structure_namespace_count_per_element is NaN");
				}
				
				argString = preInputs.get("xml_structure_node_depth");
				try {
					structureNodeDepth = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_structure_node_depth is NaN");
				}
				
				argString = preInputs.get("xml_value_attribute_length");
				try {
					valueAttributeLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_value_attribute_length is NaN");
				}
				
				argString = preInputs.get("xml_value_comment_length");
				try {
					valueCommentLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_value_comment_length is NaN");
				}
				
				argString = preInputs.get("xml_value_namespace_uri_length");
				try {
					valueNamespaceUriLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_value_namespace_uri_length is NaN");
				}
				
				argString = preInputs.get("xml_value_processing_instructions_length");
				try {
					valueProcessingInstructionsLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_value_processing_instructions_length is NaN");
				}
				
				argString = preInputs.get("xml_value_text_length");
				try {
					valueTextLength = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_value_text_length is NaN");
				}
				
				argString = preInputs.get("xml_size");
				try {
					xmlSize = Integer.parseInt(argString);
				} catch(NumberFormatException e) {
					log.warn("Pre-processing input argument xml_size is NaN");
				}
				
				
				XmlContentChecker xcc = new XmlContentChecker();
				xcc.setNameLimitAttribute(nameLimitAttribute);
				xcc.setNameLimitElement(nameLimitElement);
				xcc.setNameLimitNamespacePrefix(nameLimitNamespacePrefix);
				xcc.setNameLimitProcessingInstructionTarget(nameLimitProcessingInstructionTarget);
				xcc.setStructureAttributeCountPerElement(structureAttributeCountPerElement);
				xcc.setStructureChildCount(structureChildCount);
				xcc.setStructureNamespaceCountPerElement(structureNamespaceCountPerElement);
				xcc.setStructureNodeDepth(structureNodeDepth);
				xcc.setValueAttributeLength(valueAttributeLength);
				xcc.setValueCommentLength(valueCommentLength);
				xcc.setValueNamespaceUriLength(valueNamespaceUriLength);
				xcc.setValueProcessingInstructionsLength(valueProcessingInstructionsLength);
				xcc.setValueTextLength(valueTextLength);
				xcc.setXmlSize(xmlSize);
				
							
				try {
					InputStream inputStream = event.getServerRequest().getBody().getInputStream();
					String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
					xcc.checkDocument(content);
				} catch(XmlContentCheckerException xcce) {
					httpResp.setStatusCode(400);
					httpResp.setStatusMessage( xcce.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				} catch (Exception e) {
					httpResp.setStatusCode(500);
					httpResp.setStatusMessage( e.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				} 
				
			}

			
			// Sanitise JSON content before sending
			if (sanitiseJsonContent.equalsIgnoreCase("true")) {

				if (log.isDebugEnabled())
					log.debug("Sanitise JSON content enabled");


				try {
					InputStream inputStream = event.getServerRequest().getBody().getInputStream();
					String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
					
					String sanitisedContent = JsonSanitizer.sanitize(content);
					
					// Make sure content-length is correct
					headers.remove("Content-Length");
					headers.add("Content-Length", Long.toString(sanitisedContent.length()));

					// Set Body with JSON string
					httpReq.setBody(new StringContentProducer(sanitisedContent));
					
					
				} catch (Exception e) {
					httpResp.setStatusCode(500);
					httpResp.setStatusMessage( e.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				}

				
			}
			
			// Sanitise XML content before sending
			if (sanitiseXmlContent.equalsIgnoreCase("true")) {

				if (log.isDebugEnabled())
					log.debug("Sanitise XML content enabled");


				try {
					InputStream inputStream = event.getServerRequest().getBody().getInputStream();
					String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
					
					String sanitisedContent = Utils.xmlSanitizer(content);
					
					// Make sure content-length is correct
					headers.remove("Content-Length");
					headers.add("Content-Length", Long.toString(sanitisedContent.length()));

					// Set Body with JSON string
					httpReq.setBody(new StringContentProducer(sanitisedContent));
					
					
				} catch (Exception e) {
					httpResp.setStatusCode(500);
					httpResp.setStatusMessage( e.getMessage() );
					event.getCallContext().getResponse().setComplete(true);
					return;
				}

				
			}
			
			headers.add("X-Mashery-Processed-By", ThreatProcessor.class.getName());

			long endTime = System.currentTimeMillis();
			this.preProcessDuration = endTime - startTime;

		} else {

			if (log.isDebugEnabled())
				log.debug("PreProcessing is disabled");

		}

		if (log.isDebugEnabled())
			log.debug("Leaving preProcess method");

	}

	private void postProcess(PostProcessEvent event) {

		if (log.isDebugEnabled())
			log.debug("In postProcess method");

		Endpoint ep = event.getEndpoint();
		Processor processor = ep.getProcessor();

		if (processor.isPostProcessingEnabled()) {
			event.getServerResponse().getHeaders().add("X-Mashery-ThreatProcessor-Pre-Duration",
					Long.toString(this.preProcessDuration));
		}

		if (log.isDebugEnabled())
			log.debug("Leaving postProcess method");

	}

}
