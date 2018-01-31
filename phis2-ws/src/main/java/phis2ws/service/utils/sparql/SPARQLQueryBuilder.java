//**********************************************************************************************
//                                       SPARQLQueryBuilder.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:eloan.lagier@inra.fr, arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  Janvier 29 , 2018
// Subject: A class which permit to build a SPARQL query
//***********************************************************************************************
package phis2ws.service.utils.sparql;

/**
 * Classe concrète qui permet de créer des requêtes de recherche en SPARQL
 *
 * @author Arnaud CHARLEROY
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
     * Ajout du sélect
     *
     * @param values
     */
    public void appendSelect(String values) {
        if (values != null) {
            this.select  += values;
        }
    }

    @Override
    public String toString() {
        String queryResource = "";
        if (prefix != null) {
            queryResource += prefix + "\n";
        }
        if (ask == null){
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
        if (ask !=null) {
            queryResource += "\n" + "ASK {"+ ask + " .";
        }   else{
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
        ask =  null;
    }
}
