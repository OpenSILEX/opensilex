package org.opensilex.nosql.mongodb.metadata;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;


public class MetadataSearchFilter extends MongoSearchFilter {
    private Document attributes;

    public Document getAttributes() {
        return attributes;
    }

    public void setAttributes(Document attributes) {
        this.attributes = attributes;
    }
}

