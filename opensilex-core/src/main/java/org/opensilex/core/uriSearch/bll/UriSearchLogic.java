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

import org.opensilex.core.data.api.DataFileGetDTO;
import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataFileDaoV2;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.URITypesDTO;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.uriSearch.api.URIGlobalSearchDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    /**
     *
     * @param uri to search by
     * @return a URIGlobalSearchDTO or null if no result, to create the result we look in sparql and different mongo
     * collections, these steps are divided into private functions for readability
     * @throws Exception
     */
    public URIGlobalSearchDTO searchByUri(URI uri) throws Exception {
        //Start by searching in sparql global graph
        URIGlobalSearchDTO result = this.searchInSparql(uri);
        if(result != null){
            return result;
        }

        //If no matches search in Vocabulary
        result = this.searchInOntologies(uri);
        if(result != null){
            return result;
        }

        //If no matches search in Provenance
        result = this.searchInProvenances(uri);
        if(result != null){
            return result;
        }

        //If still no matches then search in Data
        result = this.searchInData(uri);
        if(result != null){
            return result;
        }

        //If still no match then search in Datafile
        result = this.searchInDataFiles(uri);

        return result;
    }

    private URIGlobalSearchDTO searchInSparql(URI uri) throws Exception {
        UriSearchSparqlDao.SparqlNamedResourceModelWithExtraStuff sparqlMatch = sparqlDao.searchByUri(uri);
        if(sparqlMatch == null){
            return null;
        }
        URIGlobalSearchDTO result = URIGlobalSearchDTO.fromSparqlUriGlobalSearchResult(sparqlMatch);

        //Get super types, needed to identify details page path in front
        List<URITypesDTO> types = getSuperTypesFromUri(uri);

        result.setSuperTypes(types);
        return result;
    }

    private URIGlobalSearchDTO searchInOntologies(URI uri) throws Exception {
        ClassModel model = null;

        try{
            //these two lines were taken from OntologyApi, i don't understand if i should be using OntologyDao or the OntologyStore
            OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
            model = ontologyStore.getClassModel(uri, null, currentUser.getLanguage());
        }catch(Exception e){
            return null;
        }
        URIGlobalSearchDTO result = URIGlobalSearchDTO.fromClassModel(model);
        //Get super types, needed to identify details page path in front
        List<URITypesDTO> types = getSuperTypesFromUri(uri);
        result.setSuperTypes(types);

        return result;
    }

    private URIGlobalSearchDTO searchInProvenances(URI uri) throws Exception {
        ProvenanceModel provenanceModel = null;
        try{
            //TODO dont invoke dao here when prov logic class exists
            ProvenanceDaoV2 provenanceDaoV2 = new ProvenanceDaoV2(nosql.getServiceV2());
            provenanceModel = provenanceDaoV2.get(uri);
        }catch(Exception notFound){
            return null;
        }

        URIGlobalSearchDTO result = URIGlobalSearchDTO.fromMongoModel(provenanceModel).setName(provenanceModel.getName());
        //prepare publisher info
        loadPublisherInfoIntoDtoFromMongoModel(provenanceModel, result);

        //Create a singleton list for provenance type, needed for redirection in front
        URITypesDTO type = new URITypesDTO();
        type.setUri(uri);
        type.setRdfTypes(Collections.singletonList(URI.create(Oeso.Provenance.getURI())));
        result.setSuperTypes(Collections.singletonList(type));

        //TODO temporary forcing of Provenance type-name and type as this field is currently always empty, done same for Data but is even worse because i couldn't find a Data RdfType, temporary forcing of Type label in front for Data.
        result.setType(URI.create(Oeso.Provenance.getURI()));
        setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, URI.create(Oeso.Provenance.getURI()));

        return result;
    }

    private URIGlobalSearchDTO searchInData(URI uri) throws Exception {
        DataModel dataModel = null;
        try{
            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
            dataModel = dataLogic.get(uri);
        }catch(Exception notFound){
            return null;
        }

        URIGlobalSearchDTO result = URIGlobalSearchDTO.fromMongoModel(dataModel);

        //prepare publisher info
        loadPublisherInfoIntoDtoFromMongoModel(dataModel, result);

        //Set DataDto
        Set<URI> dateVariables = new VariableDAO(sparql, nosql, fs, currentUser).getAllDateVariables();
        result.setDataDto(DataGetSearchDTO.getDtoFromModel(dataModel, dateVariables));

        return result;
    }

    private URIGlobalSearchDTO searchInDataFiles(URI uri) throws Exception {
        DataFileModel dataFileModel = null;
        try{
            DataFileDaoV2 dataFileDaoV2 = new DataFileDaoV2(nosql, sparql);
            dataFileModel = dataFileDaoV2.get(uri);
        }catch(Exception notFound){
            return null;
        }

        URIGlobalSearchDTO result = URIGlobalSearchDTO.fromMongoModel(dataFileModel);

        //prepare publisher info
        loadPublisherInfoIntoDtoFromMongoModel(dataFileModel, result);

        //Type label
        setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, result.getType());

        //Set DatafileDto
        result.setDatafileDto(DataFileGetDTO.fromModel(dataFileModel));

        return result;
    }

    private <T extends MongoModel> void loadPublisherInfoIntoDtoFromMongoModel(T model, URIGlobalSearchDTO result) throws Exception {
        AccountModel publisherAccount = new AccountDAO(sparql).get(model.getPublisher());
        UserGetDTO publisherAsUser = new UserGetDTO();
        publisherAsUser.setUri(publisherAccount.getUri());

        PersonModel publisherPerson = publisherAccount.getLinkedPerson();
        if(publisherPerson != null){
            publisherAsUser.setFirstName(publisherPerson.getFirstName());
            publisherAsUser.setLastName(publisherPerson.getLastName());
        }

        result.setPublisher(publisherAsUser);
    }

    /**
     * Used to calculate routes in front-end
     *
     * @param uri of object
     * @return Super types as URITypesDTO
     * @throws Exception
     */
    private List<URITypesDTO> getSuperTypesFromUri(URI uri) throws Exception {
        //TODO dont invoke ontology dao here
        OntologyDAO dao = new OntologyDAO(sparql);

        return dao.getSuperClassesByURI(Collections.singletonList(uri))
                .stream().map(URITypesDTO::fromModel)
                .collect(Collectors.toList());
    }

    /**
     * Function to set type label from a rdfType, used for mongoModels
     *
     */
    private void setTypeLabelOfBasicMongoSparqlDTOfromRdfType(URIGlobalSearchDTO dto, URI rdfType) throws Exception {
        if(rdfType != null){
            //TODO dont invoke dao here when ontol logic class exists
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            dto.setTypeLabel(ontologyDAO.getURILabel(rdfType, currentUser.getLanguage()));
        }
    }
}
