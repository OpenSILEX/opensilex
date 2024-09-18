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

import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataDaoV2;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.uriSearch.api.BasicMongoSparqlDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.fs.service.FileStorageService;
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
    private final MongoDBService nosql;
    private final AccountModel currentUser;
    private final FileStorageService fs;
    private final UriSearchSparqlDao sparqlDao;

    public UriSearchLogic(SPARQLService sparqlService, MongoDBService nosql, AccountModel currentUser, FileStorageService fs) {
        this.sparqlDao = new UriSearchSparqlDao(sparqlService, currentUser);
        this.nosql = nosql;
        this.sparql = sparqlService;
        this.currentUser = currentUser;
        this.fs = fs;
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
            ProvenanceDaoV2 provenanceDaoV2 = new ProvenanceDaoV2(nosql.getServiceV2());
            provenanceModel = provenanceDaoV2.get(uri);
        }catch(Exception ignore){}

        if(provenanceModel != null){
            BasicMongoSparqlDTO result = BasicMongoSparqlDTO.fromMongoModel(provenanceModel).setName(provenanceModel.getName());
            setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, provenanceModel.getRdfType());
            return Collections.singletonList(result);
        }

        //If still no matches then search in Data
        DataModel dataModel = null;
        try{
            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
            dataModel = dataLogic.get(uri);
        }catch(Exception ignore){}

        if(dataModel != null){
            BasicMongoSparqlDTO result = BasicMongoSparqlDTO.fromMongoModel(dataModel);
            setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, dataModel.getRdfType());
            return Collections.singletonList(result);
        }

        return Collections.emptyList();
    }

    /**
     * Function to set type label from a rdfType, used for mongoModels
     * TODO pointless? both data and provenance dont seem to have a rdfType, should we instead manually force a typeLabel?
     */
    private void setTypeLabelOfBasicMongoSparqlDTOfromRdfType(BasicMongoSparqlDTO dto, URI rdfType) throws Exception {
        if(rdfType != null){
            //TODO dont invoke dao here when ontol logic class exists
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            dto.setTypeLabel(ontologyDAO.getRdfType(rdfType, currentUser.getLanguage()).getTypeLabel().getDefaultValue());
        }
    }
}
