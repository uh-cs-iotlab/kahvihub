/*
 * fi.helsinki.cs.iot.kahvihub.conf.HubConfig
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

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class HubConfig {
	private String name;
	private int port;
	private String libdir;
	private String logdir;
	private String dbdir;
	private String dbname;
	private int dbversion;
	private boolean debugMode;
	
	public HubConfig(String name, int port, String libdir, String logdir, 
			String dbdir, String dbname, int dbversion,
			boolean debugMode) {
		this.name = name;
		this.port = port;
		this.libdir = libdir;
		this.logdir = logdir;
		this.dbdir = dbdir;
		this.dbname = dbname;
		this.dbversion = dbversion;
		this.debugMode = debugMode;
	}
	
	public HubConfig(int port, String libdir, String logdir, String dbdir, String dbname, int dbversion, boolean debugMode) {
		this("iothub", port, libdir, logdir, 
				dbdir, dbname, dbversion, debugMode);
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}

	public String getLogdir() {
		return logdir;
	}

	public String getDbdir() {
		return dbdir;
	}

	public String getDbName() {
		return dbname;
	}
	
	public int getDbVersion() {
		return dbversion;
	}

	public boolean isDebugMode() {
		return debugMode;
	}
	
	public String getLibdir() {
		return libdir;
	}
	
}
