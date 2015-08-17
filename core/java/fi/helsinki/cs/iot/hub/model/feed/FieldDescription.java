/*
 * fi.helsinki.cs.iot.hub.model.feed.Field
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
package fi.helsinki.cs.iot.hub.model.feed;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class FieldDescription {
	
	private String name;
    private String type;
    private String metadata;
    private boolean optional;
    private List<String> keywords;

    public FieldDescription(String name, String type, String metadata, boolean optional, List<String> keywords) {
        this.name = name;
        this.type = type;
        this.metadata = metadata;
        this.optional = optional;
        this.keywords = keywords;
    }

    public FieldDescription(String name, String type, String metadata, boolean optional) {
        this(name, type, metadata, optional, new ArrayList<String>());
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

	public boolean isOptional() {
		return optional;
	}

}
