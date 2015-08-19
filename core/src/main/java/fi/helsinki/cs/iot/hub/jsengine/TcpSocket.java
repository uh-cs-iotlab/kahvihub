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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This class enables the use of a tcp socket in javascript
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class TcpSocket extends Socket {

	private final int id;

	protected static boolean checkHostAvailability(String host, int port) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress(host,port),500);
		s.close();
		return true;
	}

	public TcpSocket(int id, String host, int port) throws IOException {
		super(host, port);
		this.id = id;
	}

	public String send(String message) throws IOException {
		PrintWriter out = new PrintWriter(getOutputStream(), true);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(getInputStream()));
		if (message != null) {
			out.println(message);
			return in.readLine();
		}
		out.close();
		in.close();
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
