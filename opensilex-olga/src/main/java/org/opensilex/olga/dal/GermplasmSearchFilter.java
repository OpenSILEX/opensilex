package org.opensilex.olga.dal;

import org.bson.conversions.Bson;

import java.util.List;

public class GermplasmSearchFilter {

    public static final GermplasmSearchFilter EMPTY = new GermplasmSearchFilter();

    // Only used in order to store effective Bson without recomputing it from F filter (ex : for logging or count+find)
    private Bson filterBson;

    // Only used in order to store effective Bson without recomputing it from F filter (ex: when use it for logging
    private String filterBsonStr;

    private List<Bson> filterList;

    String germplasmName;

    boolean logicalAnd;

    /**
     * Define that we want a logical AND filter between each fields by default
     */
    public static final boolean LOGICAL_AND_FOR_FILTERS_BY_DEFAULT = true;

    /**
     * Construct new filter with {@link #isLogicalAnd()} equals to {@link #LOGICAL_AND_FOR_FILTERS_BY_DEFAULT}
     */
    public GermplasmSearchFilter() {
        super();
        logicalAnd = LOGICAL_AND_FOR_FILTERS_BY_DEFAULT;
    }

    /**
     * @return true if a logical AND semantic must be applied between each field's filters
     */
    public boolean isLogicalAnd() {
        return logicalAnd;
    }


    public GermplasmSearchFilter setLogicalAnd(boolean logicalAnd) {
        this.logicalAnd = logicalAnd;
        return this;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public GermplasmSearchFilter setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
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
