package org.opensilex.nosql.mongodb.dao;


import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.query.SearchFilter;

import java.net.URI;

public class MongoSearchFilter extends SearchFilter {

    URI uri;

    boolean logicalAnd;
    public static final boolean LOGICAL_AND_FOR_FILTERS_BY_DEFAULT = true;

    public MongoSearchFilter(){
        super();
        logicalAnd = LOGICAL_AND_FOR_FILTERS_BY_DEFAULT;
    }

    public boolean isLogicalAnd() {
        return logicalAnd;
    }

    public MongoSearchFilter setLogicalAnd(boolean logicalAnd) {
        this.logicalAnd = logicalAnd;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public MongoSearchFilter setUri(URI uri) {
        this.uri = uri;
        return this;
    }
}
