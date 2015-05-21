/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub.conf;

/**
 * @author mineraud
 *
 */
public class ConfigurationParsingException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2863376642687000924L;
	private String message;
	
	public ConfigurationParsingException(String message) {
		super(message);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
