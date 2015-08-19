/*
 * fi.helsinki.cs.iot.kahvihub.NativePluginHelper
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
package fi.helsinki.cs.iot.kahvihub;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.NativePluginHelper;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.kahvihub.plugin.KahvihubNativePlugin;


/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class KahvihubNativePluginHelper implements NativePluginHelper {

	private static final String TAG = "KahvihubNativePluginHelper";
	private final String nativePluginsFolder;
	
	/**
	 * @param nativePluginsFolder
	 */
	public KahvihubNativePluginHelper(String nativePluginsFolder) {
		this.nativePluginsFolder = nativePluginsFolder;
	}

	@Override
	public Plugin createPlugin(Enabler enabler) {
		if (enabler == null) {
			Log.e(TAG, "The enabler is null");
		}
		else if (enabler.getPluginInfo() == null || !enabler.getPluginInfo().isNative()) {
			Log.e(TAG, "The plugin info does not permit to find the corresponding native plugin");
			return null;
		}
		try { 
			String classname = enabler.getPluginInfo().getPackageName() + "." + enabler.getPluginInfo().getServiceName();
			File pluginFile = new File(nativePluginsFolder + enabler.getPluginInfo().getFilename());
			URL[] urls = {pluginFile.toURI().toURL()};
			ClassLoader classLoader = new URLClassLoader(urls);
			Class<?> pluginClass = Class.forName(classname, true, classLoader); 

			if(KahvihubNativePlugin.class.isAssignableFrom(pluginClass)){ 
				KahvihubNativePlugin plugin = (KahvihubNativePlugin)pluginClass.newInstance();
				return new KahvihubNativePluginWrapper(plugin);
			}
			else {
				System.err.println("The provided class is not a IPlugin");
				return null;
			}
		} catch (ClassNotFoundException e1) { 
			e1.printStackTrace(); 
		} catch (InstantiationException e) { 
			e.printStackTrace(); 
		} catch (IllegalAccessException e) { 
			e.printStackTrace(); 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (AbstractMethodError e) {
			// This is needed when the plugin is not uptodate
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void checkPlugin(String serviceName, String packageName, File file)
			throws PluginException {
		try { 
			String classname = packageName + "." + serviceName;
			URL[] urls = {file.toURI().toURL()};
			ClassLoader classLoader = new URLClassLoader(urls);
			Class<?> pluginClass = Class.forName(classname, true, classLoader); 
			if(KahvihubNativePlugin.class.isAssignableFrom(pluginClass)){ 
				KahvihubNativePlugin plugin = (KahvihubNativePlugin)pluginClass.newInstance();
				plugin.plug();
			}
			else {
				throw PluginException.newNativeException("The provided class is not a KahvihubNativePlugin");
			}
		} catch (ClassNotFoundException e) { 
			e.printStackTrace(); 
			throw PluginException.newNativeException(e.getMessage());
		} catch (InstantiationException e) { 
			e.printStackTrace();
			throw PluginException.newNativeException(e.getMessage());
		} catch (IllegalAccessException e) { 
			e.printStackTrace(); 
			throw PluginException.newNativeException(e.getMessage());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw PluginException.newNativeException(e.getMessage());
		}
		catch (AbstractMethodError e) {
			// This is needed when the plugin is not uptodate
			e.printStackTrace();
			throw PluginException.newNativeException(e.getMessage());
		}
	}
}
