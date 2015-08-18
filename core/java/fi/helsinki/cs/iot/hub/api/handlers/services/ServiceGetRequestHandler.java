/*
 * fi.helsinki.cs.iot.hub.api.LibraryGetRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.services;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class ServiceGetRequestHandler extends IotHubApiRequestHandler {
	
	private static final String TAG = "LibraryGetRequestHandler";
	private Path dir;
	private List<Method> methods;
	
	public ServiceGetRequestHandler(Path dir) {
		this.dir = dir;
		this.methods = new ArrayList<>();
		this.methods.add(Method.GET);	
	}

	@Override
	public Response handleRequest(IotHubRequest uri) {
		Log.d(TAG, "I got a get request for library");
		// Case where we just want to list the libraires
		if (uri.getIdentifiers().size() == 0) {
			//At the moment all our libraries are only in javascript
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{js}")) {
				JSONArray jArray = new JSONArray();
				for (Path file: stream) {
					jArray.put(file.getFileName());
				}
				return getResponseOk(jArray.toString());
			} catch (IOException | DirectoryIteratorException x) {
				// IOException can never be thrown by the iteration.
				// In this snippet, it can only be thrown by newDirectoryStream.
				System.err.println(x);
				return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, x.getMessage());
			}
		}
		else if (uri.getIdentifiers().size() == 1) {
			Path file = Paths.get(dir.toAbsolutePath().toString(), uri.getIdentifiers().get(0));
			return getResponseOk(file.toFile(), "application/javascript");
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

}
