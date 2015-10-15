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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.NativePluginHelper;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.kahvihub.conf.ConfigurationFileParser;
import fi.helsinki.cs.iot.kahvihub.conf.ConfigurationParsingException;
import fi.helsinki.cs.iot.kahvihub.conf.HubConfig;
import fi.helsinki.cs.iot.kahvihub.database.sqliteJdbc.IotHubDbHandlerSqliteJDBCImpl;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class KahviHub {

	private static final String TAG = "KahviHub";

	private static void setLogger(HubConfig config) {
		//TODO I would need a better logger, maybe even the one from android
		// I should also try to make the logger available for native plugins
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
	
	private static void setNativePluginHelper(HubConfig config) {
		//TODO maybe in the future, I want to have separate folder for native and js plugins
		NativePluginHelper nativePluginHelper = new KahvihubNativePluginHelper(config.getLibdir());
		PluginManager.getInstance().setNativePluginHelper(nativePluginHelper);
	}
	
	//TODO init should be reading a config file
	public static void init(HubConfig config) {
		setLogger(config);
		setIotHubDataHandler(config);
		setNativePluginHelper(config);
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
				Log.e(TAG, "The config file option was not provided");
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("d", options);
				System.exit(-1);
			}
			else {
				try {
					HubConfig hubConfig = ConfigurationFileParser.parseConfigurationFile(configFile);
					Path libdir = Paths.get(hubConfig.getLibdir());
					if (hubConfig.isDebugMode()) {
						File dir = libdir.toFile();
						if (dir.exists() && dir.isDirectory())
							for(File file: dir.listFiles()) file.delete(); 
					}
					final IotHubHTTPD server = new IotHubHTTPD(hubConfig.getPort(), libdir);
					init(hubConfig);
					try {
						server.start();
					} catch (IOException ioe) {
						Log.e(TAG, "Couldn't start server:\n" + ioe);
						System.exit(-1);
					}
					Runtime.getRuntime().addShutdownHook(new Thread()
					{
						@Override
						public void run()
						{
							server.stop();
							Log.i(TAG, "Server stopped");
						}
					});

					while (true)
					{
						Thread.sleep(1000);
					}
				} catch (ConfigurationParsingException | IOException e){
					System.out.println("1:"  + e.getMessage());
					Log.e(TAG, e.getMessage());
					System.exit(-1);
				} 
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			Log.e(TAG, e.getMessage());
			System.exit(-1);
		}		
	}

}