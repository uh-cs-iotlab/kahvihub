/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub.conf;

/**
 * @author mineraud
 *
 */
public class HubConfig {
	private String name;
	private int port;
	private String libdir;
	private String logdir;
	private String dbdir;
	private String dbname;
	private int dbversion;
	private String dashboard;
	private boolean debugMode;
	
	public HubConfig(String name, int port, String libdir, String logdir, 
			String dbdir, String dbname, int dbversion,
			String dashboard, boolean debugMode) {
		this.name = name;
		this.port = port;
		this.libdir = libdir;
		this.logdir = logdir;
		this.dbdir = dbdir;
		this.dbname = dbname;
		this.dbversion = dbversion;
		this.dashboard = dashboard;
		this.debugMode = debugMode;
	}
	
	public HubConfig(int port, String libdir, String logdir, String dbdir, String dbname, int dbversion,
			String dashboard, boolean debugMode) {
		this("iothub", port, libdir, logdir, 
				dbdir, dbname, dbversion, dashboard, debugMode);
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

	public String getDashboard() {
		return dashboard;
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
