/* 
 * fi.helsinki.cs.iot.hub.utils.Json
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
package fi.helsinki.cs.iot.hub.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author mineraud
 *
 */
public class Json {

	private enum Type {
		NULL, BOOLEAN, NUMBER, STRING, ASSOCIATION, LIST 
	}

	private Type type;
	private Object value;
	private Map<String, Json> associations;
	private List<Json> list;

	private Json (Type type) {
		this.type = type;
		this.value = null;
		this.associations = null;
		this.list = null;
	}

	public Json () {
		this(Type.NULL);
	}

	public Json(boolean value) {
		this(Type.BOOLEAN);
		this.value = Boolean.valueOf(value);
	}

	public Json(int value) {
		this(Type.NUMBER);
		this.value = Integer.valueOf(value);
	}

	public Json(long value) {
		this(Type.NUMBER);
		this.value = Long.valueOf(value);
	}

	public Json(float value) {
		this(Type.NUMBER);
		this.value = Float.valueOf(value);
	}

	public Json(double value) {
		this(Type.NUMBER);
		this.value = Double.valueOf(value);
	}

	public Json(String value) {
		this(Type.STRING);
		this.value = value;
	}

	public Json(List<Json> list) {
		this(Type.LIST);
		this.list = list;
	}

	public Json(Map<String, Json> assoc) {
		this(Type.ASSOCIATION);
		this.associations = assoc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((associations == null) ? 0 : associations.hashCode());
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	public boolean checkAssociationsEqualsto(Map<String, Json> other, boolean allowSubset) {
		for (Entry<String, Json> entry : associations.entrySet()) {
			if (!other.containsKey(entry.getKey())) {
				return false;
			}
			else if (!other.get(entry.getKey()).equals(entry.getValue())) {
				return false;
			}
		}
		if (!allowSubset && associations.size() != other.size()) {
			return false;
		}
		return true;
	}

	public boolean checkListEqualsto(List<Json> other) {
		Iterator<Json> listIt = list.iterator();
		Iterator<Json> otherIt = other.iterator();
		while (listIt.hasNext()) {
			if (!otherIt.hasNext()) {
				return false;
			}
			else if (!listIt.next().equals(otherIt.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Json other = (Json) obj;
		if (type != other.type)
			return false;
		if (associations == null) {
			if (other.associations != null)
				return false;
		} else if (!checkAssociationsEqualsto(other.associations, false))
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!checkListEqualsto(other.list))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public boolean isSubsetOf(Json json) {
		if (this == json)
			return true;
		if (json == null)
			return false;
		if (type != json.type)
			return false;
		if (associations == null) {
			if (json.associations != null)
				return false;
		} else if (!checkAssociationsEqualsto(json.associations, true))
			return false;
		if (list == null) {
			if (json.list != null)
				return false;
		} else if (!checkListEqualsto(json.list))
			return false;
		if (value == null) {
			if (json.value != null)
				return false;
		} else if (!value.equals(json.value))
			return false;
		return true;
	}

	/*public static Json parseJson (String value) throws JsonException {
		JSONObject jsonObject = null;
		JSONArray jsonArray = null;
		try {
			jsonObject = new JSONObject(value);
			if (jsonObject.length() == 0) {
				return new Json();
			}
			else if (jsonObject.length() == 1) {
				try {
					boolean val = jsonObject.getBoolean(jso);
					return new Json(val);
				} catch (JSONException e) {}
				try {
					boolean val = jsonObject.getInt(arg0)(value);
					return new Json(val);
				} catch (JSONException e) {}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (jsonObject == null) {
			try {
				jsonArray = new JSONArray(value);
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static boolean checkJSONEquality(JSONObject j1, JSONObject j2, boolean isSubsetOf) {
		if (j1 == j2) {
			return true;
		}
		else if (j2 == null) {
			return false;
		}
		Iterator<String> j1KeysIt = j1.keys();
		while (j1KeysIt.hasNext()) {
			String j1Key = j1KeysIt.next();
			if (j1.)
		}
	}
 */
}
