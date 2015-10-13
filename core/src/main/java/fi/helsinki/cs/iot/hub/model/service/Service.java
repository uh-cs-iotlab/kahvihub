/*
 * fi.helsinki.cs.iot.hub.service.Service
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
package fi.helsinki.cs.iot.hub.model.service;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class Service {

	private final long id;
	private final ServiceInfo serviceInfo;
	private final String name;
	private final String metadata;
	private final String config;
	private final boolean bootAtStartup;

	/**
	 * @param id
	 * @param serviceInfo
	 * @param name
	 * @param metadata
	 * @param config
	 * @param bootAtStartup
	 */
	public Service(long id, ServiceInfo serviceInfo, String name,
			String metadata, String config, boolean bootAtStartup) {
		this.id = id;
		this.serviceInfo = serviceInfo;
		this.name = name;
		this.metadata = metadata;
		this.config = config;
		this.bootAtStartup = bootAtStartup;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the serviceInfo
	 */
	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the metadata
	 */
	public String getMetadata() {
		return metadata;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @return the bootAtStartup
	 */
	public boolean bootAtStartup() {
		return bootAtStartup;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", id);
		jsonObject.put("name", name);
		if (metadata != null) {
			jsonObject.put("metadata", metadata);
		}
		if (config != null) {
			jsonObject.put("config", config);
		}
		jsonObject.put("bootAtStartup", bootAtStartup);
		jsonObject.put("plugin", serviceInfo.toJSON());
		return jsonObject;
	}
}
