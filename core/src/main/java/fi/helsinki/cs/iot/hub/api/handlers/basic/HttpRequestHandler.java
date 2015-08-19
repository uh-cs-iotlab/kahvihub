package fi.helsinki.cs.iot.hub.api.handlers.basic;

import java.io.File;
import java.util.Map;

import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

public abstract class HttpRequestHandler {
	
	public static final String JSON_MIME_TYPE = "application/json";
	public static final String HTML_MIME_TYPE = "text/html; charset=utf-8";
	public static final String TAG = "HttpRequestHandler";
	
	public abstract boolean acceptRequest(Method method, String uri);
	public abstract Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files);
	
	public String getJsonData(Method method, Map<String, String> files) {
		if (files == null) {
			return null;
		}
		switch (method) {
		case POST:
			return files.get("postData");
		case PUT:
			File file = new File(files.get("content"));
			if (file.exists()) {
				return ScriptUtils.convertFileToString(file);
			}
		default:
			return null;
		}
		
	}
	
	public Response getHtmlResponse(String html) {
		return new NanoHTTPD.Response(Status.OK, HTML_MIME_TYPE, html);
	}

}
