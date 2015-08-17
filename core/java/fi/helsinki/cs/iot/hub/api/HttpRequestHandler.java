package fi.helsinki.cs.iot.hub.api;

import java.util.Map;

import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

public abstract class HttpRequestHandler {
	
	public static final String JSON_MIME_TYPE = "application/json";
	public static final String HTML_MIME_TYPE = "text/html; charset=utf-8";
	
	public abstract boolean acceptRequest(Method method, String uri);
	public abstract Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files);
	
	public String getJsonData(Map<String, String> files) {
		System.out.println("Entering HttpRequestHandler.getJsonData");
		if (files == null) {
			System.out.println("Entering HttpRequestHandler.getJsonData, files is null");
			return null;
		}
		for (String s: files.keySet()) {
			System.out.println("HttpRequestHandler.getJsonData:" +s);
		}
		return files.get("postData");
	}
	
	public Response getHtmlResponse(String html) {
		return new NanoHTTPD.Response(Status.OK, HTML_MIME_TYPE, html);
	}

}
