/*
 * fi.helsinki.cs.iot.kahvihub.conf.ConfigurationFileParser
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
package fi.helsinki.cs.iot.kahvihub.conf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * ConfigurationFileParser is a JSON parser to generate the HubConfig
 */
public class ConfigurationFileParser {
	
	private static String getStringProperty(JSONObject root, String name) throws ConfigurationParsingException {
		String val = null;
		try {
			val = root.getString(name);
			if (val == null) {
				throw new ConfigurationParsingException("The configuration file needs a " + name + " property (string)");
			}
		} catch (JSONException e) {
			throw new ConfigurationParsingException("The configuration file needs a " + name + " property (string)");
		}
		return val;
	}
	
	private static int getIntProperty(JSONObject root, String name) throws ConfigurationParsingException {
		int val = -1;
		try {
			val = root.getInt(name);
			if (val == -1) {
				throw new ConfigurationParsingException("The configuration file needs a " + name + " property (int)");
			}
		} catch (JSONException e) {
			throw new ConfigurationParsingException("The configuration file needs a " + name + " property (int)");
		}
		return val;
	}
	
	private static boolean getBooleanProperty(JSONObject root, String name) throws ConfigurationParsingException {
		boolean val = false;
		try {
			val = root.getBoolean(name);
		} catch (JSONException e) {
			throw new ConfigurationParsingException("The configuration file needs a " + name + " property (boolean)");
		}
		return val;
	}
	
	public static HubConfig parseConfigurationFile(String filename) throws ConfigurationParsingException, IOException {	
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line);
		}
		bufferedReader.close();
		fileReader.close();
		
		JSONObject root = null;
		try {
			root = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			throw new ConfigurationParsingException("The configuration file is not a proper JSON object");
		}
		
		String name = getStringProperty(root, "name");
		int port = getIntProperty(root, "port");
		String libdir = getStringProperty(root, "libdir");
		String logdir = getStringProperty(root, "logdir");
		String dbdir = getStringProperty(root, "dbdir");
		String dbname = getStringProperty(root, "dbname");
		int dbversion = getIntProperty(root, "dbversion");
		boolean debugMode = getBooleanProperty(root, "debug");
		
		return new HubConfig(name, port, libdir, logdir, 
				dbdir, dbname, dbversion, debugMode);
	}
}
