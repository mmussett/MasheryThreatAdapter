package com.tibco.mashery.local.ThreatAdapter;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;



public class Utils {

	public static String xmlSanitizer(String in) {
	    StringBuilder out = new StringBuilder();
	    char current;

	    if (in == null || ("".equals(in))) return "";
	    for (int i = 0; i < in.length(); i++) {
	        current = in.charAt(i);
	        if ((current == 0x9) ||
	            (current == 0xA) ||
	            (current == 0xD) ||
	            ((current >= 0x20) && (current <= 0xD7FF)) ||
	            ((current >= 0xE000) && (current <= 0xFFFD)) ||
	            ((current >= 0x10000) && (current <= 0x10FFFF)))
	            out.append(current);
	    }
	    return out.toString();
	  }
	
	public static Map<String, String> getQueryParamsFromUri(String uri)  {
		final Map<String, String> query_pairs = new HashMap<String,String>();
		final String[] pairs = uri.split(Pattern.quote("&"));
		for (String pair : pairs) {
			String[] param = pair.split(Pattern.quote("="));
		    final String key = param[0];
		    final String value = param[1];
		    if (!query_pairs.containsKey(key)) {
		      query_pairs.put(key, value);
		    }

		  }
		  return query_pairs;
		}
	
	public static String getValueFromUri(String uri, int position) {

		String result = null;
		String[] array = uri.split(Pattern.quote("?"));
		array = array[0].split(Pattern.quote("/"));

		if (array.length > position)
			result = array[position];

		return result;
	}

	public static String getQueryParamValueFromUri(String uri, String queryParam) {

		String[] array = uri.split(Pattern.quote("?"));

		if (array.length != 2)
			return null;

		String tmpValue = array[1];

		String[] tmpValueSplit = tmpValue.split(Pattern.quote("&"));
		boolean found = false;
		int count = tmpValueSplit.length;
		int pos = 0;
		String[] param;
		String result = null;

		while (!found && pos < count) {
			param = tmpValueSplit[pos].split(Pattern.quote("="));

			if (param[0].matches(queryParam)) {
				found = true;
				result = param[1];
			}

			if (!found)
				pos++;
		}

		return result;

	}

	public static String stripPathParamFromUri(String uri, int position) {

		String result = null;
		String[] array = uri.split(Pattern.quote("?"));

		String queryParams = null;
		String uriPath = "";

		if (array.length == 1) {
			uriPath = array[0];
			queryParams = null;
		} else {
			uriPath = array[0];
			queryParams = array[1];
		}

		String uriPathSplit[] = uriPath.split(Pattern.quote("/"));
		StringBuffer sb = new StringBuffer();

		int l = uriPathSplit.length;

		for (int i = 0; i < l; i++) {
			if (i == 0) {
				// Handle protocol 'http' or 'https'
				sb.append(uriPathSplit[0]).append("/");
			} else if (i != position) {
				sb.append(uriPathSplit[i]).append("/");
			}
		}

		// Remove trailing slash
		sb.deleteCharAt(sb.length() - 1);

		if (queryParams != null) {
			result = sb.append("?").append(queryParams).toString();
		} else {
			result = sb.toString();
		}

		return result;
	}
	
	public static void main(String[] args) {
		System.out.println(getValueFromUri("http:/getPolicy/1/2?api_key=1234&format=xml",3));
		System.out.println(getQueryParamValueFromUri("http:/getPolicy/1/2", "format"));
		System.out.println(stripPathParamFromUri("http:/getPolicy/1/2", 3));
		
	}

}
