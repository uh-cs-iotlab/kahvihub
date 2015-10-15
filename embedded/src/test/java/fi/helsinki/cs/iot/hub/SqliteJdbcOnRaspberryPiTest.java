/**
 * 
 */
package fi.helsinki.cs.iot.hub;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sqlite.SQLiteJDBCLoader;
import org.sqlite.util.OSInfo;

/**
 * @author mineraud
 *
 */
public class SqliteJdbcOnRaspberryPiTest {

	@Test
	public void test() {
		System.out.println("OS/NativeLibFolder: " + OSInfo.getNativeLibFolderPathForCurrentOS());
	}

}
