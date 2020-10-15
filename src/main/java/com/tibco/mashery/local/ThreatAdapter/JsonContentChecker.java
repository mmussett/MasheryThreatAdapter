package com.tibco.mashery.local.ThreatAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonContentChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonContentChecker.class);

	private JsonParser jp;
	private JsonFactory jf;

	// Specifies the maximum number of entries allowed in an object.
	private int objectEntryCount = -1;

	// Specifies the maximum string length allowed for a property name within an
	// object.
	private int objectEntryNameLength = -1;

	// Specifies the maximum length allowed for a string value.
	private int stringValueLength = -1;

	// Specifies the maximum allowed containment depth, where the containers are
	// objects or arrays. For example, an array containing an object which
	// contains an object would result in a containment depth of 3.
	private int containerDepth = -1;

	// Specifies the maximum number of elements allowed in an array.
	private int arrayElementCount = -1;
	
	// Specifies the maximum size of json
	private int jsonSize = -1;
	
	public JsonContentChecker() {
	}

	public void setObjectEntryCount(int objectEntryCount) {
		this.objectEntryCount = objectEntryCount;
	}

	public void setObjectEntryNameLength(int objectEntryNameLength) {
		this.objectEntryNameLength = objectEntryNameLength;
	}

	public void setContainerDepth(int containerDepth) {
		this.containerDepth = containerDepth;
	}

	public void setArrayElementCount(int arrayElementCount) {
		this.arrayElementCount = arrayElementCount;
	}

	public void setStringValueLength(int stringValueLength) {
		this.stringValueLength = stringValueLength;
	}

	public void setJsonSize(int jsonSize) {
		this.jsonSize = jsonSize;
	}
	
	// returns objectEntryCount
	private int handleStartObject(int currentDepth) throws IOException, JsonContentCheckerException {

		int objectCount = 0;

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering handleStartObject");

		if (this.containerDepth != -1) {
			if (currentDepth > this.containerDepth) {
				String msg = "Container depth exceeded, max depth [" + this.containerDepth+"] got ["+currentDepth+"]";
				LOGGER.warn(msg);
				throw new JsonContentCheckerException(msg);
			}
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {

			JsonToken token = jp.currentToken();
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Token is ["+token.name()+"]");

			if (token == JsonToken.START_ARRAY) {
				int result = handleStartArray(currentDepth + 1);
				if (this.arrayElementCount != -1) {
					if (result > this.arrayElementCount) {
						String msg = "Array element count exceeded, max count [" + this.arrayElementCount+"] got ["+result+"]";
						LOGGER.warn(msg);
						throw new JsonContentCheckerException(msg);
					}
				}
			}

			if (token == JsonToken.START_OBJECT) {
				int result = handleStartObject(currentDepth + 1);

				if (this.objectEntryCount != -1) {
					if (result > this.objectEntryCount) {
						String msg = "Object entry count exceeded, max count [" + this.objectEntryCount+"] got ["+result+"]";
						LOGGER.warn(msg);
						throw new JsonContentCheckerException(msg);
					}
				}
			}

			if (token == JsonToken.FIELD_NAME) {
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Procesing Json Field [" + jp.getCurrentName() + "]");

				// Check field length > objectEntryNameLength
				if (this.objectEntryNameLength != -1) {
					int len = jp.getCurrentName().length();
					if (len > this.objectEntryNameLength) {
						String msg = "Object entry name length exceeded, max length [" + this.objectEntryNameLength+"] got ["+len+"]";
						LOGGER.warn(msg);
						throw new JsonContentCheckerException(msg);
					}
				}

				// Increment the field count
				objectCount++;
			}

			if (token == JsonToken.VALUE_STRING) {

				// Check to see length > stringValueLength
				if (this.stringValueLength != -1) {
					int len = jp.getValueAsString().length();
					if (len > this.stringValueLength) {
						String msg = "String value length exceeded, max length [" + this.stringValueLength+"] got ["+len+"]";
						LOGGER.warn(msg);
						throw new JsonContentCheckerException(msg);
					}
				}
			}
		}

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Exiting handleStartObject with object count ["+objectCount+"]");

		return objectCount;

	}

	private int handleStartArray(int currentDepth) throws IOException, JsonContentCheckerException {

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering handleStartArray");

		if (this.containerDepth != -1) {
			if (currentDepth > this.containerDepth) {
				String msg = "Container depth exceeded, max depth [" + this.containerDepth+"] got ["+currentDepth+"]";
				LOGGER.warn(msg);
				throw new JsonContentCheckerException(msg);
			}
		}

		int objectCount = 0;

		while (jp.nextToken() != JsonToken.END_ARRAY) {

			JsonToken token = jp.currentToken();
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Token is ["+token.name()+"]");

			if (token == JsonToken.START_OBJECT) {
				int result = handleStartObject(currentDepth + 1);

				if (this.objectEntryCount != -1) {
					if (result >= this.objectEntryCount) {
						String msg = "Object entry count exceeded, max count [" + this.objectEntryCount+"] got ["+result+"]";
						LOGGER.warn(msg);
						throw new JsonContentCheckerException(msg);
					}
				}

				objectCount++;
			}

		}

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Exiting handleStartArray with object count ["+objectCount+"]");

		return objectCount;
	}

	public boolean checkDocument(String jsonString) throws JsonContentCheckerException {

		long begin, current;
		begin = System.currentTimeMillis();
		
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Entering check");
		
		if(this.jsonSize != -1) {
			int len = jsonString.length();
			if(len > this.jsonSize)  {
				String msg = "Json content size exceeded, max size ["+this.jsonSize+"] got ["+len+"]";
				LOGGER.warn(msg);
				throw new JsonContentCheckerException(msg);
			}
		}
		

		jf = new JsonFactory();

		try {
			jp = jf.createParser(jsonString);

			while (jp.nextToken() != null) {

				JsonToken token = jp.currentToken();
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Token is ["+token.name()+"]");
				
				if (token == JsonToken.START_OBJECT) {					
					int result = handleStartObject(0);
					if (this.objectEntryCount != -1) {
						if (result > this.objectEntryCount) {
							String msg = "Object entry count exceeded, max count [" + this.objectEntryCount+"] got ["+result+"]";
							LOGGER.warn(msg);
							throw new JsonContentCheckerException(msg);
						}
					}

				}

				if (token == JsonToken.START_ARRAY) {
					int result = handleStartArray(0);
					if (this.arrayElementCount != -1) {
						if (result > this.arrayElementCount) {
							String msg = "Array element count exceeded, max count [" + this.arrayElementCount+"] got ["+result+"]";
							LOGGER.warn(msg);
							throw new JsonContentCheckerException(msg);
						}
					}
				}

			}
			
			jp.close();

		} catch (JsonParseException jpe) {
			LOGGER.error(jpe.getMessage());
			throw new JsonContentCheckerException(jpe.getMessage());
		} catch (IOException ioe) {
			LOGGER.error(ioe.getMessage());
			throw new JsonContentCheckerException(ioe.getMessage());
		} finally {
			current = System.currentTimeMillis();
	        LOGGER.debug("Validation performed in [" + (current - begin) + "] ms");				
		}

		return true;

	}

	public static void main(String args[]) {

		JsonContentChecker jcc = new JsonContentChecker();
		try {

			String content = new String(java.nio.file.Files.readAllBytes(
					java.nio.file.Paths.get("/Users/markmussett/src/ml/adapters/ThreatAdapter/Samples/test4.json")));


			jcc.setJsonSize(67694);
			jcc.setArrayElementCount(256);
			jcc.setStringValueLength(1024);
			jcc.setContainerDepth(10);
			for(int i=0;i<50;i++) {
				jcc.checkDocument(content);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}




}
