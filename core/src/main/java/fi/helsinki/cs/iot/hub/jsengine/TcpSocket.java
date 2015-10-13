/*
 * fi.helsinki.cs.iot.hub.jsengine.TcpSocket
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
package fi.helsinki.cs.iot.hub.jsengine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This class enables the use of a tcp socket in javascript
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class TcpSocket extends Socket {

	private final int id;
	private OutputStreamWriter writer;
	private InputStreamReader reader;

	public static boolean checkHostAvailability(String host, int port) throws IOException {
		Socket s = new Socket(host, port);
		if (!s.isConnected()) {
			s.connect(new InetSocketAddress(host,port),500);
		}
		s.close();
		return true;
	}

	public TcpSocket(int id, String host, int port) throws IOException {
		super(host, port);
		this.id = id;
		writer = new OutputStreamWriter(getOutputStream());
		reader = new InputStreamReader(getInputStream());
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
	
	private String makeQuery(OutputStreamWriter writer, InputStreamReader reader, String query, boolean waitForAnswer) throws IOException {
		if (writer == null || reader == null) {
			System.err.println("Cannot perform the operation");
			return null;
		}
		writer.write(query + "\n");
		writer.flush();
		
		if (!waitForAnswer) {
			return "";
		}
		char[] cbuf = new char[1024];
		if(reader.read(cbuf) >= 0) {
			return dropExtraCommand(new String(cbuf).trim());
		}
		return null;
	}

	public String send(String message, boolean waitForAnswer) throws IOException {
		if (message != null) {
			String response = makeQuery(writer, reader, message, waitForAnswer);
			return response;
		}
		return null;
	}

	@Override
	public String toString() {
		return "TcpSocket [id=" + id + ", host=" + getInetAddress().toString() + ", port=" + getPort() + " ]";
	}

	public int getId() {
		return id;
	}

}
