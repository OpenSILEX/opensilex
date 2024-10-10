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

import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.URITypesDTO;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.uriSearch.api.BasicMongoSparqlDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
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

    public BasicMongoSparqlDTO searchByUri(URI uri) throws Exception {
        //Start by searching in sparql global graph
        UriSearchSparqlDao.SparqlNamedResourceModelWithExtraStuff sparqlMatch = sparqlDao.searchByUri(uri);
        if(!(sparqlMatch == null)){
            BasicMongoSparqlDTO result = BasicMongoSparqlDTO.fromSparqlUriGlobalSearchResult(sparqlMatch);

            //Get super types, needed to identify details page path in front
            List<URITypesDTO> types = getSuperTypesFromUri(uri);

            result.setSuperTypes(types);
            return result;
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
            //prepare publisher info
            loadPublisherInfoIntoDtoFromMongoModel(provenanceModel, result);

            //Create a singleton list for provenance type, needed for redirection in front
            URITypesDTO type = new URITypesDTO();
            type.setUri(uri);
            type.setRdfTypes(Collections.singletonList(URI.create(Oeso.Provenance.getURI())));
            result.setSuperTypes(Collections.singletonList(type));

            //setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, provenanceModel.getRdfType());
            //TODO temporary forcing of Provenance type-name and type as this field is currently always empty, replace with above line when this is no longer the case
            result.setType(URI.create(Oeso.Provenance.getURI()));
            setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, URI.create(Oeso.Provenance.getURI()));

            return result;
        }

        //If still no matches then search in Data
        DataModel dataModel = null;
        try{
            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
            dataModel = dataLogic.get(uri);
        }catch(Exception ignore){}

        if(dataModel != null){
            BasicMongoSparqlDTO result = BasicMongoSparqlDTO.fromMongoModel(dataModel);

            //prepare publisher info
            loadPublisherInfoIntoDtoFromMongoModel(dataModel, result);

            //Set DataDto
            Set<URI> dateVariables = new VariableDAO(sparql, nosql, fs, currentUser).getAllDateVariables();
            result.setDataDto(DataGetSearchDTO.getDtoFromModel(dataModel, dateVariables));

            //setTypeLabelOfBasicMongoSparqlDTOfromRdfType(result, dataModel.getRdfType());
            //TODO above line if Data will have an rdfType

            return result;
        }

        return null;
    }

    private <T extends MongoModel> void loadPublisherInfoIntoDtoFromMongoModel(T model, BasicMongoSparqlDTO result) throws Exception {
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
     * TODO pointless? both data and provenance dont seem to have a rdfType, should we instead manually force a typeLabel?
     */
    private void setTypeLabelOfBasicMongoSparqlDTOfromRdfType(BasicMongoSparqlDTO dto, URI rdfType) throws Exception {
        if(rdfType != null){
            //TODO dont invoke dao here when ontol logic class exists
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);
            dto.setTypeLabel(ontologyDAO.getURILabel(rdfType, currentUser.getLanguage()));
            //dto.setTypeLabel(ontologyDAO.getRdfType(rdfType, currentUser.getLanguage()).getTypeLabel().getDefaultValue());
        }
    }
}
