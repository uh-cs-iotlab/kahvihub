/*
 * fi.helsinki.cs.iot.hub.IotHubHTTPDTest
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

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.kahvihub.IotHubDbHandlerSqliteJDBCImpl;

/**
 * @author mineraud
 *
 */
public class IotHubHTTPDTest {

	private IotHubHTTPD iotHubHTTPD;
	private int port = 8081;
	private Path libdir;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		libdir = Files.createTempDirectory("IotHubHTTPDTest");
		libdir.toFile().deleteOnExit();
		iotHubHTTPD = new IotHubHTTPD(port, libdir);

		File dbFile = File.createTempFile("IotHubHTTPDTest", ".db");
		dbFile.deleteOnExit();

		IotHubDataAccess.setInstance(
				new IotHubDbHandlerSqliteJDBCImpl(dbFile.getAbsolutePath(), 1, true));
		try {
			iotHubHTTPD.start();
		} catch (IOException ioe) {
			fail("Couldn't start server:\n" + ioe.getMessage());
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		iotHubHTTPD.stop();
	}

	private File makePluginFile(String pluginName) {
		String pluginScript = String.format("var %s = {};", pluginName);

		//My plugin needConfiguration
		pluginScript += String.format("%s.needConfiguration = true;", pluginName);
		//Set config to null
		pluginScript += String.format("%s.config = null;", pluginName);
		//The configuration is a dictionary of json object with fields are answering the different methods
		String checkConfigurationFunction = "function(data) { "
				//+ " print(data);"
				+ " var config = JSON.parse(data); "
				+ " if (typeof(config.name) == 'undefined') { return false; }"
				+ " if (typeof(config.value) == 'undefined') { return false; }"
				+ " if (typeof(config.isSupported) !== 'boolean') { return false; }"
				+ " if (typeof(config.isAvailable) !== 'boolean') { return false; }"
				+ " if (typeof(config.isReadable) !== 'boolean') { return false; }"
				+ " if (typeof(config.isWritable) !== 'boolean') { return false; }"
				+ " if (typeof(config.getNumberOfFeatures) !== 'number') { return false; }"
				+ " if (typeof(config.getFeatureDescription) !== 'string') { return false; }"
				+ " return true; }";
		pluginScript += String.format("%s.checkConfiguration = %s;", pluginName, checkConfigurationFunction);
		pluginScript += String.format("%s.configure = function(config) { if (this.checkConfiguration(config)) {this.config = JSON.parse(config);}};", pluginName);
		pluginScript += String.format("%s.isFeatureSupported = function(name) { if (this.config && this.config.name == name) { return this.config.isSupported;}};", pluginName);
		pluginScript += String.format("%s.isFeatureAvailable = function(name) { if (this.config && this.config.name == name) { return this.config.isAvailable;}};", pluginName);
		pluginScript += String.format("%s.isFeatureReadable = function(name) { if (this.config && this.config.name == name) { return this.config.isReadable;}};", pluginName);
		pluginScript += String.format("%s.isFeatureWritable = function(name) { if (this.config && this.config.name == name) { return this.config.isWritable;}};", pluginName);
		pluginScript += String.format("%s.getNumberOfFeatures = function() { if (this.config) { return 1; } else { return 0; } };", pluginName);
		pluginScript += String.format("%s.getFeatureDescription = function(index) { if (this.config) { return JSON.stringify({name: this.config.name, type: \"whatever\"});}};", pluginName);
		pluginScript += String.format("%s.getFeatureValue = function(name) { if (this.config && this.config.name == name) {return JSON.stringify(this.config.value);} else {return null;} };", pluginName);
		pluginScript += String.format("%s.postFeatureValue = function(name, data) { if (this.config && this.config.name == name) {"
				+ "this.config.value = JSON.parse(data); makeConfigurationPersistant(JSON.stringify(this.config)); return true;} else { return false;} };", pluginName);

		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp");
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getAbsolutePath());
		assertNotNull(info);
		return temp;
	}



	private JSONObject makeJsonObjectForPlugin(String pluginName, String packageName, BasicPluginInfo.Type type, File file, boolean isService) {
		JSONObject json = new JSONObject();
		try {
			json.put("plugin", pluginName);
			json.put("package", packageName);
			json.put("type", type.name());
			String encoded = ScriptUtils.encodeBase64FromFile(file);
			json.put("file", encoded);
			if (isService) {
				json.put("isService", true);
			}
			return json;
		} catch (JSONException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Test
	public void testPluginAPI() {
		//At this point, the list should be empty
		String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/plugins/", null);
		assertEquals("[]", res.trim());

		String pluginName = "MyPlugin";
		//now I want to had a javascript plugin
		File pluginFile = makePluginFile(pluginName);
		JSONObject jsonObject = makeJsonObjectForPlugin(pluginName, null, Type.JAVASCRIPT, pluginFile, false);
		assertNotNull(jsonObject);

		//First check the if I can had it with get
		res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/plugins/", jsonObject.toString());
		assertEquals("[]", res.trim());


		PluginInfo pluginInfo = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, null);
		res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/plugins/", jsonObject.toString());
		try {
			assertEquals(pluginInfo.toJSON().toString(), res.trim());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(1, IotHubDataAccess.getInstance().getPlugins().size());

		//Now going to delete the plugininfo
		res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("DELETE", "http://127.0.0.1:" + port + "/plugins?id=1&type=enabler", null);
		try {
			assertEquals(pluginInfo.toJSON().toString(), res.trim());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(0, IotHubDataAccess.getInstance().getPlugins().size());
	}

	@Test
	public void testEnablerAPI() {
		String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/enablers/", null);
		assertEquals("[]", res.trim());

		//Now I want to create a javascript plugin to attach to my enabler
		String pluginName = "MyPlugin";
		//now I want to had a javascript plugin
		File pluginFile = makePluginFile(pluginName);
		JSONObject jsonObject = makeJsonObjectForPlugin(pluginName, null, Type.JAVASCRIPT, pluginFile, false);
		assertNotNull(jsonObject);
		String myPluginString = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/plugins/", jsonObject.toString());
		JSONObject jPlugin = null;
		try {
			jPlugin = new JSONObject(myPluginString);
			long pluginId = jPlugin.getLong("id");
			String name = "MyEnabler";
			String metadata = "A freshly created enabler";
			JSONObject jEnabler = new JSONObject();
			jEnabler.put("plugin", pluginId);
			jEnabler.put("name", "fakeName");
			jEnabler.put("metadata", metadata);

			// I should get an enable with no features as it is not configured
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/enablers/", jEnabler.toString());
			JSONObject jexpectedEnabler = new JSONObject();
			jexpectedEnabler.put("id", 1);
			jexpectedEnabler.put("name", "fakeName");
			jexpectedEnabler.put("metadata", metadata);
			jexpectedEnabler.put("plugin", jPlugin);
			JSONArray array = new JSONArray();
			jexpectedEnabler.put("features", array);
			assertEquals(jexpectedEnabler.toString(), res.trim());

			String featureName = "MyFeature";
			JSONObject config = new JSONObject();
			config.put("name", featureName);
			config.put("isSupported", true);
			config.put("isAvailable", true);
			config.put("isReadable", true);
			config.put("isWritable", true);
			config.put("getNumberOfFeatures", 1);
			config.put("getFeatureDescription", featureName);
			config.put("value", "Uksomatonta");
			JSONObject data = new JSONObject();
			data.put("configuration", config);

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + port + "/enablers/fakeName", data.toString());
			JSONObject expectedFeature = new JSONObject();
			expectedFeature.put("id", 1);
			expectedFeature.put("isSupported", true);
			expectedFeature.put("isAtomicFeed", false);
			expectedFeature.put("name", featureName);
			expectedFeature.put("isWritable", true);
			expectedFeature.put("isReadable", true);
			expectedFeature.put("type", "whatever");
			expectedFeature.put("isAvailable", true);
			array.put(expectedFeature);
			jexpectedEnabler.put("features", array);
			jexpectedEnabler.put("config", config.toString());
			assertEquals(jexpectedEnabler.toString().length(), res.trim().length());

			//Now just change quicky the configuration
			config.put("value", "Hard to believe");
			data.put("configuration", config);
			data.put("name", name);
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + port + "/enablers/fakeName", data.toString());
			jexpectedEnabler.put("name", name);
			jexpectedEnabler.put("config", config.toString());
			assertEquals(jexpectedEnabler.toString().length(), res.trim().length());

			//Now I want to change the feature as an atomic feed
			data = new JSONObject();
			data.put("enableAsAtomicFeed", true);
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + port + "/enablers/" + name + "/" + featureName, data.toString());
			expectedFeature.put("id", 2);
			expectedFeature.put("isAtomicFeed", true);
			assertEquals(expectedFeature.toString(), res.trim());

			//Now I will check for feeds
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/feeds/", null);
			JSONArray feedArray = new JSONArray(res);
			assertEquals(1, feedArray.length());
			JSONObject feed1 = feedArray.getJSONObject(0);
			String feedName = feed1.getString("name");
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/feeds/" + feedName, null);
			assertEquals("\"Hard to believe\"", res.trim());

			JSONObject toPostToFeed = new JSONObject("{\"test\": \"Unbelievable\"}");
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/feeds/" + feedName, toPostToFeed.toString());
			assertEquals(toPostToFeed.toString(), res.trim());

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/feeds/" + feedName, null);
			assertEquals(toPostToFeed.toString(), res.trim());

			data.put("enableAsAtomicFeed", false);
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + port + "/enablers/" + name + "/" + featureName, data.toString());
			expectedFeature.put("isAtomicFeed", false);
			assertEquals(expectedFeature.toString(), res.trim());

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/feeds/" + feedName, null);
			JSONObject jerror = new JSONObject(res);
			assertEquals("Error", jerror.getString("status"));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private File makePluginFileForService(String pluginName) {
		String pluginScript = String.format("var %s = {};", pluginName);

		//My plugin needConfiguration
		pluginScript += String.format("%s.needConfiguration = true;", pluginName);
		//Set config to null
		pluginScript += String.format("%s.config = null;", pluginName);

		String checkConfigurationFunction = "function(data) { "
				//+ " print(data);"
				+ " var config = JSON.parse(data); "
				+ " if (typeof(config.value) == 'undefined') { return false; }"
				+ " return true; }";
		pluginScript += String.format("%s.checkConfiguration = %s;", pluginName, checkConfigurationFunction);
		pluginScript += String.format("%s.configure = function(config) { if (this.checkConfiguration(config)) {this.config = JSON.parse(config);}};", pluginName);
		pluginScript += String.format("%s.run = function() { setTimeout(function() {print('TEST');}, 1000);};", pluginName);

		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp");
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getAbsolutePath());
		assertNotNull(info);
		return temp;
	}


	@Test
	public void testServiceAPI() {
		String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/services/", null);
		assertEquals("[]", res.trim());

		//Now I want to create a javascript plugin to attach to my service
		String pluginName = "MyPlugin";
		//now I want to had a javascript plugin
		File pluginFile = makePluginFileForService(pluginName);
		JSONObject jsonObject = makeJsonObjectForPlugin(pluginName, null, Type.JAVASCRIPT, pluginFile, true);
		assertNotNull(jsonObject);
		String myPluginString = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/plugins/", jsonObject.toString());
		JSONObject jPlugin = null;
		try {
			jPlugin = new JSONObject(myPluginString);
			long pluginId = jPlugin.getLong("id");
			String name = "MyService";
			String metadata = "A freshly created service";
			JSONObject jservice = new JSONObject();
			jservice.put("plugin", pluginId);
			jservice.put("name", name);
			jservice.put("metadata", metadata);
			jservice.put("bootAtStartup", false);

			// I should get an enable with no features as it is not configured
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + port + "/services/", jservice.toString());
			JSONObject jexpectedService = new JSONObject();
			jexpectedService.put("id", 1);
			jexpectedService.put("name", name);
			jexpectedService.put("metadata", metadata);
			jexpectedService.put("plugin", jPlugin);
			jexpectedService.put("bootAtStartup", false);
			assertEquals(jexpectedService.toString(), res.trim());
			
			JSONObject data = new JSONObject();
			JSONObject config = new JSONObject();
			config.put("value", "Text to print");
			data.put("configuration", config);
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + port + "/services/" + name, data.toString());
			jexpectedService.put("id", 1);
			jexpectedService.put("config", config.toString());
			assertEquals(jexpectedService.toString(), res.trim());
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + port + "/services/" + name + "/start", null);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail(e.getMessage());
			}
			System.out.println(res.trim());

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
