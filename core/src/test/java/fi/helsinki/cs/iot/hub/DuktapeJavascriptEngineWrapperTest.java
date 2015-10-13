/*
 * fi.helsinki.cs.iot.hub.DuktapeJavascriptEngineWrapperTest
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.jsengine.SimpleJavascriptedIotHubCode;
import fi.helsinki.cs.iot.hub.utils.socketserver.MultiThreadedSocketServer;
import fi.helsinki.cs.iot.hub.utils.socketserver.SocketProtocol;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;

/**
 * Basic Test class for the javascript engine
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class DuktapeJavascriptEngineWrapperTest {

	/**
	 * Test method for {@link fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper#performJavaHttpRequest(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testPerformJavaHttpRequest() {
		SimpleHTTPServer ts = new SimpleHTTPServer();
		try {
			ts.start();
			String response = DuktapeJavascriptEngineWrapper.performJavaHttpRequest("GET", "http://localhost:8111", null);
			JSONObject json = new JSONObject();
			json.put("test", "testValue");
			assertEquals(json.toString(), response.trim());
			ts.stop();
		} catch (IOException | JSONException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public final void testJsXmlHttpRequest() {
		String jsScript = "var xhr = XMLHttpRequest(); var res;";
		jsScript += "xhr.open('GET', 'http://127.0.0.1:8111', true);";
		jsScript += "xhr.onreadystatechange = function (event) {";
		jsScript += "if (xhr.readyState == 4 && xhr.status == 200) {";
		jsScript += "res = xhr.responseText;}};";
		jsScript += "xhr.send(null); res;";

		Path libdir = null;
		try {
			libdir = Files.createTempDirectory("testJsXmlHttpRequest");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		libdir.toFile().deleteOnExit();

		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(jsScript);
		SimpleHTTPServer ts = new SimpleHTTPServer();
		try {
			ts.start();
			DuktapeJavascriptEngineWrapper wrapper = 
					new DuktapeJavascriptEngineWrapper(libdir);
			try {
				assertNull(wrapper.runScript(simpleCode.getScript()));
			} catch (JavascriptEngineException e) {
				assertEquals("Script error: ReferenceError: identifier 'XMLHttpRequest' undefined", e.getMessage().trim());
			}
			wrapper =  new DuktapeJavascriptEngineWrapper(libdir, simpleCode, DuktapeJavascriptEngineWrapper.HTTP_REQUEST);
			String response = null;
			try {
				response = wrapper.runScript(simpleCode.getScript());
			} catch (JavascriptEngineException e) {
				fail(e.getMessage());
			}
			if (response != null) {
				response = response.trim();
			}
			JSONObject json = new JSONObject();
			json.put("test", "testValue");
			assertEquals(json.toString(), response);
			ts.stop();
		} catch (IOException | JSONException e) {
			fail(e.getMessage());
		} 
	}

	private class SimpleHTTPServer extends NanoHTTPD {

		private JSONObject json;

		public SimpleHTTPServer() {
			super("127.0.0.1", 8111);
			json = new JSONObject();
			try {
				json.put("test", "testValue");
			} catch (JSONException e) {
				fail(e.getMessage());
			}
		}

		@Override
		public Response serve(IHTTPSession session) {
			Response response = new Response(Response.Status.OK, "application/json", json.toString());
			return response;
		}
	}	

	public class SimpleEchoProtocol implements SocketProtocol {
		
		List<String> msgReceived;
		
		public SimpleEchoProtocol() {
			this.msgReceived = new ArrayList<>();
		}
		
		public List<String> getMsgReceived() {
			return this.msgReceived;
		}
		
		@Override
		public String processInput(String input) {
			if (input != null) {
				this.msgReceived.add(input);
			}
			return input;
		}
	}

	@Test
	public final void testTcpSocket() {
		int port = 50005;
		SimpleEchoProtocol echoProtocol = new SimpleEchoProtocol();
		MultiThreadedSocketServer tcpServer = new MultiThreadedSocketServer(echoProtocol, port);
		try {
			tcpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		String jsScript = "var sc = TCPSocket();";
		//jsScript += "sc.onreceive = function(msg) { print('Messaged received:' + msg);};";
		//jsScript += "sc.onreceive = null;";
		jsScript += "sc.onerror = function(msg) { print('Error:' + msg); };";
		jsScript += String.format("sc.connect('%s', %d);", "127.0.0.1", port);
		jsScript += "sc.send('I want to send this message');";
		jsScript += "sc.close();";

		Path libdir = null;
		try {
			libdir = Files.createTempDirectory("testTcpSocket");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		libdir.toFile().deleteOnExit();

		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(jsScript);
		DuktapeJavascriptEngineWrapper dtw = 
				new DuktapeJavascriptEngineWrapper(libdir, simpleCode, DuktapeJavascriptEngineWrapper.TCP_SOCKET);
		try {
			dtw.runScript(simpleCode.getScript());
		} catch (JavascriptEngineException e) {
			fail(e.getMessage());
		}
		// At that point, I should have stop the tcp socket
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tcpServer.stop();
		assertEquals(1, echoProtocol.getMsgReceived().size());
		assertEquals("I want to send this message", echoProtocol.getMsgReceived().get(0));
	}

	@Test
	public final void testNativeRunScript() {
		String helloWorld = "Hello world!";
		String script = "var echo = function(data) {return data}; echo(\"" + helloWorld + "\");";

		Path libdir = null;
		try {
			libdir = Files.createTempDirectory("testNativeRunScript");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		libdir.toFile().deleteOnExit();

		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(script);
		DuktapeJavascriptEngineWrapper wrapper = new DuktapeJavascriptEngineWrapper(libdir, simpleCode, DuktapeJavascriptEngineWrapper.TCP_SOCKET);
		try {
			String res = wrapper.runScript(script);
			assertEquals(helloWorld, res);
		} catch (JavascriptEngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public final void testNativeGetLibraryOutput() {
		String libraryName = "Test";
		String script = "var Test = { test: function() {return 'test';} }; ";

		Path libdir = null;
		try {
			libdir = Files.createTempDirectory("testNativeRunScript");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail(e1.getMessage());
		}
		libdir.toFile().deleteOnExit();

		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(script);
		DuktapeJavascriptEngineWrapper wrapper = new DuktapeJavascriptEngineWrapper(libdir, simpleCode, DuktapeJavascriptEngineWrapper.TCP_SOCKET);
		try {
			assertEquals("test", wrapper.getLibraryOutput(libraryName, script, "Test.test();"));
		} catch (JavascriptEngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



}
