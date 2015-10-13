package fi.helsinki.cs.iot.hub.api.handlers.basic;

import java.util.LinkedList;
import java.util.Map;

import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

public class ListHttpRequestHandler extends HttpRequestHandler {

	
	private class Pair {
		private int priority;
		private HttpRequestHandler handler;
		private Pair(int priority, HttpRequestHandler handler) {
			this.priority = priority;
			this.handler = handler;
		}
	}
	
	private LinkedList<Pair> requestHandlerList;
	
	public ListHttpRequestHandler () {
		requestHandlerList = new LinkedList<ListHttpRequestHandler.Pair>();
	}
	
	public void addHttpRequestHandler(HttpRequestHandler handler, int priority) {
		if (handler == null || priority < 0) {
			return;
		}
		int index = 0;
		for(Pair pair : requestHandlerList) {
			if (pair.priority > priority) {
				requestHandlerList.add(index, new Pair(priority, handler));
				return;
			}
			index++;
		}
		requestHandlerList.add(new Pair(priority, handler));
	}
	
	@Override
	public boolean acceptRequest(Method method, String uri) {
		for (Pair pair : requestHandlerList) {
			if (pair.handler.acceptRequest(method, uri)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		for (Pair pair : requestHandlerList) {
			if (pair.handler.acceptRequest(method, uri)) {
				return pair.handler.handleRequest(method, uri, parameters, mimeType, files);
			}
		}
		//TODO maybe I should return a 404 here
		return null;
	}
	
	

}
