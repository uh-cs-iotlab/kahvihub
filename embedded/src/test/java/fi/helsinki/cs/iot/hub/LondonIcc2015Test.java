package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.junit.Test;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.kahvihub.IotHubDbHandlerSqliteJDBCImpl;

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
		String helvarAddress = "10.254.1.1";
		int iothubPort = 8081;
		String gCalPluginResourceName = "/gCalPlugin.js";
		String gCalPluginName = "GCalPlugin";

		try {
			GCalServer gcalServer = new GCalServer(gcalPort);
			gcalServer.add("room1", gcalKey, false);
			gcalServer.add("room2", gcalKey, true);
			SimpleHelvarnetRouter helvarRouter = new SimpleHelvarnetRouter(helvarAddress, helvarPort);
			
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
			fail(res);
			

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
