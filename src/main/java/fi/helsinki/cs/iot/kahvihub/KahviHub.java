/*
 * fi.helsinki.cs.iot.kahvihub.KahviHub
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
package fi.helsinki.cs.iot.kahvihub;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.BasicIotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * Based on the SimpleWebServer example of NanoHttpd
 */
public class KahviHub extends NanoHTTPD {
	
	private String host;
	private int port;
	private File rootDir;
	private BasicIotHubApiRequestHandler requestHandler;
	
	public KahviHub(String host, int port, File rootDir) {
		super(host, port);
		this.rootDir = rootDir;
		this.requestHandler = new BasicIotHubApiRequestHandler(this.rootDir);
		this.init();
	}
	
	//TODO init should be reading a config file
	public void init() {
		//TODO I would need a better logger, maybe even the one from android
		Logger logger = new Logger(){
			@Override
			public void d(String tag, String msg) {
				System.out.println("[Debug] " + tag + ": " + msg);
			}
			@Override
			public void i(String tag, String msg) {
				System.out.println("[Info] " + tag + ": " + msg);
			}
			@Override
			public void w(String tag, String msg) {
				System.err.println("[Warn] " + tag + ": " + msg);
			}
			@Override
			public void e(String tag, String msg) {
				System.err.println("[Error] " + tag + ": " + msg);
			}
		};
		Log.setLogger(logger);
		//TODO this would have to be configured via a configuration file
		IotHubDataAccess.setInstance(new IotHubDbHandlerSqliteJDBCImpl("kahvihub.db", 0, true));
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public File getRootDir() {
		return this.rootDir;
	}
	
	@Override
	public void start() throws IOException {
		// TODO Auto-generated method stub
		super.start();
		try {
			IotHubDataAccess.getInstance().open();
		} catch (IotHubDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Server started");
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
		try {
			IotHubDataAccess.getInstance().close();
		} catch (IotHubDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Server stopped");
		
	}
	
	private String getMimeType(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        return header.get("Content-type");
    }
	
	private String getBodyData(IHTTPSession session) throws IOException, ResponseException {
        if (session.getMethod() == Method.PUT || session.getMethod() == Method.POST) {
            Map<String, String> files = new HashMap<String, String>();
            session.parseBody(files);
            String bodyData = files.get("postData");
            return bodyData;
        } else {
            return null;
        }
    }

	@Override
	public Response serve(IHTTPSession session) {
		String bodyData = null;
        try {
            bodyData = getBodyData(session);
        } catch (Exception e) {
        }

        String uri = session.getUri();
        Method method = session.getMethod();
        String mimeType = getMimeType(session);
        //FIXME connect the requestId to the database logger
        //long requestId = 0L;
        Response response = requestHandler.handleRequest(method, 
        		uri, session.getParms(), mimeType, bodyData);
        //TODO log the time at the call of serve and when the response is ready
        return response;
	}

	public static void main(String[] args) throws InterruptedException {
		int port = 8080;
		String host = "127.0.0.1";
		File rootDir = new File("www");
		
		final KahviHub server = new KahviHub(host, port, rootDir);
		try {
			server.start();
		} catch (IOException ioe) {
			System.err.println("Couldn't start server:\n" + ioe);
			System.exit(-1);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutdown hook ran!");
                server.stop();
            }
        });
		
		while (true)
        {
            Thread.sleep(1000);
        }
	}
	
}