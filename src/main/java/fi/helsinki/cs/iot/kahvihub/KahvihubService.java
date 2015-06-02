/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceException;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;

/**
 * @author mineraud
 *
 */
public class KahvihubService implements Service {

	private long id;
	private ServiceInfo serviceInfo;
	private String name;
	private String metadata;
	private String config;
	private boolean bootOnStartup;
	
	/**
	 * @param id
	 * @param name
	 * @param metadata
	 * @param config
	 * @param bootOnStartup
	 */
	public KahvihubService(long id, ServiceInfo serviceInfo, String name, String metadata,
			String config, boolean bootOnStartup) {
		this.id = id;
		this.name = name;
		this.metadata = metadata;
		this.config = config;
		this.bootOnStartup = bootOnStartup;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the serviceInfo
	 */
	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the metadata
	 */
	public String getMetadata() {
		return metadata;
	}

	/**
	 * @return the config
	 */
	public String getConfig() {
		return config;
	}

	/**
	 * @return the bootOnStartup
	 */
	public boolean isBootOnStartup() {
		return bootOnStartup;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#compareConfiguration(java.lang.String)
	 */
	@Override
	public boolean compareConfiguration(String configuration) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#configure(java.lang.String)
	 */
	@Override
	public void configure(String configuration) throws ServiceException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#isConfigured()
	 */
	@Override
	public boolean isConfigured() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#isStarted()
	 */
	@Override
	public void isStarted() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#needConfiguration()
	 */
	@Override
	public boolean needConfiguration() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getJsonDescription() {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", name);
			return jsonObject.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			
		}
		return null;
	}

}
