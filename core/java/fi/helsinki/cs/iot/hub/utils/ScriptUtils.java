/**
 * 
 */
package fi.helsinki.cs.iot.hub.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.codec.binary.Base64;

/**
 * @author mineraud
 *
 */
public class ScriptUtils {
	
	public static String convertStreamToString(InputStream is) {
		// To convert the InputStream to String we use the BufferedReader.readLine()
		// method. We iterate until the BufferedReader return null which means
		// there's no more data to read. Each line will appended to a StringBuilder
		// and returned as String.
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
	
	public static String convertFileToString(File file) {
		if (file == null || file.isDirectory() || !file.exists()) {
			return null;
		}
		else {
			try {
				return ScriptUtils.convertStreamToString(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public static String encodeBase64FromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String lineWithLine = line + "\n";
				String encodedLine = Base64.encodeBase64String(lineWithLine.getBytes());
				sb.append(encodedLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
	
	public static String decodeBase64ToString(String encoded) throws IOException {
		return new String(Base64.decodeBase64(encoded));
	}
	
	public static File decodeBase64ToFile(String filename, String encoded) throws IOException {
		File file = new File(filename);
		if (file.exists()) {
			throw new IOException("The file " + filename + " already exists");
		}
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
		String decodedString = decodeBase64ToString(encoded);
		bufferedWriter.write(decodedString);
		bufferedWriter.close();
		return file;
	}
	
	public static File decodeBase64ToFile(String encoded) throws IOException {
		File file = File.createTempFile("script", null);
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
		String decodedString = decodeBase64ToString(encoded);
		bufferedWriter.write(decodedString);
		bufferedWriter.close();
		return file;
	}

}
