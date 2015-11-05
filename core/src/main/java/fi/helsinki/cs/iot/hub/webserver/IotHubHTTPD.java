/*
 * fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD
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
package fi.helsinki.cs.iot.hub.webserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import fi.helsinki.cs.iot.hub.api.handlers.basic.BasicIotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.handlers.basic.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.Logger;

/**
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * Based on the SimpleWebServer example of NanoHttpd
 */
public class IotHubHTTPD extends NanoHTTPD {

	private static final String TAG = "IotHubHTTPD";
	private int port;
	private Path libdir;
	private String host;
	private HttpRequestHandler requestHandler;

	public IotHubHTTPD(int port, Path libdir) {
		super("127.0.0.1", port);
		this.port = port;
		this.host = "127.0.0.1";
		this.libdir = libdir;
		this.requestHandler = new BasicIotHubApiRequestHandler(this.libdir);
	}

	public IotHubHTTPD(int port, Path libdir, String host) {
		super(host, port);
		this.port = port;
		this.host = host;
		this.libdir = libdir;
		this.requestHandler = new BasicIotHubApiRequestHandler(this.libdir);
	}

	public void setHttpRequestHandler(HttpRequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	public void setLogger(Logger logger) {
		Log.setLogger(logger);
	}

	public void setIotHubDataHandler(IotHubDataHandler handler) {
		IotHubDataAccess.setInstance(handler);
	}

	public int getPort() {
		return this.port;
	}

	public String getHost() {
		return this.host;
	}

	@Override
	public void start() throws IOException {
		// TODO Auto-generated method stub
		super.start();
		try {
			IotHubDataAccess.getInstance().open();
			for (Enabler enabler : IotHubDataAccess.getInstance().getEnablers()) {
				//It will initialize the plugins
				try {
					PluginManager.getInstance().getConfiguredPlugin(enabler);
				} catch (PluginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IotHubDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "Server started on port " + this.port);
	}

	@Override
	public void stop() {
		super.stop();
		try {
			IotHubDataAccess.getInstance().close();
		} catch (IotHubDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "Server stopped");

	}

	private String getMimeType(IHTTPSession session) {
		Map<String, String> header = session.getHeaders();
		for (Entry<String, String> e : header.entrySet()) {
			// content-type is case-insensitive
			if (e.getKey().equalsIgnoreCase("content-type")) {
				return e.getValue();
			}
		}
		return null;
	}

	@Override
	public Response serve(IHTTPSession session) {
		Map<String, String> files = null;
		if (session.getMethod() == Method.PUT || session.getMethod() == Method.POST) {
			files = new HashMap<String, String>();
			try {
				session.parseBody(files);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ResponseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		String uri = session.getUri();
		Method method = session.getMethod();
		String mimeType = getMimeType(session);
		//FIXME connect the requestId to the database logger
		//long requestId = 0L;
		Response response = requestHandler.handleRequest(method, 
				uri, session.getParms(), mimeType, files);
		//TODO log the time at the call of serve and when the response is ready
		return response;
	}

}
