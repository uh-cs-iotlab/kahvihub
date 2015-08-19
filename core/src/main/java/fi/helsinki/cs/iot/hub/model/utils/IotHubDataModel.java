/*
 * fi.helsinki.cs.iot.hub.model.utils.IotHubDataModel
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
package fi.helsinki.cs.iot.hub.model.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the core component that links the plugins/enablers/feeds/services/applications
 * Each component running on the IoT Hub defines the data types that are involved in the each of
 * the components. This class should also in the future maintain a list of dependencies between 
 * data types and also how to convert some data (with units) to a different units or to another
 * data type.
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class IotHubDataModel {
	
	private static IotHubDataModel instance;
	
	private List<String> dataTypes;
	
	private IotHubDataModel () {
		this.dataTypes = new ArrayList<>();
	}
	
	public static IotHubDataModel getInstance() {
		if (instance == null) {
			instance = new IotHubDataModel();
		}
		return instance;
	}
	
	public boolean checkType(String type) {
		//TODO this method should check the type of 
		return true;
	}
	
	public boolean addType(String type) {
		if (dataTypes.contains(type)) {
			return false;
		}
		else {
			return dataTypes.add(type);
		}
	}

}
