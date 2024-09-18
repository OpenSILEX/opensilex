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
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class UriSearchLogic {
    /*private final SPARQLService sparql;
    private final MongoDBService nosql;*/
    private final UriSearchSparqlDao sparqlDao;

    public UriSearchLogic(SPARQLService sparqlService, AccountModel currentUser) {
        this.sparqlDao = new UriSearchSparqlDao(sparqlService, currentUser);
    }

    public List<BasicMongoSparqlDTO> searchByUri(URI uri) throws Exception {
        //Start by searching in sparql global graph
        List<SPARQLNamedResourceModel> sparqlMatches = sparqlDao.searchByUri(uri);
        return sparqlMatches.stream().map(BasicMongoSparqlDTO::fromSparqlNamedResourceModel).collect(Collectors.toList());
    }
}
