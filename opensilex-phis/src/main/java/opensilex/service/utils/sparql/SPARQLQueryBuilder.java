//******************************************************************************
//                               SPARQLQueryBuilder.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact: eloan.lagier@inra.fr, arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils.sparql;

/**
 * SPARQL query builder.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class SPARQLQueryBuilder extends SPARQLStringBuilder {

    private String where = null;
    private Boolean count = null;
    private Boolean distinct = null;
    private String orderBy = null;
    private Integer limit = null;
    private Integer offset = null;
    private String graph = null;
    private String optional = null;
    private String ask = null;
    
    // Permits to concatenate a list of values in sparql without repeating data.
    private String groupBy = null;
    
    public final static String GROUP_CONCAT_SEPARATOR = ",";

    public SPARQLQueryBuilder() {
        super();
    }

    public void appendFrom(String from) {
        this.from = from;
    }

    public void appendOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public void appendLimit(Integer limit) {
        this.limit = limit;
    }

    public void appendOffset(Integer offset) {
        this.offset = offset;
    }

    public void appendCount(Boolean count) {
        this.count = count;
    }

    public void appendDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public void appendGraph(String graph) {
        this.graph = graph;
    }

    public void appendOptional(String optional) {
        this.optional = optional;
    }

    public void appendAsk(String ask) {
        this.ask = ask;
    }

    /**
     * Appends a SELECT value.
     * @param values
     */
    public void appendSelect(String values) {
        if (values != null) {
            this.select += " " + values;
        }
    }
    /**
     * Appends a groupby for parameters not used in group concatenate function.
     * @param values 
     */
    public void appendGroupBy(String values) {
        if (values != null) {
            if(this.groupBy == null){
            this.groupBy = "";
        }
            this.groupBy += " " + values;
        }
    }
    
    /**
     * Adds group_concat values to SELECT.
     * Concatenate a list of values in sparql without repeating data.
     * @see https://en.wikibooks.org/wiki/SPARQL/Aggregate_functions
     * @example
     * (GROUP_CONCAT(DISTINCT ?bodyValue; SEPARATOR=",") AS ?bodyValues)
     * BodyValues 
     * "test,test2"
     * @param values the value name to separate
     * @param separator the separator betweend returned value
     * @param outputValueName the name of the labelled value
     */
    public void appendSelectConcat(String values,String separator,String outputValueName) {
        if (values != null) {
            this.select += " (GROUP_CONCAT(DISTINCT " + values + "; SEPARATOR=\"" + separator + "\") AS "+ outputValueName +")";
        }
    }
    
    @Override
    public String toString() {
        String queryResource = "";
        if (prefix != null) {
            queryResource += prefix + "\n";
        }
        if (ask == null) {
            if (count != null && count) {
                if (select.length() == 0) {
                    queryResource += "SELECT ( COUNT ( DISTINCT * ) as ?count) ";
                } else {
                    queryResource += "SELECT ( COUNT ( DISTINCT " + select + "  ) as ?count) ";
                }
            } else if (this.distinct != null) {
                if (select.length() == 0) {
                    queryResource += "SELECT DISTINCT * ";
                } else {
                    queryResource += "SELECT DISTINCT " + select + " ";
                }
            } else if (select.length() == 0) {
                queryResource += "SELECT * ";
            } else {
                queryResource += "SELECT " + select + " ";
            }
        }

        if (from != null) {
            queryResource += "FROM " + from + "\n";
        }
        if (ask != null) {
            if (ask.equals("")) {
                queryResource += "\n" + "ASK {";
            } else {
                queryResource += "\n" + "ASK {" + ask + " .";
            }

        } else {
            queryResource += "WHERE {\n";
        }

        if (graph != null) {
            queryResource += "GRAPH <" + graph + "> {";
        }
        queryResource += body;
        if (optional != null) {
            queryResource += "\nOPTIONAL {" + optional + " } \n";
        }
        if (!filter.isEmpty()) {
            queryResource += "\nFILTER ( " + filter + " ) \n";
        }
        if (graph != null) {
            queryResource += "}";
        }
        queryResource += "}";
        if (parameters.length() > 0) {
            queryResource += "\n" + parameters;
        }
        if (groupBy != null) {
            queryResource += "\n" + "GROUP BY " + groupBy + " ";
        }

        if (orderBy != null) {
            queryResource += "\n" + "ORDER BY " + orderBy + " ";
        }
        if (limit != null) {
            queryResource += "\n" + "LIMIT " + limit + " ";
        }
        if (offset != null) {
            queryResource += "\n" + "OFFSET " + offset + " ";
        }

        return queryResource;
    }

    @Override
    public void clearBuilder() {
        super.clearBuilder();
        count = null;
        distinct = null;
        limit = null;
        offset = null;
        orderBy = null;
        where = null;
        filter = "";
        optional = null;
        ask = null;
    }

    public void clearSelect() {
        select = "";
    }

    public void clearLimit() {
        limit = null;
    }

    public void clearOffset() {
        offset = null;
    }
    
    public void clearGroupBy() {
        groupBy = null;
    }

    public void clearOrderBy() {
        orderBy = null;
    }
}
