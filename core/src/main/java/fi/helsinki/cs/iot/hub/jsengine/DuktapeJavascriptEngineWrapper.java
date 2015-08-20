/*
 * fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper
 * v0.1
 * 2015
 *
 * Copyright 2015 University of Helsinki
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 */ 
package fi.helsinki.cs.iot.hub.jsengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class DuktapeJavascriptEngineWrapper {

	private static final String TAG = "DuktapeJavascriptEngineWrapper";

	public static final int TCP_SOCKET = 1;
	public static final int HTTP_REQUEST = 2;
	public static final int EVENT_LOOP = 4;

	private boolean needToStopAllEvents;
	private HashMap<Integer, TcpSocket> sockets;
	private JavascriptedIotHubCode javascriptedIotHubCode;
	private int lastSocketId;
	private final int modes;
	
	//TODO in the future I would like to have this done directly with C
	public static final String minifiedEventloopJs = "function setTimeout(e,t){var n,o,i;if(\"number \"!=typeof t)throw new TypeError(\"delay is not a number\");"
			+ "if(\"string\"==typeof e)n=eval.bind(this,e);else{if(\"function\"!=typeof e)throw new TypeError(\"callback is not a function/string\");"
			+ "arguments.length>2?(o=Array.prototype.slice.call(arguments,2),o.unshift(this),n=e.bind.apply(e,o)):n=e}return i=EventLoop.createTimer(n,t,!0)}"
			+ "function clearTimeout(e){if(\"number\"!=typeof e)throw new TypeError(\"timer ID is not a number\");EventLoop.deleteTimer(e)}function "
			+ "setInterval(e,t){var n,o,i;if(\"number\"!=typeof t)throw new TypeError(\"delay is not a number\");if(\"string\"==typeof e)n=eval.bind(this,e);"
			+ "else{if(\"function\"!=typeof e)throw new TypeError(\"callback is not a function/string\");arguments.length>2?(o=Array.prototype.slice.call(arguments,2),"
			+ "o.unshift(this),n=e.bind.apply(e,o)):n=e}return i=EventLoop.createTimer(n,t,!1)}function clearInterval(e){if(\"number\"!=typeof e)throw new TypeError(\""
			+ "timer ID is not a number\");EventLoop.deleteTimer(e)}function requestEventLoopExit(){EventLoop.requestExit()}EventLoop.socketListening={},"
			+ "EventLoop.socketReading={},EventLoop.socketConnecting={},EventLoop.fdPollHandler=function(e,t){var n,o,i;if(t&Poll.POLLIN)if(o=this.socketReading[e])"
			+ "{if(n=Socket.read(e),0===n.length)return void this.close(e);o(e,n)}else o=this.socketListening[e],o&&(i=Socket.accept(e),o(i.fd,i.addr,i.port));"
			+ "t&Poll.POLLOUT&&(o=this.socketConnecting[e],o&&(delete this.socketConnecting[e],o(e))),0!==(t&~(Poll.POLLIN|Poll.POLLOUT))&&this.close(e)},"
			+ "EventLoop.server=function(e,t,n){var o=Socket.createServerSocket(e,t);this.socketListening[o]=n,this.listenFd(o,Poll.POLLIN)},"
			+ "EventLoop.connect=function(e,t,n){var o=Socket.connect(e,t);this.socketConnecting[o]=n,this.listenFd(o,Poll.POLLOUT)},"
			+ "EventLoop.close=function(e){EventLoop.listenFd(e,0),delete this.socketListening[e],delete this.socketReading[e],delete this.socketConnecting[e],Socket.close(e)},"
			+ "EventLoop.setReader=function(e,t){this.socketReading[e]=t,this.listenFd(e,Poll.POLLIN)},EventLoop.write=function(e,t){Socket.write(e,Duktape.Buffer(t))};";

	// load the built libraries
	static {
		System.loadLibrary("jsDuktapeJni");
	}

	public DuktapeJavascriptEngineWrapper() {
		this(null, 0);
	}

	public DuktapeJavascriptEngineWrapper(JavascriptedIotHubCode javascriptConfigurable, int modes) {
		this.javascriptedIotHubCode = javascriptConfigurable;
		this.needToStopAllEvents = false;
		this.sockets = new HashMap<>();
		this.lastSocketId = 1000;
		this.modes = modes;
	}

	public boolean needToStopAllEvents() {
		return needToStopAllEvents;
	}

	public void stopAllEvents(boolean needToStopAllEvents) {
		this.needToStopAllEvents = needToStopAllEvents;
	}

	public boolean hasTcpSockets() {
		return javascriptedIotHubCode != null && 
				(modes & TCP_SOCKET) == TCP_SOCKET;
	}

	public boolean hasHttpRequest() {
		return javascriptedIotHubCode != null && 
				(modes & HTTP_REQUEST) == HTTP_REQUEST;
	}

	public boolean hasEventLoop() {
		return javascriptedIotHubCode != null &&
				(modes & EVENT_LOOP) == EVENT_LOOP;
	}

	public native String runScript(String script) throws JavascriptEngineException;
	public native boolean checkService(String serviceName, String serviceScript) throws JavascriptEngineException;
	public native boolean checkPlugin(String libraryName, String script) throws JavascriptEngineException;
	public native String getLibraryOutput(String libraryName, String script, String toEvaluate) throws JavascriptEngineException;
	public native void run(String serviceName, String serviceScript, String serviceConf) throws JavascriptEngineException;

	public boolean pluginNeedConfiguration(String pluginName, String script) throws JavascriptEngineException {
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.needConfiguration;", pluginName));
		if (res == null || !(res.equals("true") || res.equals("false"))) {
			throw new JavascriptEngineException(TAG, 
					String.format("The method %s.needConfiguration does not provide a boolean value (returned %s)", 
							pluginName, res));
		}
		return res.equals("true");
	}

	public boolean serviceNeedConfiguration(String serviceName, String script) throws JavascriptEngineException {
		return pluginNeedConfiguration(serviceName, script);
	}

	public boolean serviceCheckConfiguration(String serviceName, String script, String configuration) throws JavascriptEngineException {
		return pluginCheckConfiguration(serviceName, script, configuration);
	}

	public boolean pluginCheckConfiguration(String pluginName, String script, String configuration) throws JavascriptEngineException {
		//TODO maybe check if I can configure a plugin with null
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return false;
		}
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.checkConfiguration(\"%s\");", pluginName, configurationForJS));
		if (res == null || !(res.equals("true") || res.equals("false"))) {
			throw new JavascriptEngineException(TAG, 
					String.format("The method %s.checkConfiguration(%s) does not provide a boolean value (returned %s)", 
							pluginName, configurationForJS, res));
		}
		return res.equals("true");
	}	

	private boolean internalCheckPluginFeatureFunction(String functionName, String pluginName, String script, String configuration, 
			String featureName) throws JavascriptEngineException {
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return false;
		}
		String evalScript = String.format("%s.configure(\"%s\"); %s.%s(\"%s\");", 
				pluginName, configurationForJS,
				pluginName, functionName, featureName);
		String res = getLibraryOutput(pluginName, script, evalScript);
		if (res == null || !(res.equals("true") || res.equals("false"))) {
			throw new JavascriptEngineException(TAG, 
					String.format("The method %s.%s(\"%s\") does not provide a boolean value (returned %s)", 
							pluginName, functionName, configurationForJS, res));
		}
		return res.equals("true");
	}

	public boolean isPluginFeatureSupported(String pluginName, String script, String configuration, 
			String featureName) throws JavascriptEngineException {
		return internalCheckPluginFeatureFunction("isFeatureSupported", pluginName, script, configuration, featureName);
	}

	public boolean isPluginFeatureAvailable(String pluginName, String script, String configuration, 
			String featureName) throws JavascriptEngineException {
		return internalCheckPluginFeatureFunction("isFeatureAvailable", pluginName, script, configuration, featureName);
	}

	public boolean isPluginFeatureReadable(String pluginName, String script, String configuration, 
			String featureName) throws JavascriptEngineException {
		return internalCheckPluginFeatureFunction("isFeatureReadable", pluginName, script, configuration, featureName);
	}

	public boolean isPluginFeatureWritable(String pluginName, String script, String configuration, 
			String featureName) throws JavascriptEngineException {
		return internalCheckPluginFeatureFunction("isFeatureWritable", pluginName, script, configuration, featureName);
	}

	public int getPluginNumberOfFeatures(String pluginName, String script, String configuration) throws JavascriptEngineException {
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return -1;
		}
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.configure(\"%s\"); %s.getNumberOfFeatures();", 
						pluginName, configurationForJS, pluginName));
		try {
			return Integer.parseInt(res);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	public FeatureDescription getPluginFeatureDescription(String pluginName, String script, String configuration, int index) throws JavascriptEngineException {
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return null;
		}
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.configure(\"%s\"); %s.getFeatureDescription(%d);", 
						pluginName, configurationForJS, 
						pluginName, index));
		JSONObject json;
		if(res == null) {
			return null;
		}
		try {
			json = new JSONObject(res);
			FeatureDescription fd = new FeatureDescription(json.getString("name"), json.getString("type"));
			return fd;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println(res);
			e.printStackTrace();
		}
		return null;
	}

	public String getPluginFeatureValue(String pluginName, String script, String configuration, String featureName) throws JavascriptEngineException {
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return null;
		}
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.configure(\"%s\"); %s.getFeatureValue(\"%s\");", 
						pluginName, configurationForJS, 
						pluginName, featureName));
		return res;
	}

	private String jsonConfigToString(JSONObject json) {
		return json.toString().replaceAll("\"", Matcher.quoteReplacement("\\\""));
	}

	private String jsonConfigToString(JSONArray json) {
		return json.toString().replaceAll("\"", Matcher.quoteReplacement("\\\""));
	}

	private String getDataToSend(String data) {
		String dataToSend = null;
		try {
			JSONObject jobj = new JSONObject(data);
			dataToSend = jsonConfigToString(jobj);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		if (dataToSend == null) {
			try {
				JSONArray jarr = new JSONArray(data);
				dataToSend = jsonConfigToString(jarr);
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		return dataToSend;
	}

	public boolean postPluginFeatureValue(String pluginName, String script, String configuration, String featureName, String data) throws JavascriptEngineException {
		String configurationForJS = getDataToSend(configuration);
		if (configurationForJS == null) {
			return false;
		}
		String dataToSend = getDataToSend(data);
		if (dataToSend == null) {
			return false;
		}
		String res = getLibraryOutput(pluginName, script, 
				String.format("%s.configure(\"%s\"); %s.postFeatureValue(\"%s\", \"%s\");", 
						pluginName, configurationForJS, 
						pluginName, featureName, dataToSend));
		if (res == null || !(res.equals("true") || res.equals("false"))) {
			throw new JavascriptEngineException(TAG, 
					String.format("The method %s.%s(\"%s\", \"%s\") does not provide a boolean value (returned %s)", 
							pluginName, "postFeatureValue", featureName, dataToSend, res));
		}
		return res.equals("true");
	}

	public static String performJavaHttpRequest(String method, String url, String data) {
		HttpClient httpclient = HttpClientBuilder.create().build();
		try {
			switch (method) {
			case "GET": 
				return performJavaHttpGetRequest(httpclient, method, url);
			case "POST": 
				return performJavaHttpPostRequest(httpclient, method, url, data);
			case "PUT": 
				return performJavaHttpPutRequest(httpclient, method, url, data);
			case "DELETE": 
				return performJavaHttpDeleteRequest(httpclient, method, url);
			default:
				return "UNDEFINED method";
			}
		} catch (Exception e) {
			return "I got the exception:" + e.getMessage();
		} finally {
		}
	}

	private static String performJavaHttpGetRequest(HttpClient httpclient,
			String method, String url) throws ClientProtocolException, IOException {
		HttpGet httpget = new HttpGet(url);
		return executeHttpRequest(httpclient, httpget);
	}

	private static String performJavaHttpPostRequest(HttpClient httpclient,
			String method, String url, String data) throws ClientProtocolException, IOException {
		HttpPost httppost = new HttpPost(url);
		addRequestBody(httppost, data);
		if(httppost.containsHeader("content-type")) {
			for (Header h : httppost.getHeaders("content-type")) {
				Log.d(TAG, "The content type is " + h.getName() + " " + h.getValue());
			}
		}
		else {
			Log.d(TAG, "The is no content type");
		}
		return executeHttpRequest(httpclient, httppost);
	}

	private static String performJavaHttpPutRequest(HttpClient httpclient,
			String method, String url, String data) throws ClientProtocolException, IOException {
		HttpPut httpput = new HttpPut(url);
		addRequestBody(httpput, data);
		return executeHttpRequest(httpclient, httpput);
	}

	private static String performJavaHttpDeleteRequest(HttpClient httpclient,
			String method, String url) throws ClientProtocolException, IOException {
		HttpDelete httpdelete = new HttpDelete(url); 
		return executeHttpRequest(httpclient, httpdelete);
	}

	private static void addRequestBody (HttpEntityEnclosingRequestBase message, String data) 
			throws UnsupportedEncodingException {
		if (data != null) {
			StringEntity se =new StringEntity(data);
			message.addHeader("content-type", HttpRequestHandler.JSON_MIME_TYPE);
			message.setEntity(se);
		}
	}

	private static String executeHttpRequest(HttpClient httpclient, HttpUriRequest request) 
			throws ClientProtocolException, IOException {
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
			String result= ScriptUtils.convertStreamToString(instream);
			// now you have the string representation of the HTML request
			instream.close();
			return result;
		}
		return null;
	}

	public boolean makePluginConfigurationPersistant(String configuration) {
		if (javascriptedIotHubCode == null) {
			Log.e(TAG, "Try to save persistant configuration on a null IoT hub code");
			return false;
		}
		return javascriptedIotHubCode.configurePersistant(configuration);
	}

	public int tcpSocketConnect(String address, int port) {
		//Need to return -1 if fails
		TcpSocket socket;
		try {
			if (!TcpSocket.checkHostAvailability(address, port)) {
				return -1;
			}
			socket = new TcpSocket(lastSocketId + 1, address, port);
			sockets.put(socket.getId(), socket);
			lastSocketId++;
			return socket.getId();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public String tcpSocketSend(int socketId, String message) throws IOException, JavascriptEngineException {
		try {
			TcpSocket socket = sockets.get(socketId);
			if (socket != null) {
				return socket.send(message);
			}
			throw new JavascriptEngineException(TAG, "No tcp socket has been found for id " + socketId);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public boolean tcpSocketClose(int socketId) {
		TcpSocket socket = sockets.get(socketId);
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return this.sockets.remove(socketId) != null;
		}
		return false;
	}

	
}
