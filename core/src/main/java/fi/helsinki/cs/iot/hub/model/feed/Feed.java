/*
 * fi.helsinki.cs.iot.hub.model.feed.Feed
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

import java.util.List;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public abstract class Feed {

    private long id;
    private String name;
    private String metadata;
    public List<String> keywords;

    public Feed(long id, String name, String metadata, List<String> keywords) {
        this.id = id;
        this.name = name;
        this.metadata = metadata;
        this.keywords = keywords;
    }

    public final long getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final String getMetadata() {
        return metadata;
    }

    public final void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public final List<String> getKeywords() {
        return keywords;
    }

    public final void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public abstract FeedType getFeedType ();

    public abstract String getDescription();
    public abstract String getValue();
    public abstract boolean postValue(String json);
}
