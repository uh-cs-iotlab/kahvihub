/*
 * fi.helsinki.cs.iot.hub.model.enabler.PluginInfo
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
package fi.helsinki.cs.iot.hub.model.enabler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginInfo extends BasicPluginInfo {

    private long id;

    public PluginInfo(long id, Type type, String serviceName, String packageName, String filename) {
        super(type, serviceName, packageName, filename);
        this.id = id;
    }

    public long getId() {
        return id;
    }

	public JSONObject toJSON() throws JSONException {
		JSONObject jObj = new JSONObject();
		jObj.put("id", id);
		jObj.put("pluginType", getType().toString());
		jObj.put("service", getServiceName());
		if (getPackageName() != null) {
			jObj.put("package", getPackageName());
		}
		return jObj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PluginInfo other = (PluginInfo) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}
