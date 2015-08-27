/**
 * 
 */
package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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


	@Test
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
			System.out.println(res);
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
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
