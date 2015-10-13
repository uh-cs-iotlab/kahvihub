/**
 * 
 */
package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fi.helsinki.cs.iot.hub.utils.socketserver.MultiThreadedSocketServer;
import fi.helsinki.cs.iot.hub.utils.socketserver.SocketProtocol;

/**
 * @author mineraud
 *
 */
public class SocketServerTest {
	
	private class KnockKnockProtocol implements SocketProtocol {
		
		private Map<String, Integer> messages;
		
		public KnockKnockProtocol() {
			this.messages = new HashMap<>();
			String input = null;
			while ((input = processInput(input)) != null) {
				messages.put(input, 0);
			}
		}
		
		@Override
		public String processInput(String input) {
			if (input != null) {
				this.messages.put(input, this.messages.get(input).intValue() + 1);
			}
			if (input == null) {
				return "Knock knock!";
			}
			else if ("Knock knock!".equals(input)) {
				return "Who's there?";
			}
			else if ("Who's there?".equals(input)) {
				return "Dexter.";
			}
			else if ("Dexter.".equals(input)) {
				return "Dexter who?";
			}
			else if ("Dexter who?".equals(input)) {
				return "Dexter halls with boughs of holly.";
			}
			else if ("Dexter halls with boughs of holly.".equals(input)) {
				return "Groan.";
			}
			else if ("Groan.".equals(input)) {
				return "Bye.";
			}
			return null;
		}
		
		public void check() {
			assertEquals(1, messages.get("Bye.").intValue());
		}
	}
	
	private class KnockKnockClientListener {
		
		private int count;
		private int total;
		private MultiThreadedSocketServer server;
		
		public KnockKnockClientListener(int total, MultiThreadedSocketServer server) {
			this.count = 0;
			this.total = total;
			this.server = server;
		}
		
		public void notifyCompleted() {
			this.count++;
			if (isCompleted()) {
				server.stop();
			}
		}
		
		public boolean isCompleted() {
			return count >= total;
		}
	}
	
	private class KnockKnockClient implements Runnable {
		private final int port;
		private final KnockKnockProtocol protocol;
		private KnockKnockClientListener listener;
		
		public KnockKnockClient(int port, KnockKnockClientListener listenener, KnockKnockProtocol protocol) {
			this.port = port;
			this.protocol = protocol;
			this.listener = listenener;
		}
		
		@Override
		public void run() {
			try {
				Socket socket = new Socket("127.0.0.1", port);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(socket.getInputStream()));
				
				String fromServer, fromClient;
				while ((fromServer = in.readLine()) != null) {
				    if ("Bye.".equals(fromServer)) {
				    	break;
				    }

				    fromClient = protocol.processInput(fromServer);
				    out.println(fromClient);
				    out.flush();
				}
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (this.listener != null) {
				this.listener.notifyCompleted();
			}
		}
		
		public void start() {
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	@Test
	public void testKnockKnock() {
		int port = 50002;
		KnockKnockProtocol knockKnockProtocol = new KnockKnockProtocol();
		MultiThreadedSocketServer knockKnockServer = new MultiThreadedSocketServer(knockKnockProtocol, port);
		try {
			knockKnockServer.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		KnockKnockClientListener listener = new KnockKnockClientListener(2, knockKnockServer);
		
		KnockKnockClient knockKnockClient = new KnockKnockClient(port, listener, knockKnockProtocol);
		knockKnockClient.start();
		KnockKnockClient knockKnockClient2 = new KnockKnockClient(port, listener, knockKnockProtocol);
		knockKnockClient2.start();
		
		try {
			knockKnockServer.getThread().join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		knockKnockProtocol.check();
	}

}
