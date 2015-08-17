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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.jsengine.SimpleJavascriptedIotHubCode;
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
		TemporaryServer ts = new TemporaryServer();
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
		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(DuktapeJavascriptEngineWrapper.HTTP_REQUEST, jsScript);
		TemporaryServer ts = new TemporaryServer();
		try {
			ts.start();
			DuktapeJavascriptEngineWrapper wrapper = 
					new DuktapeJavascriptEngineWrapper();
			try {
				assertNull(wrapper.runScript(simpleCode.getScript()));
			} catch (JavascriptEngineException e) {
				assertEquals("Script error: ReferenceError: identifier 'XMLHttpRequest' undefined", e.getMessage().trim());
			}
			wrapper =  new DuktapeJavascriptEngineWrapper(simpleCode);
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

	private class TemporaryServer extends NanoHTTPD {

		private JSONObject json;

		public TemporaryServer() {
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

	@Test
	public final void testTcpSocket() {

		int port = 50000;
		TemporaryTcpServer tcpServer = new TemporaryTcpServer(port);
		tcpServer.start();

		
		String jsScript = "var sc = TCPSocket();";
		jsScript += "sc.onsocketreceive = function(msg) { print('Messaged received:' + msg); sc.close(); };";
		jsScript += "sc.onsocketerror = function(msg) { print('Error:' + msg); };";
		jsScript += String.format("sc.connect('%s', %d);", "127.0.0.1", port);
		jsScript += "sc.send('I want to send this message');";
		SimpleJavascriptedIotHubCode simpleCode = new SimpleJavascriptedIotHubCode(DuktapeJavascriptEngineWrapper.TCP_SOCKET, jsScript);
		DuktapeJavascriptEngineWrapper dtw = 
				new DuktapeJavascriptEngineWrapper(simpleCode);
		try {
			dtw.runScript(simpleCode.getScript());
		} catch (JavascriptEngineException e) {
			fail(e.getMessage());
		}
		// At that point, I should have stop the tcp socket
		tcpServer.stop();
		assertEquals(1, tcpServer.getMsgReceivedByServer().size());
		assertEquals("I want to send this message", tcpServer.getMsgReceivedByServer().get(0));
	}

	private class TemporaryTcpServer {
		List<String> msgReceivedByServer = new ArrayList<String>();
		boolean stopTheThread;
		int port;
		Thread thread;

		TemporaryTcpServer(int port) {
			this.port = port;
			this.stopTheThread = false;
		}

		void start() {
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					ServerSocket serverSocket = null;
					try {
						serverSocket = new ServerSocket(port);
						Socket clientSocket = serverSocket.accept();
						PrintWriter out =
								new PrintWriter(clientSocket.getOutputStream(), true);
						BufferedReader in = new BufferedReader(
								new InputStreamReader(clientSocket.getInputStream()));
						String inputLine;
						while (!stopTheThread && (inputLine = in.readLine()) != null) {
							msgReceivedByServer.add(inputLine);
							out.println("So nice of you to send me a message");
						}
						in.close();
						out.close();
					} catch (IOException e) {
						fail(e.getMessage());
					}
					finally {
						if (serverSocket != null) {
							try {
								serverSocket.close();
							} catch (IOException e) {
								fail(e.getMessage());
							}
						}
					}
				}
			});
			thread.start();
		}

		void stop() {
			this.stopTheThread = true;
		}

		List<String> getMsgReceivedByServer() {
			return msgReceivedByServer;
		}

	}

}
