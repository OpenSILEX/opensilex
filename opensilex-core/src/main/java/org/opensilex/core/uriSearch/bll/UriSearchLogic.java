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

import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.uriSearch.api.BasicMongoSparqlDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UriSearchLogic {
    private final SPARQLService sparql;
    private final MongoDBServiceV2 nosql;
    AccountModel currentUser;
    private final UriSearchSparqlDao sparqlDao;

    public UriSearchLogic(SPARQLService sparqlService, MongoDBServiceV2 nosql, AccountModel currentUser) {
        this.sparqlDao = new UriSearchSparqlDao(sparqlService, currentUser);
        this.nosql = nosql;
        this.sparql = sparqlService;
        this.currentUser = currentUser;
    }

    public List<BasicMongoSparqlDTO> searchByUri(URI uri) throws Exception {
        //Start by searching in sparql global graph
        List<SPARQLNamedResourceModel> sparqlMatches = sparqlDao.searchByUri(uri);
        if(!sparqlMatches.isEmpty()){
            return sparqlMatches.stream().map(BasicMongoSparqlDTO::fromSparqlNamedResourceModel).collect(Collectors.toList());
        }
        //If no matches search in Provenance
        ProvenanceModel provenanceModel = null;
        try{
            //TODO dont invoke dao here when prov logic class exists
            ProvenanceDaoV2 provenanceDaoV2 = new ProvenanceDaoV2(nosql);
            provenanceModel = provenanceDaoV2.get(uri);
        }catch(Exception ignore){}

        if(provenanceModel != null){
            BasicMongoSparqlDTO result = BasicMongoSparqlDTO.fromMongoModel(provenanceModel).setName(provenanceModel.getName());
            if(provenanceModel.getRdfType() != null){
                //TODO dont invoke dao here when ontol logic class exists
                OntologyDAO ontologyDAO = new OntologyDAO(sparql);
                result.setTypeLabel(ontologyDAO.getRdfType(provenanceModel.getRdfType(), currentUser.getLanguage()).getTypeLabel().getDefaultValue());
            }
            return Collections.singletonList(result);
        }

        return Collections.emptyList();
    }
}
