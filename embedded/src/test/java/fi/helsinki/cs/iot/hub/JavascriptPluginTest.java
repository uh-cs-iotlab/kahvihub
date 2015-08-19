/*
 * fi.helsinki.cs.iot.hub.JavascriptPluginTest
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.JavascriptPluginHelperImpl;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;

public class JavascriptPluginTest {
	
	private Path libdir;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		libdir = Paths.get(System.getProperty("user.dir"));
		PluginManager.getInstance().setJavascriptPluginHelper(
				new JavascriptPluginHelperImpl(libdir));
	}

	private Plugin makePluginNeedConfiguration(String pluginName, int type) {
		PluginManager.getInstance().removeAllPlugins();
		String enablerName = "MyPluginEnabler";
		String pluginScript = String.format("var %s = {};", pluginName);
		String[] methods = {"needConfiguration", "checkConfiguration", "configure", "isFeatureSupported", "isFeatureAvailable",
				"isFeatureReadable", "isFeatureWritable", "getNumberOfFeatures", "getFeatureDescription", 
				"getFeatureValue", "postFeatureValue"};
		for (String method : methods) {
			if (method.equals("needConfiguration")) {
				switch (type) {
				case 1:
					// Set to a function
					pluginScript += String.format("%s.%s = function () {return true; };", pluginName, method);
					break;
				case 2:
					// Set to a string
					pluginScript += String.format("%s.%s = \"yes\";", pluginName, method);
				case 3:
					// Set to a int
					pluginScript += String.format("%s.%s = 1;", pluginName, method);
					break;
				case 4:
					// Set to a bool
					pluginScript += String.format("%s.%s = true;", pluginName, method);
					break;
				case 5:
					// Set to a string which works
					pluginScript += String.format("%s.%s = \"false\";", pluginName, method);
					break;
				default:
					//UNSET
					pluginScript += String.format("%s.%s = null;", pluginName, method);
					break;
				}
			}
			else if (method.equals("checkConfiguration")) {
				pluginScript += String.format("%s.%s = function(config) { var jconf = JSON.parse(config); return \"good config\" == jconf.c;};", pluginName, method);
			}
			else {
				pluginScript += String.format("%s.%s = null;", pluginName, method);
			}
		}
		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp", libdir.toFile());
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getName());
		assertNotNull(info);
		Enabler enabler = new Enabler(1, enablerName, null, info, null);
		assertNotNull(enabler);
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler);
		assertNotNull(plugin);
		return plugin;
	}

	@Test
	public final void testNeedConfiguration() {
		String pluginName = "MyPlugin";
		for (int i = 0; i <= 5; i++) {
			Plugin plugin = makePluginNeedConfiguration(pluginName, i);
			try {
				boolean result = plugin.needConfiguration();
				switch (i) {
				case 4:
					assertTrue(result);
					break;
				case 5:
					assertFalse(result);
					break;
				default:
					assertFalse(true);
					break;
				}
			} catch (PluginException e) {
				switch (i) {
				case 0:
					assertEquals(String.format("The method %s.needConfiguration does not provide a boolean value (returned null)", pluginName), e.getMessage());
					break;
				case 1:
				case 2:
				case 3:
					assertTrue(e.getMessage().startsWith(
							String.format("The method %s.needConfiguration does not provide a boolean value (", 
									pluginName)));
					break;
				default:
					fail(e.getMessage());
					break;
				}
			}
		}
	}

	@Test
	public final void testConfiguration() {
		String pluginName = "MyPlugin";
		Plugin plugin = makePluginNeedConfiguration(pluginName, 4);
		try {
			assertTrue(plugin.needConfiguration());
			assertTrue(plugin.compareConfiguration(null));
			String badConfig = "{\"c\": \"bad config\"}";
			String goodConfig = "{\"c\": \"good config\"}";
			assertFalse(plugin.configure(badConfig));
			assertFalse(plugin.isConfigured());
			assertTrue(plugin.compareConfiguration(null));
			assertFalse(plugin.compareConfiguration(badConfig));
			assertFalse(plugin.compareConfiguration(goodConfig));
			assertTrue(plugin.configure(goodConfig));
			assertTrue(plugin.isConfigured());
			assertFalse(plugin.compareConfiguration(null));
			assertFalse(plugin.compareConfiguration(badConfig));
			assertTrue(plugin.compareConfiguration(goodConfig));
			assertFalse(plugin.configure(badConfig));
			assertTrue(plugin.isConfigured());
		} catch (PluginException e) {
			fail(e.getMessage());
		}
	}

	private Plugin makePlugin(String pluginName) {
		PluginManager.getInstance().removeAllPlugins();
		String enablerName = "MyPluginEnabler";
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
		pluginScript += String.format("%s.getNumberOfFeatures = function() { return 1; };", pluginName);
		pluginScript += String.format("%s.getFeatureDescription = function(index) { if (this.config) { return JSON.stringify({name: this.config.name, type: \"whatever\"});}};", pluginName);

		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp", libdir.toFile());
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getName());
		assertNotNull(info);
		Enabler enabler = new Enabler(1, enablerName, null, info, null);
		assertNotNull(enabler);
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler);
		assertNotNull(plugin);
		return plugin;
	}

	@Test
	public final void testBasicPluginFunctions() {
		String pluginName = "MyPlugin";
		String featureName = "MyPluginFeature";
		Plugin plugin = makePlugin(pluginName);
		assertNotNull(plugin);
		JSONObject config = new JSONObject();
		try {
			config.put("name", featureName);
			assertFalse(plugin.configure(config.toString()));
			config.put("isSupported", true);
			config.put("isAvailable", false);
			config.put("isReadable", false);
			config.put("isWritable", true);
			config.put("getNumberOfFeatures", 1);
			config.put("getFeatureDescription", featureName);
			assertTrue(plugin.configure(config.toString()));
			FeatureDescription fd = new FeatureDescription(featureName, null);
			assertTrue(plugin.isSupported(fd));
			assertFalse(plugin.isAvailable(fd));
			assertFalse(plugin.isReadable(fd));
			assertTrue(plugin.isWritable(fd));
			FeatureDescription fdout = plugin.getFeatureDescription(0);
			assertNotNull(fdout);
			assertEquals(featureName, fdout.getName());
			assertEquals(1, plugin.getNumberOfFeatures());
		}
		catch (JSONException | PluginException e) {
			fail(e.getMessage());
		}
	}

	private class GCalServer extends NanoHTTPD {

		final String uri;
		boolean hasConcurrentEvent;
		final String key;

		GCalServer(String calId, String calKey, boolean hasConcurrentEvent) {
			super("127.0.0.1", 8111);
			this.uri = "/calendar/v3/calendars/" + calId + "/events";
			this.key = calKey;
			this.hasConcurrentEvent = hasConcurrentEvent;
		}

		String getFormattedDate(int addedHours, int addedMinutes) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
			Date now = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.add(Calendar.HOUR, addedHours);
			cal.add(Calendar.MINUTE, addedMinutes);
			return dateFormat.format(cal.getTime());
		}

		JSONObject getEvent(int startAddedHours, int startAddedMinutes, int endAddedHours, int endAddedMinutes) throws JSONException {
			JSONObject event = new JSONObject();
			JSONObject start = new JSONObject();
			start.put("dateTime", getFormattedDate(startAddedHours, startAddedMinutes));
			JSONObject end = new JSONObject();
			end.put("dateTime", getFormattedDate(endAddedHours, endAddedMinutes));
			event.put("start", start);
			event.put("end", end);
			return event;
		}

		JSONObject getJsonResponse() throws JSONException {
			JSONObject json = new JSONObject();
			JSONArray items = new JSONArray();

			JSONObject oneHourPassedEvent = getEvent(-2, 0, -1, 0);
			items.put(oneHourPassedEvent);

			if (hasConcurrentEvent) {
				JSONObject concurrentEvent = getEvent(0, -30, 0, 30);
				items.put(concurrentEvent);
			}

			JSONObject oneHourToComeEvent = getEvent(1, 0, 2, 0);
			items.put(oneHourToComeEvent);

			json.put("items", items);
			return json;
		}

		@Override
		public Response serve(IHTTPSession session) {
			if (!session.getUri().equals(uri) || !session.getParms().containsKey("key") || !session.getParms().get("key").equals(key)) {
				return new Response(Response.Status.BAD_REQUEST, "text/plain", "Bad request");
			}
			try {
				JSONObject json = getJsonResponse();
				Response response = new Response(Response.Status.OK, "application/json", json.toString());
				return response;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new Response(Response.Status.NOT_FOUND, "text/plain", "Not found");
			}
		}
	}

	private Plugin getGCalPlugin(String enablerName) {
		PluginManager.getInstance().removeAllPlugins();
		String pluginName = "GCalPlugin";
		String pluginScript = String.format("var %s = {};", pluginName);

		//My plugin needConfiguration
		pluginScript += String.format("%s.needConfiguration = true;", pluginName);
		//Set config to null
		pluginScript += String.format("%s.config = null;", pluginName);
		//The configuration is an json object with calId and calKey
		String checkConfigurationFunction = "function(data) { "
				+ " var config = JSON.parse(data); "
				+ " if (typeof(config.calId) !== 'string') { return false; }"
				+ " if (typeof(config.calKey) !== 'string') { return false; }"
				+ " return true; }";
		pluginScript += String.format("%s.checkConfiguration = %s;", pluginName, checkConfigurationFunction);
		pluginScript += String.format("%s.configure = function(config) { if (this.checkConfiguration(config)) {this.config = JSON.parse(config);}};", pluginName);
		pluginScript += String.format("%s.isFeatureSupported = function(name) { return (this.config && name == \"events\");};", pluginName);
		pluginScript += String.format("%s.isFeatureAvailable = this.isFeatureSupported;", pluginName);
		pluginScript += String.format("%s.isFeatureReadable = this.isFeatureSupported;", pluginName);
		pluginScript += String.format("%s.isFeatureWritable = function(name) { return false; };", pluginName);
		pluginScript += String.format("%s.postFeatureValue = function(name, data) { return false; };", pluginName);
		pluginScript += String.format("%s.getNumberOfFeatures = function() { return 1; };", pluginName);
		pluginScript += String.format("%s.getFeatureDescription = function(index) { if (index == 0) { return JSON.stringify({name: \"events\", type: \"TimePeriod\"});}};", pluginName);

		String processResults = "function (res) {"
				+ " var jres = JSON.parse(res);"
				+ " var array = [];"
				+ " if (typeof(jres.items) !== 'undefined') { "
				+ " var index; "
				+ " for	(index = 0; index < jres.items.length; index++) {"
				+ " var item = jres.items[index];"
				+ " var s = {date: {time: item.start.dateTime, format: \"RFC 3339\"}};"
				+ " var e = {date: {time: item.end.dateTime, format: \"RFC 3339\"}};"
				+ " array[array.length] = {period: {start: s, end: e}};"
				+ "	}"
				+ " }"
				+ " return array;}";

		String getValueFunction = "function (name) {"
				+ " if ( name !== \"events\") { return null; }"
				+ " var url = \"http://127.0.0.1:8111/calendar/v3/calendars/\" + this.config.calId + \"/events?key=\" + this.config.calKey;"
				+ " var xhr = XMLHttpRequest();"
				+ " var res; "
				+ " xhr.open('GET', url, true);"
				+ " xhr.onreadystatechange = function (event) { if (xhr.status == 200) { res = xhr.responseText; }};"
				+ " xhr.send(null);"
				+ " var processResults = " + processResults + ";"
				+ " return JSON.stringify(processResults(res));}";
		pluginScript += String.format("%s.getFeatureValue = %s;", pluginName, getValueFunction);

		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp", libdir.toFile());
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getName());
		assertNotNull(info);
		Enabler enabler = new Enabler(1, enablerName, null, info, null);
		assertNotNull(enabler);
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler);
		assertNotNull(plugin);
		return plugin;
	}

	@Test
	public final void testGetValue() {
		String id = "myCalId";
		String key = "myCalKey";
		String enablerName = "GCalEnablerRoom1";
		GCalServer server = new GCalServer(id, key, true);
		try {
			server.start();
			Plugin gcalPlugin = getGCalPlugin(enablerName);
			assertNotNull(gcalPlugin);
			JSONObject config = new JSONObject();
			config.put("calId", id);
			config.put("calKey", key);
			assertTrue(gcalPlugin.configure(config.toString()));
			FeatureDescription fd = new FeatureDescription("events", "TimePeriod");
			String res = gcalPlugin.getValue(fd);
			assertNotNull(res);
			JSONArray array = new JSONArray(res);
			assertEquals(3, array.length());
			server.hasConcurrentEvent = false;
			res = gcalPlugin.getValue(fd);
			assertNotNull(res);
			array = new JSONArray(res);
			assertEquals(2, array.length());
			server.stop();
		} catch (IOException | JSONException | PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private class SimpleHelvarnetRouter {
		List<String> msgReceivedByServer = new ArrayList<String>();
		boolean stopTheThread;
		final int port;
		final Map<String, String> queriesReplies;

		static final String QUERY_GROUPS = ">V:2,C:165#";
		static final String QUERY_GROUP_ROOM_1 = ">V:2,C:164,G:1#";
		static final String QUERY_GROUP_ROOM_2 = ">V:2,C:164,G:2#";
		static final String QUERY_DEVICE_TYPE_LUM_ROOM_1 = ">V:2,C:104,@1.1.1.1#";
		static final String QUERY_DEVICE_TYPE_LUM_ROOM_2 = ">V:2,C:104,@1.1.2.1#";
		static final String QUERY_DEVICE_TYPE_BUTTONS_ROOM_1 = ">V:2,C:104,@1.1.1.61#";
		static final String QUERY_DEVICE_TYPE_BUTTONS_ROOM_2 = ">V:2,C:104,@1.1.2.61#";
		static final String QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_1 = ">V:2,C:104,@1.1.1.62#";
		static final String QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_2 = ">V:2,C:104,@1.1.2.62#";
		static final String QUERY_DEVICE_DESCRIPTION_LUM_ROOM_1 = ">V:2,C:106,@1.1.1.1#";
		static final String QUERY_DEVICE_DESCRIPTION_LUM_ROOM_2 = ">V:2,C:106,@1.1.2.1#";
		static final String QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_1 = ">V:2,C:106,@1.1.1.61#";
		static final String QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_2 = ">V:2,C:106,@1.1.2.61#";
		static final String QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_1 = ">V:2,C:106,@1.1.1.62#";
		static final String QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_2 = ">V:2,C:106,@1.1.2.62#";
		static final String QUERY_DEVICE_STATE_LUM_ROOM_1 = ">V:2,C:110,@1.1.1.1#";
		static final String QUERY_DEVICE_STATE_LUM_ROOM_2 = ">V:2,C:110,@1.1.2.1#";
		static final String QUERY_DEVICE_STATE_BUTTONS_ROOM_1 = ">V:2,C:110,@1.1.1.61#";
		static final String QUERY_DEVICE_STATE_BUTTONS_ROOM_2 = ">V:2,C:110,@1.1.2.61#";
		static final String QUERY_DEVICE_STATE_MULTISENSOR_ROOM_1 = ">V:2,C:110,@1.1.1.62#";
		static final String QUERY_DEVICE_STATE_MULTISENSOR_ROOM_2 = ">V:2,C:110,@1.1.2.62#";
		static final String FADE_IN_LUM_ROOM_1 = ">V:2,C:14,L:100,F:200,@1.1.1.1#";
		static final String FADE_IN_LUM_ROOM_2 = ">V:2,C:14,L:100,F:200,@1.1.2.1#";
		static final String FADE_OUT_LUM_ROOM_1 = ">V:2,C:14,L:10,F:200,@1.1.1.1#";
		static final String FADE_OUT_LUM_ROOM_2 = ">V:2,C:14,L:10,F:200,@1.1.2.1#";

		SimpleHelvarnetRouter(String address, int port) {
			this.port = port;
			this.stopTheThread = false;
			this.queriesReplies = new HashMap<>();
			this.queriesReplies.put(QUERY_GROUPS, "?V:2,C:165=1,2#");
			this.queriesReplies.put(QUERY_GROUP_ROOM_1, "?V:2,C:164,G:1=@1.1.1.1,@1.1.1.61,@1.1.1.62#");
			this.queriesReplies.put(QUERY_GROUP_ROOM_2, "?V:2,C:164,G:2=@1.1.2.1,@1.1.2.61,@1.1.2.62#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_LUM_ROOM_1, "?V:2,C:104,@1.1.1.1=1537#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_LUM_ROOM_2, "?V:2,C:104,@1.1.2.1=1537#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_BUTTONS_ROOM_1, "?V:2,C:104,@1.1.1.1=1265666#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_BUTTONS_ROOM_2, "?V:2,C:104,@1.1.2.1=1265666#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_1, "?V:2,C:104,@1.1.1.1=3220738#");
			this.queriesReplies.put(QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_2, "?V:2,C:104,@1.1.2.1=3220738#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_LUM_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/LED luminaire#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_LUM_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/LED luminaire#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/7-button#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/7-button#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/Multisensor 312#");
			this.queriesReplies.put(QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/Multisensor 312#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_LUM_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_LUM_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_BUTTONS_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_BUTTONS_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_MULTISENSOR_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			this.queriesReplies.put(QUERY_DEVICE_STATE_MULTISENSOR_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			this.queriesReplies.put(FADE_IN_LUM_ROOM_1, "");
			this.queriesReplies.put(FADE_IN_LUM_ROOM_2, "");
			this.queriesReplies.put(FADE_OUT_LUM_ROOM_1, "");
			this.queriesReplies.put(FADE_OUT_LUM_ROOM_2, "");
		}

		class SimpleHelvarnetServerThread implements Runnable {

			private Socket socket;

			public SimpleHelvarnetServerThread(Socket s) throws IOException {
				this.socket = s;
			}

			@Override
			public void run() {
				// do something with in and out
				if (stopTheThread) {
					return;
				}
				try {
					PrintWriter out =
							new PrintWriter(socket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					String inputLine = in.readLine();
					if (inputLine != null) {
						msgReceivedByServer.add(inputLine);
						String answer = queriesReplies.get(inputLine);
						if (answer != null) {
							//If the answer is now empty I need to provide the answer
							if(!answer.isEmpty()) {
								out.println(answer);
							}
						}
						else {
							//Send the message about 
							if (inputLine.length() > 0 && (inputLine.startsWith("?") || inputLine.startsWith(">"))) {
								out.println("!" + inputLine.substring(1));
							}
							else {
								out.println("!" + inputLine);
							}
						}
					}
					out.close();
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		}

		void start() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					final ExecutorService service = Executors.newCachedThreadPool();
					try {
						ServerSocket serverSocket = new ServerSocket(port);
						while(!stopTheThread) {
							Socket socket = serverSocket.accept();
							service.submit(new SimpleHelvarnetServerThread(socket));
						}
						serverSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						fail(e.getMessage());
					}
				}
			}).start();
		}

		void stop() throws IOException {
			this.stopTheThread = true;
		}

		List<String> getMsgReceivedByServer() {
			return msgReceivedByServer;
		}

		@Override
		public String toString() {
			String msgList = "messages received = [ ";
			for(String msg : getMsgReceivedByServer()) {
				msgList += String.format("'%s'\n", msg);
			}
			return msgList + "]";
		}
	}

	private Plugin getHelvarnetPlugin(String enablerName) {
		PluginManager.getInstance().removeAllPlugins();
		String pluginName = "HelvarnetPlugin";
		String pluginScript = String.format("var %s = {};", pluginName);

		//My plugin needConfiguration
		pluginScript += String.format("%s.needConfiguration = true;", pluginName);
		//Set config to null
		pluginScript += String.format("%s.config = null;", pluginName);
		//The configuration is an json object with calId and calKey
		String checkConfigurationFunction = "function(data) { "
				+ " var config = JSON.parse(data); "
				+ " if (typeof(config.address) !== 'string') { return false; }"
				+ " if (typeof(config.port) !== 'number') { return false; }"
				+ " return true; }";
		pluginScript += String.format("%s.checkConfiguration = %s;", pluginName, checkConfigurationFunction);
		pluginScript += String.format("%s.configure = function(config) { if (this.checkConfiguration(config)) {this.config = JSON.parse(config);}};", pluginName);

		String isValidAnswer = "function (answer) {return answer.slice(1) !== \"!\";};";
		String getAnswers = "function (answer) { var isValidAnswer = " +isValidAnswer + " if(!isValidAnswer(answer)) {return [];}"
				+ "var indexEqual = answer.lastIndexOf(\"=\");"
				+ "var sub = answer.slice(indexEqual + 1, -1);"
				+ "return sub.split(\",\");"
				+ "};";

		String checkFeatureStatus = "function(address) {"
				+ " if (!this.config) { return false; }"
				+ " var tcp = TCPRequest();"
				+ " var res;"
				+ " tcp.connect(this.config.address, this.config.port);"
				+ " tcp.onreceive = function (msg) { var str = \"!\"; res = msg.slice(0, str.length) == str;}; "
				+ " tcp.onerror = function (code, msg) { res = false; };"
				+ " var command = '>V:2,C:110,@' + address + '#';"
				+ " tcp.send(command);"
				+ " tcp.close();"
				+ " return res; }";

		pluginScript += String.format("%s.isFeatureSupported = %s;", pluginName, checkFeatureStatus);
		pluginScript += String.format("%s.isFeatureAvailable = this.isFeatureSupported;", pluginName);
		pluginScript += String.format("%s.isFeatureReadable = function(name) { return false; };", pluginName);
		pluginScript += String.format("%s.isFeatureWritable = function(name) { return true; };", pluginName);
		pluginScript += String.format("%s.getFeatureValue = function(name) { return null; };", pluginName);

		String getFeatureType = "function (feature, address, port) {"
				+ "var r_ftype = TCPSocket();"
				+ "var t;"
				+ "r_ftype.connect(address, port);"
				+ "r_ftype.onreceive = function (msg) { var getAnswers = " + getAnswers + " var types = getAnswers(msg);"
				+ "if (types.length == 1) {t = types[0];}};"
				+ "r_ftype.send(\">V:2,C:104,\" + feature + \"#\");"
				+ "r_ftype.close();"
				+ "return t;"
				+ "};";

		String getFeaturesFromGroup = "function (groupId, address, port) {"
				+ "var r_group = TCPSocket();"
				+ "var features = [];"
				+ "r_group.connect(address, port);"
				+ "r_group.onreceive = function (msg) { var getAnswers = " + getAnswers + " var feat_in_group = getAnswers(msg);"
				+ "var index; var getFeatureType = " + getFeatureType
				+ "for(index = 0; index < feat_in_group.length; ++index) {"
				+ "var t = getFeatureType(feat_in_group[index], address, port);"
				+ "if (t == \"1537\") { "
				+ "var ft = {}; ft.light = {}; ft.light.luminosity = \"number\"; ft.light.fade = \"number\";"
				+ "var n = feat_in_group[index];"
				+ "features[features.length] = {name: n, type: ft};"
				+ "}"
				+ "}};"
				+ "var command = \">V:2,C:164,G:\" + groupId + \"#\";"
				+ "r_group.send(command);"
				+ "r_group.close();"
				+ "return features;"
				+ "};";

		//For the get number of features, I need to ask groups
		String getNbFeatures = "function () {"
				+ "var total = 0;"
				+ "var r_groups = TCPSocket();"
				+ "r_groups.connect(this.config.address, this.config.port);"
				+ "r_groups.onreceive = function (msg) { var getAnswers = " + getAnswers + " var groups = getAnswers(msg);"
				+ "var getFeaturesFromGroup = " + getFeaturesFromGroup
				+ "var index; for(index = 0; index < groups.length; ++index) {"
				+ "var feats_in_group = getFeaturesFromGroup(groups[index], r_groups.settings.address, r_groups.settings.port);"
				+ "total = total + feats_in_group.length;"
				+ "}};"
				+ "r_groups.send(\">V:2,C:165#\");"
				+ "r_groups.close();"
				+ "return total;"
				+ "};";

		pluginScript += String.format("%s.getNumberOfFeatures = %s", pluginName, getNbFeatures);

		//For the get number of features, I need to ask groups
		String getFeatureDescription = "function (position) {"
				+ "var allFeatures = [];"
				+ "var result;"
				+ "var r_groups = TCPSocket();"
				+ "r_groups.connect(this.config.address, this.config.port);"
				+ "r_groups.onreceive = function (msg) { var getAnswers = " + getAnswers + " var groups = getAnswers(msg);"
				+ "var getFeaturesFromGroup = " + getFeaturesFromGroup
				+ "var index; for(index = 0; index < groups.length; ++index) {"
				+ "var feats_in_group = getFeaturesFromGroup(groups[index], r_groups.settings.address, r_groups.settings.port);"
				+ "allFeatures = allFeatures.concat(feats_in_group);"
				+ "if (position < allFeatures.length) {"
				+ "result = JSON.stringify(allFeatures[position]); break;}"
				+ "}};"
				+ "r_groups.send(\">V:2,C:165#\");"
				+ "r_groups.close();"
				+ "return result;"
				+ "};";

		pluginScript += String.format("%s.getFeatureDescription = %s", pluginName, getFeatureDescription);
		
		String initializeFeatures = "function (address, port) {"
				+ "var features = {};"
				+ "var r_groups = TCPSocket();"
				+ "r_groups.connect(address, port);"
				+ "r_groups.onreceive = function (msg) { var getAnswers = " + getAnswers + " var groups = getAnswers(msg);"
				+ "var getFeaturesFromGroup = " + getFeaturesFromGroup
				+ "var index; for(index = 0; index < groups.length; ++index) {"
				+ "var feats_in_group = getFeaturesFromGroup(groups[index], r_groups.settings.address, r_groups.settings.port);"
				+ "for (var i = 0; i < feats_in_group.length; i++) {"
				+ "features[feats_in_group[i].name] = feats_in_group[i].type;"
				//+ "print('Key=' + feats_in_group[i].name);"
				+ "}"
				+ "}};"
				+ "r_groups.send(\">V:2,C:165#\");"
				+ "r_groups.close();"
				+ "return features;"
				+ "};";
		
		String getNbFt = "function(features) { var counter = 0; for (var ft in features) {counter += 1;} return counter;};";
		
		String checkObject = "function(obj, typ) {"
				+ "var res = true;"
				+ "for (var key in typ) {"
				//+ "print('Checking key:' + key + ', typeof(obj[key]):' + typeof(obj[key]) + ', typeof(typ[key]):' + typeof(typ[key]));"
				+ "if (typeof(obj[key]) == 'undefined') { return false;}"
				+ "else if (typeof(obj[key]) == 'object') { if (typeof(typ[key]) !== 'object') { return false;} else { return res && check(obj[key], typ[key]);}}"
				+ "else { res = res && typeof(obj[key]) == typ[key]; }"
				+ "}"
				+ "return res;"
				+ "};";
		
		String postFeatureValue = "function(name, data) {"
				+ "var result = true;"
				+ "var countFeatures = " + getNbFt
				+ "if (this.config && typeof(this.config.features) == 'undefined') {"
				+ "var initializeFeatures = " + initializeFeatures
				+ "this.config.features = initializeFeatures(this.config.address, this.config.port);"
				+ "if (typeof(makeConfigurationPersistant) !== 'undefined') { makeConfigurationPersistant(JSON.stringify(this.config)); }"
				+ "}"
				+ "var check = " + checkObject
				+ "var fd = this.config.features[name];"
				+ "try {"
				+ "var jdata = JSON.parse(data);"
				+ "if (check(jdata, fd)) {"
				+ "var r_post = TCPSocket();"
				+ "r_post.connect(this.config.address, this.config.port);"
				+ "r_post.onreceive = function (msg) { result = false; };"
				+ "var command = '>V:2,C:14,L:' + jdata.light.luminosity + ',F:' + jdata.light.fade + ',' + name + '#';"
				+ "r_post.send(command);"
				+ "r_post.close();"
				+ "return result;}"
				+ "return false; } catch (e) { return false;}};";
		pluginScript += String.format("%s.postFeatureValue = %s", pluginName, postFeatureValue);

		//System.out.println(pluginScript.replaceAll(";", ";\n"));

		File temp = null;
		try {
			temp = File.createTempFile("tempfile", ".tmp", libdir.toFile());
			temp.deleteOnExit();
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
			bufferedWriter.write(pluginScript);
			bufferedWriter.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		PluginInfo info = new PluginInfo(1, Type.JAVASCRIPT, pluginName, null, temp.getName());
		assertNotNull(info);
		Enabler enabler = new Enabler(1, enablerName, null, info, null);
		assertNotNull(enabler);
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler);
		assertNotNull(plugin);
		return plugin;
	}

	@Test
	public final void testPostValue() {
		int port = 50000;
		String address = "10.254.1.1";
		String enablerName = "HelvarnetEnabler";
		SimpleHelvarnetRouter simpleHelvarnetRouter = new SimpleHelvarnetRouter(address, port);
		try {
			simpleHelvarnetRouter.start();
			Plugin helvarnetPlugin = getHelvarnetPlugin(enablerName);
			assertNotNull(helvarnetPlugin);
			JSONObject config = new JSONObject();
			config.put("address", "127.0.0.1");
			config.put("port", port);
			assertTrue(helvarnetPlugin.configure(config.toString()));
			int nbFeatures = helvarnetPlugin.getNumberOfFeatures();
			assertEquals(2, nbFeatures);
			List<FeatureDescription> features = new ArrayList<>();
			for (int i = 0; i < nbFeatures; i++) {
				features.add(helvarnetPlugin.getFeatureDescription(i));
			}
			String badData = "This is not even a JSON object :)";
			for (FeatureDescription fd : features) {
				assertFalse(fd.toString(), helvarnetPlugin.postValue(fd, badData));
			}
			badData = "{\"light\": {\"luminosity\": 100}}"; //missing fade attribute
			JSONObject jobj = new JSONObject(badData);
			for (FeatureDescription fd : features) {
				assertFalse(fd.toString(), helvarnetPlugin.postValue(fd, jobj.toString()));
			}
			badData = "{\"light\": {\"luminosity\": 10, \"fade\": 100}}"; //Something the server does not understand
			jobj = new JSONObject(badData);
			for (FeatureDescription fd : features) {
				assertFalse(fd.toString(), helvarnetPlugin.postValue(fd, jobj.toString()));
			}
			String goodData = "{\"light\": {\"luminosity\": 10, \"fade\": 200}}";
			jobj = new JSONObject(goodData);
			for (FeatureDescription fd : features) {
				assertTrue(fd.toString(), helvarnetPlugin.postValue(fd, jobj.toString()));
			}
			simpleHelvarnetRouter.stop();
		} catch (JSONException | PluginException | IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public final void testPostValueRealRouter() {
		int port = 50000;
		String address = "10.254.1.1";
		String enablerName = "HelvarnetEnabler";
		try {
			Plugin helvarnetPlugin = getHelvarnetPlugin(enablerName);
			assertNotNull(helvarnetPlugin);
			JSONObject config = new JSONObject();
			config.put("address", address);
			config.put("port", port);
			assertTrue(helvarnetPlugin.configure(config.toString()));
			int nbFeatures = helvarnetPlugin.getNumberOfFeatures();
			assertEquals(2, nbFeatures);
			List<FeatureDescription> features = new ArrayList<>();
			for (int i = 0; i < nbFeatures; i++) {
				features.add(helvarnetPlugin.getFeatureDescription(i));
			}
			String goodData = "{\"light\": {\"luminosity\": 1, \"fade\": 100}}";
			JSONObject jobj = new JSONObject(goodData);
			for (FeatureDescription fd : features) {
				assertTrue(fd.toString(), helvarnetPlugin.postValue(fd, jobj.toString()));
			}
		} catch (JSONException | PluginException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
