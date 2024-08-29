/*
 * ******************************************************************************
 *                                     UriSearchLogic.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.uriSearch.bll;

import org.opensilex.core.uriSearch.api.BasicMongoSparqlDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;

public class UriSearchLogic {
    /*private final SPARQLService sparql;
    private final MongoDBService nosql;*/
    private final UriSearchSparqlDao sparqlDao;

    public UriSearchLogic(SPARQLService sparqlService) {
        this.sparqlDao = new UriSearchSparqlDao(sparqlService);
    }

    public List<BasicMongoSparqlDTO> searchByUri(URI uri){
        //Start by searching in sparql global graph
        //List<SPARQLNamedResourceModel> sparqlMatches =
        return null;
    }
}
