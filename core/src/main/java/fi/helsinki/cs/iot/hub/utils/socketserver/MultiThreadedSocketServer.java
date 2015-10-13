/**
 * 
 */
package fi.helsinki.cs.iot.hub.utils.socketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * @author mineraud
 *
 */
public class MultiThreadedSocketServer {

	protected static final String TAG = "MultiThreadedSocketServer";
	private ServerSocket serverSocket;
	private boolean doRunServerThread;
	private final int port;
	private final SocketProtocol protocol;
	private Thread thread;

	public class ClientService implements Runnable {

		private Socket clientSocket;

		public ClientService(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			BufferedReader in = null; 
			PrintWriter out = null;
			// Print out details of this connection 
			Log.d(TAG, "Accepted Client Address - " + clientSocket.getInetAddress().getHostName());
			try {                                
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
				out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream())); 

				String line = null;
				String processedLine = protocol.processInput(line);
				if (processedLine != null) {
					//Say something to the client
					out.println(processedLine);
					out.flush();
				}

				while((line = in.readLine()) != null) {                    
					Log.d(TAG, "Client: " + line);
					processedLine = protocol.processInput(line);
					if (processedLine != null) {
						//Say something to the client
						out.println(processedLine);
						out.flush();
					}
				}
			} 
			catch(Exception e) { 
				e.printStackTrace(); 
			} 
			finally { 
				// Clean up 
				try {                    
					in.close(); 
					out.close(); 
					clientSocket.close();
				} 
				catch(IOException ioe) { 
					ioe.printStackTrace(); 
				} 
			}
		}

	}

	public MultiThreadedSocketServer(SocketProtocol protocol, int port) {
		this.port = port;
		this.protocol = protocol;
		this.doRunServerThread = false;
	}

	public void start() throws IOException {

		if (doRunServerThread) {
			return;
		}
		doRunServerThread = true;
		serverSocket = new ServerSocket(port);

		Runnable runnable = new Runnable() {	
			@Override
			public void run() {
				while(doRunServerThread) {
					try {
						Socket clientSocket = serverSocket.accept();
						ClientService clientService = new ClientService(clientSocket);
						Thread thread = new Thread(clientService);
						thread.start();
					} 
					catch (SocketException se) {
						Log.d(TAG, se.getMessage());
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

		thread = new Thread(runnable);
		thread.start();
	}

	public void stop() {
		doRunServerThread = false;
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		thread.interrupt();
	}
	
	public Thread getThread() {
		return thread;
	}



}
