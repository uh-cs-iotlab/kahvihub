/*
 * fi.helsinki.cs.iot.hub.service.ServiceInfo
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
 * @author mineraud
 *
 */
public class ServiceInfo {
	
	public enum Type {
		NATIVE, JAVASCRIPT
	}
	
	private Type type;
	private long id;
	private String name;
	private String filename;
	
	/**
	 * @param id
	 * @param name
	 */
	public ServiceInfo(long id, Type type, String name, String filename) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.filename = filename;
	}

	public long getId() {
        return id;
    }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the file
	 */
	public String getFilename() {
		return filename;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject jObj = new JSONObject();
		jObj.put("id", id);
		jObj.put("pluginType", type.name());
		jObj.put("service", name);
		return jObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServiceInfo other = (ServiceInfo) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
