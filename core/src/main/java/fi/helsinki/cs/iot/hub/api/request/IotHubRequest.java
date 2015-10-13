/*
 * fi.helsinki.cs.iot.hub.api.request.IotHubRequest
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
package fi.helsinki.cs.iot.hub.api.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class IotHubRequest {
	
	public enum Type {
		APPLICATION, SERVICE, FEED, PLUGIN, ENABLER, UNKNOWN
	}
	
	private Method method;
	private Type type;
	private List<String> identifiers;
	private String fullUri;
	private Map<String, String> options;
	private String mimeType;
	private String bodyData;
	
	public IotHubRequest (Method method, String uri, Map<String, String> options) {
		this(method, uri, options, null, null);
	}
	
	public IotHubRequest (Method method, String uri, Map<String, String> options, 
			String mimeType, String bodyData) {
		this.method = method;
		this.fullUri = uri;
		String trimmedUri = null;
		if (uri != null && uri.startsWith("/feeds")) {
			this.type = Type.FEED;
			trimmedUri = uri.replaceFirst("/feeds", "");
		}
		else if (uri != null && uri.startsWith("/plugins")) {
			this.type = Type.PLUGIN;
			trimmedUri = uri.replaceFirst("/plugins", "");
		}
		else if (uri != null && uri.startsWith("/enablers")) {
			this.type = Type.ENABLER;
			trimmedUri = uri.replaceFirst("/enablers", "");
		}
		else if (uri != null && uri.startsWith("/applications")) {
			this.type = Type.APPLICATION;
			trimmedUri = uri.replaceFirst("/applications", "");
		}
		else if (uri != null && uri.startsWith("/services")) {
			this.type = Type.SERVICE;
			trimmedUri = uri.replaceFirst("/services", "");
		}
		else {
			this.type = Type.UNKNOWN;
		}
		if (trimmedUri != null && trimmedUri.startsWith("/")) {
			trimmedUri = trimmedUri.substring(1);
		}
		this.options = options;
		if (this.type != Type.UNKNOWN) {
			identifiers = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(trimmedUri, "/");
			while (st.hasMoreTokens()) {
				identifiers.add(st.nextToken());
			}
		}
		this.mimeType = mimeType;
		this.bodyData = bodyData;
	}
	public Type getType() {
		return type;
	}

	public String getFullUri() {
		return fullUri;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public List<String> getIdentifiers() {
		return identifiers;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getBodyData() {
		return bodyData;
	}

	public Method getMethod() {
		return method;
	}
	
}
