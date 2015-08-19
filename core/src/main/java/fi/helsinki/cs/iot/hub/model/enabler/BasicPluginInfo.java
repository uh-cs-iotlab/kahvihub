/*
 * fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo
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


/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * 
 */
public class BasicPluginInfo {

	public enum Type {
		NATIVE, JAVASCRIPT
	}
	
	private Type type;
    private String serviceName;
    private String packageName;
    private String filename;

    public BasicPluginInfo(Type type, String serviceName, String packageName, String filename) {
        this.type = type;
    	this.serviceName = serviceName;
        this.packageName = packageName;
        this.filename = filename;
    }
    
    public Type getType() {
    	return type;
    }
    
    public boolean isNative() {
    	return type == Type.NATIVE;
    }
    
    public boolean isJavascript() {
    	return !isNative();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFilename() {
		return filename;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicPluginInfo)) return false;

        BasicPluginInfo that = (BasicPluginInfo) o;

        if (!type.equals(that.type)) return false;
        if (!packageName.equals(that.packageName)) return false;
        if (!serviceName.equals(that.serviceName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BasicPluginInfo{" +
        		"type='" + type.toString() + '\'' +
                "serviceName='" + serviceName + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
