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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fi.helsinki.cs.iot.hub.api.BasicIotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.ListHttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.kahvihub.admin.AdminHttpRequestHandler;
import fi.helsinki.cs.iot.kahvihub.conf.ConfigurationFileParser;
import fi.helsinki.cs.iot.kahvihub.conf.ConfigurationParsingException;
import fi.helsinki.cs.iot.kahvihub.conf.HubConfig;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class KahviHub {

	private static void setLogger(HubConfig config) {
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
	}

	private static void setIotHubDataHandler(HubConfig config) {
		String dbfilename = config.getDbdir() + config.getDbName();
		IotHubDataAccess.setInstance(new IotHubDbHandlerSqliteJDBCImpl(dbfilename, 
				config.getDbVersion(), config.isDebugMode()));
	}

	//TODO init should be reading a config file
	public static void init(HubConfig config) {
		setLogger(config);
		setIotHubDataHandler(config);
	}

	public static void main(String[] args) throws InterruptedException {
		
		// create Options object
		Options options = new Options();
		// add conf file option
		options.addOption("c", true, "config file");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			String configFile = cmd.getOptionValue("c");
			if (configFile == null) {
				System.err.println("The config file option was not provided");
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("c", options);
				System.exit(-1);
			}
			else {
				try {
					HubConfig hubConfig = ConfigurationFileParser.parseConfigurationFile(configFile);
					File libdir = new File(hubConfig.getLibdir());
					File dashboard = new File(hubConfig.getDashboard());
					final IotHubHTTPD server = new IotHubHTTPD(hubConfig.getPort(), libdir, dashboard);
					ListHttpRequestHandler handler = new ListHttpRequestHandler();
					handler.addHttpRequestHandler(new AdminHttpRequestHandler(hubConfig.getLibdir()), 0); //Max priority for the admin page
					handler.addHttpRequestHandler(new BasicIotHubApiRequestHandler(libdir), 1);
					server.setHttpRequestHandler(handler);
					init(hubConfig);
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
							server.stop();
							System.out.println("Server stopped");
						}
					});

					while (true)
					{
						Thread.sleep(1000);
					}
				} catch (ConfigurationParsingException | IOException e){
					System.err.println(e.getMessage());
					System.exit(-1);
				} 
			}

		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}		
	}

}