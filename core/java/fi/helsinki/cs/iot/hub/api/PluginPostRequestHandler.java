/*
 * fi.helsinki.cs.iot.hub.api.PluginPostRequestHandler
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
package fi.helsinki.cs.iot.hub.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fi.helsinki.cs.iot.hub.api.uri.IotHubUri;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginPostRequestHandler extends IoTHubApiRequestHandler {
	
	//private static final String TAG = "PluginPostRequestHandler";
	private List<Method> methods;
	private Path libdir;

	public PluginPostRequestHandler(Path libdir) {
		this.libdir = libdir;
		this.methods = new ArrayList<>();
		this.methods.add(Method.POST);	
	}

	/**
	 * @return the libdir
	 */
	public Path getLibdir() {
		return libdir;
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}


	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubUri uri) {
		// TODO Auto-generated method stub
		return null;
	}

}
