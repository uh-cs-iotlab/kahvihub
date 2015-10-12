/**
 * 
 */
package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.TcpSocket;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.kahvihub.IotHubDbHandlerSqliteJDBCImpl;

/**
 * @author mineraud
 *
 */
public class HelvarBoxTest {

	private IotHubHTTPD iotHubHTTPD;
	private int iothubport = 8081;
	private Path libdir;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		libdir = Files.createTempDirectory("IotHubHTTPDTest");
		libdir.toFile().deleteOnExit();
		iotHubHTTPD = new IotHubHTTPD(iothubport, libdir);

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
	
	private String dropExtraCommand(String message) {
		int count = message.length() - message.replace("#", "").length();
		if (count > 1) {
			int currentIndex = 0;
			for (int i = 0; i < count; i++) {
				int nextIndex = message.indexOf("#", currentIndex);
				String substr = message.substring(currentIndex, nextIndex + 1);
				if (substr.startsWith("?")) {
					System.err.println("I had to drop some messages for " + message + ", and I only kept: " + substr);
					message = substr;
					break;
				}
				currentIndex = nextIndex + 1;
			}
		}
		return message;
	}
	
	private String makeQuery(OutputStreamWriter writer, InputStreamReader reader, String query) throws IOException {
		if (writer == null || reader == null) {
			System.err.println("Cannot perform the operation");
			return null;
		}

		writer.write(query);
		writer.flush();

		char[] cbuf = new char[1024];
		if(reader.read(cbuf) >= 0) {
			return dropExtraCommand(new String(cbuf).trim());
		}
		return null;
	}
	
	public void testNative() {
		int port = 50000;
		String address = "10.254.1.1";
		
		//Toggle that if you need to check if it works natively
		org.junit.Assume.assumeTrue(false);
		
		boolean isHostAvailable = false;
		try {
			isHostAvailable = TcpSocket.checkHostAvailability(address, port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		org.junit.Assume.assumeTrue(isHostAvailable);

		try {
			Date startTime = new Date();
			Socket socket = new Socket(address, port);
			
			OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
			InputStreamReader reader = new InputStreamReader(socket.getInputStream());
			
			//First we get the list of groups
			String response = makeQuery(writer, reader, ">V:2,C:165#");
			Date interTime = new Date();
			System.out.println(String.format("It took %.3f seconds to get the groups %s", 
					(interTime.getTime() - startTime.getTime()) / 1000.0, response));
			String[] groups = response.substring(response.indexOf("=") + 1, response.indexOf("#")).split(",");
			for (String group : groups) {
				response = makeQuery(writer, reader, ">V:2,C:164,G:" + group + "#");
				Date newTime = new Date();
				System.out.println(String.format("It took %.3f seconds to get the features %s", 
						(newTime.getTime() - interTime.getTime()) / 1000.0, response));
				interTime = newTime;
				String[] features = response.substring(response.indexOf('=') + 1, response.indexOf('#')).split(",");
				for (String feature : features) {
					response = makeQuery(writer, reader, ">V:2,C:104," + feature + "#");
					newTime = new Date();
					System.out.println(String.format("It took %.3f seconds to get the feature type %s", 
							(newTime.getTime() - interTime.getTime()) / 1000.0, response));
					interTime = newTime;
				}
			}
			socket.shutdownInput();
			socket.shutdownOutput();
			socket.close();
			System.out.println(String.format("It took %.3f seconds to do the initialisation", 
					(interTime.getTime() - startTime.getTime()) / 1000.0));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	public void test() {
		int port = 50000;
		String address = "10.254.1.1";

		boolean isHostAvailable = false;
		try {
			isHostAvailable = TcpSocket.checkHostAvailability(address, port);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		org.junit.Assume.assumeTrue(isHostAvailable);

		try {
			//Now we need to install the helvarNet plugin
			File helvarPluginFile = new File(HelvarBoxTest.class.getResource("/helvarnetPlugin.js").toURI());
			assertTrue(helvarPluginFile.exists());
			String helvarPluginName = "HelvarnetPlugin";
			String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubport + "/plugins/", 
					makeJsonObjectForPlugin(helvarPluginName, null, Type.JAVASCRIPT, helvarPluginFile, false).toString());
			JSONObject resultPostHelvarPlugin = new JSONObject(res);
			assertEquals(1, resultPostHelvarPlugin.getInt("id"));
			assertEquals(helvarPluginName, resultPostHelvarPlugin.getString("service"));
			assertEquals("JAVASCRIPT", resultPostHelvarPlugin.getString("pluginType"));

			//Now we want install the enabler for the helvarnetPlugin
			JSONObject enablerHelvar = new JSONObject();
			enablerHelvar.put("plugin", 1);
			enablerHelvar.put("name", "helvar");
			enablerHelvar.put("metadata", "An enabler for the Helvarnet router");
			JSONObject config = new JSONObject();
			config.put("address", address);
			config.put("port", port);
			enablerHelvar.put("configuration", config);

			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubport + "/enablers/", enablerHelvar.toString());
			JSONObject resultPostHelvarEnabler = new JSONObject(res);
			assertEquals(1, resultPostHelvarEnabler.getInt("id"));
			assertEquals("helvar", resultPostHelvarEnabler.getString("name"));
			org.junit.Assume.assumeTrue("The helvarbox is probably not the one I used for demo, sorry",
					2 == resultPostHelvarEnabler.getJSONArray("features").length());

			JSONObject featureToFeed = new JSONObject();
			featureToFeed.put("enableAsAtomicFeed", true);
			//Now I need to make the helvarnet feature available as feeds
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("PUT", "http://127.0.0.1:" + iothubport + "/enablers/helvar/@1.1.1.1", featureToFeed.toString());
			JSONObject expectedFeature = new JSONObject(res);
			assertTrue(expectedFeature.getBoolean("isAtomicFeed"));
			
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://127.0.0.1:" + iothubport + "/feeds/", null);
			JSONArray feeds = new JSONArray(res);
			assertEquals(1, feeds.length());
			JSONObject feed = feeds.getJSONObject(0);
			assertFalse(feed.getBoolean("readable"));
			assertTrue(feed.getBoolean("writable"));
			String feedName = feed.getString("name");
			
			//Now I want to change the lights
			JSONObject lightdata = new JSONObject("{\"light\": {\"luminosity\": 50, \"fade\": 200}}");
			res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("POST", "http://127.0.0.1:" + iothubport + "/feeds/" + feedName, lightdata.toString());
			System.out.println(res);



		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
