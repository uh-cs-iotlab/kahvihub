/*
 * fi.helsinki.cs.iot.hub.model.enabler.PluginManager
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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * 
 */
public class PluginManager {

	private static final String TAG = "PluginManager";
	private static PluginManager instance = null;
	private Map<String, Plugin> plugins;

	private NativePluginHelper nativePluginHelper;
	private JavascriptPluginHelper javascriptPluginHelper;


	private PluginManager() {
		this.plugins = new HashMap<String, Plugin>();
		this.nativePluginHelper = null;
		this.javascriptPluginHelper = null;
	}

	public static PluginManager getInstance() {
		if (instance == null) {
			instance = new PluginManager();
		}
		return instance;
	}	

	public Plugin getPlugin(Enabler enabler) {
		if (enabler == null) {
			return null;
		}
		Plugin plugin = plugins.get(enabler.getName());
		// If the plugin does not exist, I need to create one
		if (plugin == null) {
			if (enabler.getPluginInfo().isNative()) {
				if (nativePluginHelper == null) {
					Log.e(TAG, "Trying to instanciate a native plugin when no native plugin helper has been set");
					return null;
				}
				else {
					plugin = nativePluginHelper.createPlugin(enabler);
					if (plugin == null) {
						Log.e(TAG, "The native plugin could not be created");
					}
					else {
						plugins.put(enabler.getName(), plugin);
						Log.i(TAG, "Native plugin for enable " + enabler.getName() + 
								" added to the list of plugins");
					}
					return plugin;
				}
			}
			else if (enabler.getPluginInfo().isJavascript()) {
				try {
					//javascriptPluginHelper.checkPlugin(pluginName, scriptFile);
					plugin = javascriptPluginHelper.createPluginWithEnabler(enabler);
				} catch (PluginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (plugin == null) {
					Log.e(TAG, "The javascript plugin could not be created");
				}
				else {
					plugins.put(enabler.getName(), plugin);
					Log.i(TAG, "Javascript plugin for enable " + enabler.getName() + 
							" added to the list of plugins");
				}
				return plugin;
			}
			else {
				Log.e(TAG, "Unsupported type of plugin");
				return null;
			}
		}
		return plugin;
	}

	public Plugin getConfiguredPlugin(Enabler enabler) throws PluginException {
		Plugin plugin = getPlugin(enabler);
		if (plugin == null) {
			return plugin;
		}
		if (plugin.needConfiguration()) {
			if (plugin.isConfigured()) {
				if (!plugin.compareConfiguration(enabler.getPluginConfig())) {
					plugin.configure(enabler.getPluginConfig());
				}
				Log.d(TAG, "No need to reconfigure the plugin with the same configuration");
			}
			else {
				Log.d(TAG, "Trying to configure the plugin with configuration " + enabler.getPluginConfig());
				plugin.configure(enabler.getPluginConfig());
			}
			return plugin;
		}
		else {
			//No need to do anything if the plugin does not need to be configured
			return plugin;
		}
	}
	
	public void setJavascriptPluginHelper(JavascriptPluginHelper javascriptPluginHelper) {
		if (javascriptPluginHelper == null) {
			Log.w(TAG, "Be careful, you are unsetting the javascript plugin helper.");
		}
		this.javascriptPluginHelper = javascriptPluginHelper;
	}

	public void setNativePluginHelper(NativePluginHelper nativePluginHelper) {
		if (nativePluginHelper == null) {
			Log.w(TAG, "Be careful, you are unsetting the native plugin helper.");
		}
		this.nativePluginHelper = nativePluginHelper;
	}

	public void checkNativePlugin(String serviceName, String packageName, File file) throws PluginException {
		if (nativePluginHelper != null) {
			nativePluginHelper.checkPlugin(serviceName, packageName, file);
		}
		else {
			Log.e(TAG, "Cannot check the native plugin if the helper is null");
		}
	}

	public void checkJavacriptPlugin(String pluginName, String pluginScript) throws PluginException {
		if (javascriptPluginHelper != null) {
			javascriptPluginHelper.checkPlugin(pluginName, pluginScript);
		}
		else {
			Log.e(TAG, "Cannot check the javascript plugin if the helper is null");
		}
	}

	public void removeAllPlugins() {
		for(Iterator<Map.Entry<String, Plugin>> it = plugins.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Plugin> entry = it.next();
			try {
				entry.getValue().close();
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			it.remove();
		}
	}

	public Plugin removePlugin(Enabler enabler) {
		Plugin plugin = plugins.get(enabler.getName());
		try {
			if (plugin != null) {
				plugin.close();
			}
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return plugins.remove(enabler.getName());

	}

	public void removeAllPluginsForPluginInfo(PluginInfo pluginInfo) {
		for(Iterator<Map.Entry<String, Plugin>> it = plugins.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Plugin> entry = it.next();
			Enabler enabler = IotHubDataAccess.getInstance().getEnabler(entry.getKey());
			if (enabler != null && enabler.getPluginInfo().equals(pluginInfo)) {
				try {
					entry.getValue().close();
				} catch (PluginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				it.remove();
			}
		}
	}
}
