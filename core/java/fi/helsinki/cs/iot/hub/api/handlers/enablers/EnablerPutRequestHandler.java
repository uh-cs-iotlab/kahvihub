/*
 * fi.helsinki.cs.iot.hub.api.handlers.enablers.PluginPutRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.enablers;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class EnablerPutRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "EnablerPutRequestHandler";
	private List<Method> methods;

	public EnablerPutRequestHandler() {
		this.methods = new ArrayList<>();
		this.methods.add(Method.PUT);	
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	private Response getResponseForEnablerUpdate(String enablerName, String bodyData) {
		if (enablerName == null || bodyData == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method put is not available without the enabler name");
		}
		else {
			try {
				Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerName);
				if (enabler == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Cannot update the non-exisiting enabler " + enablerName);
				}

				JSONObject jdata = new JSONObject(bodyData);

				if (!jdata.has("configuration") && !jdata.has("name") && !jdata.has("metadata")) {
					Log.w(TAG, "No need to update if nothing is new");
					System.err.println("No need to update if nothing is new");
					return getResponseOk(enabler.toJSON().toString());
				}

				String newName = jdata.has("name") ? jdata.getString("name") : enabler.getName();
				String newMetadata = jdata.has("metadata") ? jdata.getString("metadata") : enabler.getMetadata();
				String newConfiguration = jdata.has("configuration") ? jdata.getString("configuration") : enabler.getPluginConfig();

				//Need to check the new configuration if there is one
				if (jdata.has("configuration")) {
					try {
						Plugin plugin = PluginManager.getInstance().getConfiguredPlugin(enabler);
						if (plugin == null) {
							return getResponseKo(STATUS_BAD_REQUEST, "Could not get the corresponding plugin");
						}
						if (!plugin.configure(newConfiguration)) {
							return getResponseKo(STATUS_BAD_REQUEST, "The plugin could not be configured");
						}
						else {
							//I need to remove all previous features
							if(IotHubDataAccess.getInstance().deleteFeaturesOfEnabler(enabler) == null) {
								Log.e(TAG, "I could not delete the previous features of the enabler");
								return getResponseKo(STATUS_BAD_REQUEST, "I could not delete the previous features of the enabler");
							}
							if (!plugin.needConfiguration() || plugin.isConfigured()) {
								for(int i = 0; i < plugin.getNumberOfFeatures(); i++) {
									FeatureDescription fd = plugin.getFeatureDescription(i);
									Feature feature = IotHubDataAccess.getInstance().addFeature(enabler, fd.getName(), fd.getType());
									if (feature == null) {
										Log.e(TAG, "The feature should not be null");
										break;
									}
								}
								enabler = IotHubDataAccess.getInstance().getEnabler(enabler.getId());
							}
							else {
								Log.w(TAG, "The plugin needs to be configured to install the features");
								System.out.println("The plugin needs to be configured to install the features");
							}
						}
						
					} catch (PluginException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return getResponseKo(STATUS_BAD_REQUEST, "The configuration item was not good");
					}
				}
				
				Enabler updatedEnabler = IotHubDataAccess.getInstance().updateEnabler(enabler, newName, newMetadata, newConfiguration);
				if (updatedEnabler == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not update the enabler with name " + enablerName);
				}
				else {
					return getResponseOk(updatedEnabler.toJSON().toString());
				}
			} catch (JSONException e) {
				return getResponseKo(STATUS_BAD_REQUEST, "Could not update the enabler with name " + enablerName);
			}
		}
	}
	
	private Response getResponseForFeatureUpdate(String enablerName, String featureName, String bodyData) {
		if (enablerName == null || featureName == null || bodyData == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, 
					"The method put is not available without the enabler name and the feature name");
		}
		Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerName);
		if (enabler == null) {
			return getResponseKo(STATUS_BAD_REQUEST, "Could not get the corresponding enabler");
		}
		for (Feature feature : enabler.getFeatures()) {
			
			if (feature.getName().equals(featureName)) {
				try {
					JSONObject jdata = new JSONObject(bodyData);
					boolean enableAsAtomic = jdata.getBoolean("enableAsAtomicFeed");
					if (feature.isAtomicFeed() == enableAsAtomic) {
						Log.w(TAG, "No need to update a feature with the same configuration");
						return getResponseOk(feature.toJSON().toString());
					}
					else {
						//TODO In the future, I would want to update the name (just the one for the api, not the plugin)
						Feature updatedFeature = IotHubDataAccess.getInstance().updateFeature(feature, enableAsAtomic);
						if (updatedFeature == null) {
							return getResponseKo(STATUS_BAD_REQUEST, "Could not update the feature " + feature.toJSON().toString());
						}
						else {
							return getResponseOk(updatedFeature.toJSON().toString());
						}
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return getResponseKo(STATUS_BAD_REQUEST, "The configuration item was not good");
				}
			}
		}
		return getResponseKo(STATUS_BAD_REQUEST, "Could not get the corresponding feature"); 
	}


	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.request.IotHubRequest)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		if (request == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Cannot work with no request");
		}
		else if (request.getMethod() != Method.PUT) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Method " + request.getMethod() + " not supported");
		}
		else if (request.getIdentifiers().size() == 0) {
			String enablerName = request.getOptions().get("name");
			if (enablerName == null) {
				return getResponseKo(STATUS_BAD_REQUEST, "Cannot update the enabler without name");
			}
			else {
				return getResponseForEnablerUpdate(enablerName, request.getBodyData());	
			}
		}
		else if (request.getIdentifiers().size() == 1) {
			//TODO In the future, I would like to be able to update many features at once
			return getResponseForEnablerUpdate(request.getIdentifiers().get(0), request.getBodyData());	
		}
		else if (request.getIdentifiers().size() == 2){
			return getResponseForFeatureUpdate(request.getIdentifiers().get(0),
					request.getIdentifiers().get(1), request.getBodyData());
		}
		else {
			//FIXME fix that
			return getResponseKo(STATUS_BAD_REQUEST, STATUS_BAD_REQUEST);
		}
	}

}
