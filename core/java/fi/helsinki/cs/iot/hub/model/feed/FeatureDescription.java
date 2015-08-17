/*
 * fi.helsinki.cs.iot.hub.model.feed.FeatureDescription
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

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class FeatureDescription {

    private String name;
    private String type;

    public int describeContents() {
        return 0;
    }

    /*
     public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(FeatureUtils.featureTypeToString(type));
    }

    public static final Parcelable.Creator<FeatureDescription> CREATOR
            = new Parcelable.Creator<FeatureDescription>() {
        public FeatureDescription createFromParcel(Parcel in) {
            return new FeatureDescription(in);
        }

        public FeatureDescription[] newArray(int size) {
            return new FeatureDescription[size];
        }
    };

    private FeatureDescription(Parcel in) {
        name = in.readString();
        type = FeatureUtils.stringToFeatureType(in.readString());
    }
    */

    public FeatureDescription(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeatureDescription)) return false;

        FeatureDescription that = (FeatureDescription) o;

        if (!name.equals(that.name)) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
