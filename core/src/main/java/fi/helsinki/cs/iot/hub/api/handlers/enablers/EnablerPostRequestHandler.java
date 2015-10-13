/*
 * fi.helsinki.cs.iot.hub.api.handlers.enablers.PluginPostRequestHandler
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
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
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
public class EnablerPostRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "EnablerPostRequestHandler";
	private List<Method> methods;

	public EnablerPostRequestHandler() {
		this.methods = new ArrayList<>();
		this.methods.add(Method.POST);	
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}


	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.request.IotHubRequest)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		if (request == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Cannot work with no request");
		}
		else if (request.getMethod() != Method.POST) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Method " + request.getMethod() + " not supported");
		}
		else if (request.getIdentifiers().size() != 0){
			return getResponseKo(STATUS_BAD_REQUEST, "Cannot work with this request if the number of identifier is greater than 0");
		}
		else {
			String bodyData = request.getBodyData();
			if (bodyData == null) {
				return getResponseKo(STATUS_BAD_REQUEST, "The data seems to be missing");
			}
			try {
				JSONObject jdata = new JSONObject(bodyData);
				long pluginId = jdata.getLong("plugin");
				String name = jdata.getString("name");
				
				String metadata = jdata.has("metadata") ? jdata.getString("metadata") : null;
				String config = jdata.has("configuration") ? jdata.getString("configuration") : null;

				Enabler enabler = null;

				PluginInfo pluginInfo = IotHubDataAccess.getInstance().getPluginInfo(pluginId);
				if (pluginInfo != null) {
					enabler = IotHubDataAccess.getInstance().addEnabler(name, metadata, pluginInfo, config);
				}

				if (enabler == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "I could not add the enabler to the db");
				}
				else {
					Plugin plugin = null;
					try {
						plugin = PluginManager.getInstance().getConfiguredPlugin(enabler);
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
							Log.d(TAG, "The plugin needs to be configured to install the features");
						}
					} catch (PluginException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Too bad the configuration of the plugin failed");
					}
					return getResponseOk(enabler.toJSON().toString());
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getResponseKo(STATUS_BAD_REQUEST, "The data is not JSON data or is incorrect");
			}
		}

	}

}
