package org.opensilex.nosql.mongodb.metadata;

import org.opensilex.nosql.mongodb.MongoModel;

import java.util.Map;

/**
 * @author rcolin
 * Model use to store any attributes as a {@link Map} into MongoDB
 */
public class MetaDataModel extends MongoModel {

    private Map<String,String> attributes;

    public static final String ATTRIBUTES_FIELD = "attributes";

    public MetaDataModel() {
    }

    public MetaDataModel(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
