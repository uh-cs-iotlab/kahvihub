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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

	private class SimpleTcpServer {
		List<String> msgReceivedByServer;
		boolean stopTheThread;
		final int port;

		public SimpleTcpServer(int port) {
			this.port = port;
			this.stopTheThread = false;
			msgReceivedByServer = new ArrayList<String>();
		}

		class SimpleTcpServerThread implements Runnable {

			private Socket socket;

			public SimpleTcpServerThread(Socket s) throws IOException {
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
						out.println(inputLine);//Just echo
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
							service.submit(new SimpleTcpServerThread(socket));
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

	@Test
	public final void testTcpSocket() {

		int port = 50005;
		SimpleTcpServer tcpServer = new SimpleTcpServer(port);
		tcpServer.start();


		String jsScript = "var sc = TCPSocket();";
		jsScript += "sc.onsocketreceive = function(msg) { print('Messaged received:' + msg); };";
		jsScript += "sc.onsocketerror = function(msg) { print('Error:' + msg); };";
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
		try {
			tcpServer.stop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertEquals(1, tcpServer.getMsgReceivedByServer().size());
		assertEquals("I want to send this message", tcpServer.getMsgReceivedByServer().get(0));
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
