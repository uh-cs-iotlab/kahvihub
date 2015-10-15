package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.utils.socketserver.MultiThreadedSocketServer;
import fi.helsinki.cs.iot.hub.utils.socketserver.SocketProtocol;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.kahvihub.database.sqliteJdbc.IotHubDbHandlerSqliteJDBCImpl;

public class LondonIcc2015Test {

	private class GCalServer extends NanoHTTPD {

		class GCalItem {
			String id;
			String key;
			boolean isConcurrent;
			public GCalItem(String id, String key, boolean isConcurrent) {
				super();
				this.id = id;
				this.key = key;
				this.isConcurrent = isConcurrent;
			}
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + ((id == null) ? 0 : id.hashCode());
				result = prime * result + ((key == null) ? 0 : key.hashCode());
				return result;
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				GCalItem other = (GCalItem) obj;
				if (!getOuterType().equals(other.getOuterType()))
					return false;
				if (id == null) {
					if (other.id != null)
						return false;
				} else if (!id.equals(other.id))
					return false;
				if (key == null) {
					if (other.key != null)
						return false;
				} else if (!key.equals(other.key))
					return false;
				return true;
			}
			private GCalServer getOuterType() {
				return GCalServer.this;
			}
		}
		
		Map<String, GCalItem> calendars;
		GCalServer(int port) {
			super("127.0.0.1", port);
			this.calendars = new HashMap<>();
		}
		
		String getUri(GCalItem calendar) {
			return "/calendar/v3/calendars/" + calendar.id + "/events";
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

		JSONObject getJsonResponse(GCalItem calItem) throws JSONException {
			JSONObject json = new JSONObject();
			JSONArray items = new JSONArray();

			JSONObject oneHourPassedEvent = getEvent(-2, 0, -1, 0);
			items.put(oneHourPassedEvent);

			if (calItem.isConcurrent) {
				JSONObject concurrentEvent = getEvent(0, -30, 0, 30);
				items.put(concurrentEvent);
			}

			JSONObject oneHourToComeEvent = getEvent(1, 0, 2, 0);
			items.put(oneHourToComeEvent);

			json.put("items", items);
			return json;
		}
		
		public void add(String gcalId, String gcalKey, boolean isConcurrent) {
			this.calendars.put(gcalId, new GCalItem(gcalId, gcalKey, isConcurrent));
		}

		@Override
		public Response serve(IHTTPSession session) {
			
			for (GCalItem calItem : calendars.values()) {
				String uri = getUri(calItem);
				if(session.getUri().equals(uri)) {
					if (!session.getParms().containsKey("key") || !session.getParms().get("key").equals(calItem.key)) {
						return new Response(Response.Status.BAD_REQUEST, "text/plain", "Bad request");
					}
					try {
						JSONObject json = getJsonResponse(calItem);
						Response response = new Response(Response.Status.OK, "application/json", json.toString());
						return response;
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return new Response(Response.Status.BAD_REQUEST, "text/plain", "Bad request");
					}
				}
			}
			return new Response(Response.Status.NOT_FOUND, "text/plain", "Not found");			
		}
	}
	
	private class HelvarBoxProtocol implements SocketProtocol {

		List<String> msgReceived;
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
		static final String FADE_IN_LUM_ROOM_1 = ">V:2,C:14,L:10,F:200,@1.1.1.1#";
		static final String FADE_IN_LUM_ROOM_2 = ">V:2,C:14,L:10,F:200,@1.1.2.1#";
		static final String FADE_OUT_LUM_ROOM_1 = ">V:2,C:14,L:100,F:200,@1.1.1.1#";
		static final String FADE_OUT_LUM_ROOM_2 = ">V:2,C:14,L:100,F:200,@1.1.2.1#";
		
		public HelvarBoxProtocol() {
			msgReceived = new ArrayList<String>();
			queriesReplies = new HashMap<>();
			queriesReplies.put(QUERY_GROUPS, "?V:2,C:165=1,2#");
			queriesReplies.put(QUERY_GROUP_ROOM_1, "?V:2,C:164,G:1=@1.1.1.1,@1.1.1.61,@1.1.1.62#");
			queriesReplies.put(QUERY_GROUP_ROOM_2, "?V:2,C:164,G:2=@1.1.2.1,@1.1.2.61,@1.1.2.62#");
			queriesReplies.put(QUERY_DEVICE_TYPE_LUM_ROOM_1, "?V:2,C:104,@1.1.1.1=1537#");
			queriesReplies.put(QUERY_DEVICE_TYPE_LUM_ROOM_2, "?V:2,C:104,@1.1.2.1=1537#");
			queriesReplies.put(QUERY_DEVICE_TYPE_BUTTONS_ROOM_1, "?V:2,C:104,@1.1.1.1=1265666#");
			queriesReplies.put(QUERY_DEVICE_TYPE_BUTTONS_ROOM_2, "?V:2,C:104,@1.1.2.1=1265666#");
			queriesReplies.put(QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_1, "?V:2,C:104,@1.1.1.1=3220738#");
			queriesReplies.put(QUERY_DEVICE_TYPE_MULTISENSOR_ROOM_2, "?V:2,C:104,@1.1.2.1=3220738#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_LUM_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/LED luminaire#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_LUM_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/LED luminaire#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/7-button#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_BUTTONS_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/7-button#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_1, "?V:2,C:106,@1.1.1.1=Meeting Room 1/Multisensor 312#");
			queriesReplies.put(QUERY_DEVICE_DESCRIPTION_MULTISENSOR_ROOM_2, "?V:2,C:106,@1.1.2.1=Meeting Room 2/Multisensor 312#");
			queriesReplies.put(QUERY_DEVICE_STATE_LUM_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			queriesReplies.put(QUERY_DEVICE_STATE_LUM_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			queriesReplies.put(QUERY_DEVICE_STATE_BUTTONS_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			queriesReplies.put(QUERY_DEVICE_STATE_BUTTONS_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			queriesReplies.put(QUERY_DEVICE_STATE_MULTISENSOR_ROOM_1, "?V:2,C:110,@1.1.1.1=0#");
			queriesReplies.put(QUERY_DEVICE_STATE_MULTISENSOR_ROOM_2, "?V:2,C:110,@1.1.2.1=0#");
			queriesReplies.put(FADE_IN_LUM_ROOM_1, "");
			queriesReplies.put(FADE_IN_LUM_ROOM_2, "");
			queriesReplies.put(FADE_OUT_LUM_ROOM_1, "");
			queriesReplies.put(FADE_OUT_LUM_ROOM_2, "");
		}
		
		@Override
		public String processInput(String input) {
			String answer = queriesReplies.get(input);
			if (input != null) {
				msgReceived.add(input);
			}
			if (answer != null) {
				return answer;
			}
			else {
				if (input == null) {
					return null;
				}
				//Send the message about 
				else if (input.length() > 0 && (input.startsWith("?") || input.startsWith(">"))) {
					return "!" + input.substring(1);
				}
				else {
					return "!" + input;
				}
			}
		}

		public List<String> getMsgReceived() {
			return msgReceived;
		}
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
	public void test() {
		String gcalKey = "myCalKey";
		int gcalPort = 8082;
		int helvarPort = 50000;
		int iothubPort = 8081;
		String gCalPluginResourceName = "/gCalPlugin.js";
		String gCalPluginName = "GCalPlugin";
		String helvarPluginResourceName = "/helvarnetPlugin.js";
		String helvarPluginName = "HelvarnetPlugin";
		String lastCallLightPluginName = "LastCallLight";
		String lastCallLightPluginResourceName = "/lastCallLight.js";

		try {
			GCalServer gcalServer = new GCalServer(gcalPort);
			gcalServer.add("room1", gcalKey, false);
			gcalServer.add("room2", gcalKey, true);
			HelvarBoxProtocol helvarBoxProtocol = new HelvarBoxProtocol();
			MultiThreadedSocketServer helvarRouter = new MultiThreadedSocketServer(helvarBoxProtocol, helvarPort);
			
			Path iothubLibdir = Files.createTempDirectory("IotHubHTTPDTest");
			iothubLibdir.toFile().deleteOnExit();
			IotHubHTTPD iotHubHTTPD = new IotHubHTTPD(iothubPort, iothubLibdir);

			File dbFile = File.createTempFile("IotHubHTTPDTest", ".db");
			dbFile.deleteOnExit();

			IotHubDataAccess.setInstance(
					new IotHubDbHandlerSqliteJDBCImpl(dbFile.getAbsolutePath(), 1, true));

			//Start all the servers
			gcalServer.start();
			helvarRouter.start();
			iotHubHTTPD.start();

			//First I need to install the gcalPlugin
			File gCalPluginFile = new File(LondonIcc2015Test.class.getResource(gCalPluginResourceName).toURI());
			assertTrue(gCalPluginFile.exists());

			String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/plugins/", 
					makeJsonObjectForPlugin(gCalPluginName, null, Type.JAVASCRIPT, gCalPluginFile, false).toString());
			
			JSONObject resultPostGcalPlugin = new JSONObject(res);
			assertEquals(1, resultPostGcalPlugin.getInt("id"));
			assertEquals(gCalPluginName, resultPostGcalPlugin.getString("service"));
			assertEquals("JAVASCRIPT", resultPostGcalPlugin.getString("pluginType"));
			
			//Now I will add two enablers, one for each meeting room
			JSONObject enablerGcal = new JSONObject();
			enablerGcal.put("plugin", 1);
			enablerGcal.put("name", "CalRoom1");
			enablerGcal.put("metadata", "An enabler for the calendar of meeting room 1");
			JSONObject config = new JSONObject();
			config.put("server", "http://127.0.0.1:" + gcalPort);
			config.put("calId", "room1");
			config.put("calKey", gcalKey);
			enablerGcal.put("configuration", config);

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/enablers/", enablerGcal.toString());
			JSONObject resultPostGcalEnabler = new JSONObject(res);
			assertEquals(1, resultPostGcalEnabler.getInt("id"));
			assertEquals("CalRoom1", resultPostGcalEnabler.getString("name"));
			assertEquals(1, resultPostGcalEnabler.getJSONArray("features").length());
			//TODO, I would need to have more checks on the Json value returned in the future
			JSONObject gCalFeatureToFeed = new JSONObject();
			gCalFeatureToFeed.put("enableAsAtomicFeed", true);
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + iothubPort + "/enablers/CalRoom1/events", gCalFeatureToFeed.toString());
			JSONObject expectedFeature = new JSONObject(res);
			assertEquals(1, expectedFeature.getInt("id"));
			assertTrue(expectedFeature.getBoolean("isAtomicFeed"));
			
			//Now the second enabler for the room 2
			enablerGcal.put("plugin", 1);
			enablerGcal.put("name", "CalRoom2");
			enablerGcal.put("metadata", "An enabler for the calendar of meeting room 2");
			config.put("calId", "room2");
			config.put("calKey", gcalKey);
			enablerGcal.put("configuration", config);
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/enablers/", enablerGcal.toString());
			resultPostGcalEnabler = new JSONObject(res);
			assertEquals(2, resultPostGcalEnabler.getInt("id"));
			assertEquals("CalRoom2", resultPostGcalEnabler.getString("name"));
			assertEquals(1, resultPostGcalEnabler.getJSONArray("features").length());
			//TODO, I would need to have more checks on the Json value returned in the future
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + iothubPort + "/enablers/CalRoom2/events", gCalFeatureToFeed.toString());
			expectedFeature = new JSONObject(res);
			assertEquals(2, expectedFeature.getInt("id"));
			assertTrue(expectedFeature.getBoolean("isAtomicFeed"));
			
			//Now we check our atomic feeds
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/feeds", null);
			JSONArray feedArray = new JSONArray(res);
			assertEquals(2, feedArray.length());
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/feeds/atomicFeature1", null);
			JSONArray eventArray = new JSONArray(res);
			assertEquals(2, eventArray.length());
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/feeds/atomicFeature2", null);
			eventArray = new JSONArray(res);
			assertEquals(3, eventArray.length());
			
			//Now we need to install the helvarNet plugin
			File helvarPluginFile = new File(LondonIcc2015Test.class.getResource(helvarPluginResourceName).toURI());
			assertTrue(helvarPluginFile.exists());

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/plugins/", 
					makeJsonObjectForPlugin(helvarPluginName, null, Type.JAVASCRIPT, helvarPluginFile, false).toString());
			JSONObject resultPostHelvarPlugin = new JSONObject(res);
			assertEquals(2, resultPostHelvarPlugin.getInt("id"));
			assertEquals(helvarPluginName, resultPostHelvarPlugin.getString("service"));
			assertEquals("JAVASCRIPT", resultPostHelvarPlugin.getString("pluginType"));
			
			//Now we want install the enabler for the helvarnetPlugin
			JSONObject enablerHelvar = new JSONObject();
			enablerHelvar.put("plugin", 2);
			enablerHelvar.put("name", "helvar");
			enablerHelvar.put("metadata", "An enabler for the Helvarnet router");
			config = new JSONObject();
			config.put("address", "127.0.0.1");
			config.put("port", helvarPort);
			enablerHelvar.put("configuration", config);

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/enablers/", enablerHelvar.toString());
			JSONObject resultPostHelvarEnabler = new JSONObject(res);
			assertEquals(3, resultPostHelvarEnabler.getInt("id"));
			assertEquals("helvar", resultPostHelvarEnabler.getString("name"));
			assertEquals(2, resultPostHelvarEnabler.getJSONArray("features").length());
			
			//Now I need to make the helvarnet feature available as feeds
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + iothubPort + "/enablers/helvar/@1.1.1.1", gCalFeatureToFeed.toString());
			expectedFeature = new JSONObject(res);
			assertTrue(expectedFeature.getBoolean("isAtomicFeed"));
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + iothubPort + "/enablers/helvar/@1.1.2.1", gCalFeatureToFeed.toString());
			expectedFeature = new JSONObject(res);
			assertTrue(expectedFeature.getBoolean("isAtomicFeed"));
			
			//Now we check our atomic feeds
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/feeds", null);
			feedArray = new JSONArray(res);
			assertEquals(4, feedArray.length());
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/feeds/atomicFeature3", null);
			JSONObject error = new JSONObject(res);
			assertEquals("Error", error.getString("status"));
			
			//Now I need to create the plugin for the application
			File lastCallLightPluginFile = new File(LondonIcc2015Test.class.getResource(lastCallLightPluginResourceName).toURI());
			assertTrue(lastCallLightPluginFile.exists());
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/plugins/", 
					makeJsonObjectForPlugin(lastCallLightPluginName, null, Type.JAVASCRIPT, lastCallLightPluginFile, true).toString());
			JSONObject resultPostLastCallLightPlugin = new JSONObject(res);
			assertEquals(1, resultPostLastCallLightPlugin.getInt("id"));
			assertEquals(lastCallLightPluginName, resultPostLastCallLightPlugin.getString("service"));
			assertEquals("JAVASCRIPT", resultPostLastCallLightPlugin.getString("pluginType"));
			
			
			JSONObject jservice = new JSONObject();
			jservice.put("plugin", 1);
			jservice.put("name", lastCallLightPluginName);
			jservice.put("metadata", "The service that does the last call light");		
			jservice.put("bootAtStartup", false);
			config = new JSONObject();
			config.put("server", "http://127.0.0.1:" + iothubPort);
			config.put("oneshot", true);
			config.put("interval", 3000);
			JSONArray rooms = new JSONArray();
			JSONObject room1 = new JSONObject();
			room1.put("calendar", "atomicFeature1");
			JSONArray lights1 = new JSONArray();
			lights1.put("atomicFeature3");
			room1.put("lights", lights1);
			rooms.put(room1);
			JSONObject room2 = new JSONObject();
			room2.put("calendar", "atomicFeature2");
			JSONArray lights2 = new JSONArray();
			lights2.put("atomicFeature4");
			room2.put("lights", lights2);
			rooms.put(room2);
			config.put("rooms", rooms);
			jservice.put("configuration", config);
			
			// I should get an enable with no features as it is not configured
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubPort + "/services/", jservice.toString());
			JSONObject expectedService = new JSONObject(res);
			assertEquals(1, expectedService.getInt("id"));
			assertTrue(expectedService.has("config"));
			
			int currentNumberOfMessage = helvarBoxProtocol.getMsgReceived().size();
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubPort + "/services/" + lastCallLightPluginName + "/start", null);
			long start = System.currentTimeMillis();
			while(helvarBoxProtocol.getMsgReceived().size() < currentNumberOfMessage + 2) {
				long now = System.currentTimeMillis();
				if (now - start > 60000) {
					fail("The helvarnet server have not received the commands in time");
				}
			}
			iotHubHTTPD.stop();
			helvarRouter.stop();
			gcalServer.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
