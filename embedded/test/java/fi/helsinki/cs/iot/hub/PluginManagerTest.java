/*
 * fi.helsinki.cs.iot.hub.PluginManagerTest
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
package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;

/**
 * @author mineraud
 *
 */
public class PluginManagerTest {

	/**
	 * Test method for {@link fi.helsinki.cs.iot.hub.model.enabler.PluginManager#getInstance()}.
	 */
	@Test
	public final void testGetInstance() {
		PluginManager pluginManager = PluginManager.getInstance();
		assertNotNull(pluginManager);
	}

	/**
	 * Test method for {@link fi.helsinki.cs.iot.hub.model.enabler.PluginManager#getPlugin(fi.helsinki.cs.iot.hub.model.enabler.Enabler)}.
	 */
	@Test
	public final void testGetPlugin() {
		PluginManager.getInstance().removeAllPlugins();
		assertNull(PluginManager.getInstance().getPlugin(null));
		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp");
		} catch (IOException e) {
			fail(e.getMessage());
		} 
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, "whatever", null, temp.getAbsolutePath());
		Enabler enabler = new Enabler(1, "testEnabler", null, info, null);
		assertNotNull(enabler);
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler);
		assertNotNull(plugin);
		PluginInfo info2 = new PluginInfo(2, Type.JAVASCRIPT, "whatever2", null, temp.getAbsolutePath());
		Enabler enabler2 = new Enabler(2, "testEnabler", null, info2, null);
		Plugin plugin2 = PluginManager.getInstance().getPlugin(enabler2);
		assertEquals(plugin, plugin2);
		enabler2 = new Enabler(2, "testEnabler2", null, info2, null);
		plugin2 = PluginManager.getInstance().getPlugin(enabler2);
		assertNotEquals(plugin, plugin2);
	}


	/**
	 * Test method for {@link fi.helsinki.cs.iot.hub.model.enabler.PluginManager#checkJavacriptPlugin(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testCheckJavacriptPluginWithString() {
		String pluginScript = "var MyMispelledPlugin = {};";
		try {
			PluginManager.getInstance().checkJavacriptPlugin("MyPlugin", pluginScript);
			assertTrue(false);
		} catch (PluginException e) {
			assertEquals("Plugin MyPlugin is unknown", e.getMessage());
		}

		pluginScript = "var MyPlugin = {};";

		String[] methods = {"needConfiguration", "checkConfiguration", "configure", "isFeatureSupported", "isFeatureAvailable",
				"isFeatureReadable", "isFeatureWritable", "getNumberOfFeatures", "getFeatureDescription", 
				"getFeatureValue", "postFeatureValue"};
		for (String method : methods) {
			try {
				PluginManager.getInstance().checkJavacriptPlugin("MyPlugin", pluginScript);
				assertTrue(pluginScript,false);
			} catch (PluginException e) {
				assertEquals("The plugin MyPlugin has no method " + method, e.getMessage());
			}
			pluginScript += String.format("MyPlugin.%s = null;", method);
		}
		try {
			PluginManager.getInstance().checkJavacriptPlugin("MyPlugin", pluginScript);
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test method for {@link fi.helsinki.cs.iot.hub.model.enabler.PluginManager#getConfiguredPlugin(fi.helsinki.cs.iot.hub.model.enabler.Enabler)}.
	 */
	@Test
	public final void testGetConfiguredPlugin() {
		//fail("Not yet implemented"); // TODO
	}

}
