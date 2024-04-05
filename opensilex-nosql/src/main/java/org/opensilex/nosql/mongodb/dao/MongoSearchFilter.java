package org.opensilex.nosql.mongodb.dao;


import org.bson.conversions.Bson;
import org.opensilex.sparql.service.SearchFilter;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class MongoSearchFilter extends SearchFilter {

    public static final MongoSearchFilter EMPTY = new MongoSearchFilter();

    // Only used in order to store effective Bson without recomputing it from F filter (ex : for logging or count+find)
    private Bson filterBson;

    // Only used in order to store effective Bson without recomputing it from F filter (ex: when use it for logging
    private String filterBsonStr;

    private List<Bson> filterList;

    URI uri;

    boolean logicalAnd;

    /**
     * Define that we want a logical AND filter between each fields by default
     */
    public static final boolean LOGICAL_AND_FOR_FILTERS_BY_DEFAULT = true;

    /**
     * Construct new filter with {@link #isLogicalAnd()} equals to {@link #LOGICAL_AND_FOR_FILTERS_BY_DEFAULT}
     */
    public MongoSearchFilter() {
        super();
        logicalAnd = LOGICAL_AND_FOR_FILTERS_BY_DEFAULT;
    }

    /**
     * @return true if a logical AND semantic must be applied between each field's filters
     */
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


    /**
     * @apiNote This method is package protected. No need to access to it outside the Dao classes which are in the same package.
     */
    Bson getFilterBson() {
        return filterBson;
    }

    /**
     * @apiNote This method is package protected. No need to access to it outside the Dao classes which are in the same package.
     */
    String getFilterBsonStr() {
        return filterBsonStr;
    }

    List<Bson> getFilterList() {
        return filterList;
    }

    void setFilterBson(Bson filterBson) {
        this.filterBson = filterBson;
    }

    void setFilterBsonStr(String filterBsonStr) {
        this.filterBsonStr = filterBsonStr;
    }

    void setFilterList(List<Bson> filterList) {
        this.filterList = filterList;
    }
}
