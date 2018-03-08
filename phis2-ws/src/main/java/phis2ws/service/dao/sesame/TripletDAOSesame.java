//******************************************************************************
//                                       TripletsDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject:
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.OType;
import phis2ws.service.resources.dto.TripletDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Triplet;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TripletDAOSesame extends DAOSesame<Triplet> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(TripletDAOSesame.class);

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * check each triplet's values 
     * @param tripletsGroup
     * @return the check of the triplets. Contains the list of the errors founded
     */
    public POSTResultsReturn checkTripletsGroup(ArrayList<TripletDTO> tripletsGroup) {
        //returned status list
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        //check triplets
        for (TripletDTO tripletDTO : tripletsGroup) {
            if ((boolean) tripletDTO.isOk().get(AbstractVerifiedClass.STATE)) {
                UriDaoSesame uriDaoSesame = new UriDaoSesame();
                
                //1. check if triplet.s is an uri and if it exist
                UrlValidator urlValidator = new UrlValidator();
                if (urlValidator.isValid(tripletDTO.getS())) {
                    if (!uriDaoSesame.existObject(tripletDTO.getS())) { //unknown uri
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getS()));
                    }
                } else { //malformed uri
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_URI + " " + tripletDTO.getS()));
                }
                
                //2. check if triplet.p is an existing relation
                if (!uriDaoSesame.existObject(tripletDTO.getP())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getP()));
                }
                
                //3. check if triplet.o_type is equals to "literal" or "uri" 
                //   and check the value of triplet.o
                if (tripletDTO.getO_type().equals(OType.URI.toString())) { //if value is supposed to be an uri
                    // check if triplet.o is a valid uri
                    if (urlValidator.isValid(tripletDTO.getO())) {
                        //if the uri does not exist in the triplestore, error
                        if (!uriDaoSesame.existObject(tripletDTO.getO())) {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + tripletDTO.getO()));
                        }
                    } else {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_URI + " " + tripletDTO.getO()));
                    }
                } else if (tripletDTO.getO_type().equals(OType.LITERAL.toString())) {
                    if (tripletDTO.getO_lang() != null) {
                        //if the triplet.o_lang isn't an ISO language
                        if (!Arrays.asList(Locale.getISOLanguages()).contains(tripletDTO.getO_lang())) {
                            dataOk = false;
                            checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.BAD_DATA_FORMAT + " " + tripletDTO.getO_lang()));
                        }
                    }
                } else {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, StatusCodeMsg.BAD_DATA_FORMAT + " " + tripletDTO.getO_type()));
                }
            } else { //missing data
                dataOk = false;
                tripletDTO.isOk().remove(AbstractVerifiedClass.STATE);
                checkStatusList.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(tripletDTO.isOk()).toString()));
            }
        }
        
        POSTResultsReturn tripletsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        tripletsCheck.statusList = checkStatusList;
        return tripletsCheck;
    }
    
    /**
     * for each list of triplets, check if there are some errors and return them
     * @see TripletDAOSesame#checkTripletsGroup(java.util.ArrayList) 
     * @param triplets
     * @return 
     */
    public POSTResultsReturn check(ArrayList<ArrayList<TripletDTO>> triplets) {
        //returned status list
        List<Status> checkStatusList = new ArrayList<>();
        boolean dataOk = true;
        
        for (ArrayList<TripletDTO> tripletsGroup : triplets) {
            POSTResultsReturn tripletsGroupCheck = checkTripletsGroup(tripletsGroup);
            if (!tripletsGroupCheck.getDataState()) {
                dataOk = false;
                checkStatusList.addAll(tripletsGroupCheck.getStatusList());
            }
        }
        
        POSTResultsReturn tripletsCheck = new POSTResultsReturn(dataOk, null, dataOk);
        tripletsCheck.statusList = checkStatusList;
        
        return tripletsCheck;
    }
    
    public SPARQLUpdateBuilder prepareInsertQuery(TripletDTO triplet, String graphUri) {
        SPARQLUpdateBuilder insertQuery = new SPARQLUpdateBuilder();
        
        insertQuery.appendGraphURI(graphUri);
        
        String object = triplet.getO_lang() != null ? "\"" + triplet.getO() + "\"@" + triplet.getO_lang() : triplet.getO();
        
        insertQuery.appendTriplet(triplet.getS(), triplet.getP(), object, null);
        
        return insertQuery;
    }
    
    public POSTResultsReturn insertTripletsGroup(ArrayList<TripletDTO> tripletsGroup, String graphUri) {
        //SILEX:todo
        //add transaction
        //\SILEX:todo
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        boolean tripletInserted = true;
        boolean resultState = false;
        
        final Iterator<TripletDTO> iteratorTriplets = tripletsGroup.iterator();
        
        while(iteratorTriplets.hasNext() && tripletInserted) {
            TripletDTO tripletDTO = iteratorTriplets.next();
            try {
                //SILEX:test
                //All the triplestore connection has to been checked and updated
                //This is an unclean hot fix
                String sesameServer = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "sesameServer");
                String repositoryID = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILENAME, "repositoryID");
                rep = new HTTPRepository(sesameServer, repositoryID); //Stockage triplestore Sesame
                rep.initialize();
                this.setConnection(rep.getConnection());
                this.getConnection().begin();

                //Register triplet in the triplestore
                SPARQLUpdateBuilder insertQuery = prepareInsertQuery(tripletDTO, graphUri);
                LOGGER.debug("SPARQL query : " + insertQuery.toString());
                Update prepareInsert = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertQuery.toString());
                prepareInsert.execute();
                if (tripletDTO.getG() != null) {
                    SPARQLUpdateBuilder insertInGivenGraph = prepareInsertQuery(tripletDTO, tripletDTO.getG());
                    LOGGER.debug("SPARQL query : " + insertInGivenGraph.toString());
                    Update prepareInsertInGivenGraph = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, insertInGivenGraph.toString());
                    prepareInsertInGivenGraph.execute();
                }
                //\SILEX:test
                createdResourcesUris.add(tripletDTO.getS());
                
                if (tripletInserted) {
                    resultState = true;
                    getConnection().commit();
                } else {
                    getConnection().rollback();
                }
            } catch (RepositoryException ex) {
                    LOGGER.error(StatusCodeMsg.COMMIT_TRIPLESTORE_ERROR, ex);
            } catch (MalformedQueryException e) {
                   LOGGER.error(e.getMessage(), e);
                   tripletInserted = false;
                   insertStatus.add(new Status(StatusCodeMsg.QUERY_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.MALFORMED_CREATE_QUERY + " : " + e.getMessage()));
            } 
        }
        
        POSTResultsReturn result = new POSTResultsReturn(resultState, tripletInserted, true);
        result.statusList = insertStatus;
        result.setCreatedResources(createdResourcesUris);
        if (resultState && !createdResourcesUris.isEmpty()) {
            result.createdResources = createdResourcesUris;
            result.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUris.size() + " new resource(s)"));
        }
        
        return result;
    }
    
    /**
     * for each group of triplets, insert them in the triplestore
     * @see POSTResultsReturn
     * @see TripletDAOSesame#insertTripletsGroup(java.utils.ArrayList)
     * @param triplets
     * @return the insertion result. Contains the list of created resources
     */
    public POSTResultsReturn insert(ArrayList<ArrayList<TripletDTO>> triplets, String graphUri) {
        //SILEX:todo
        //add transaction
        //\SILEX:todo
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        boolean tripletInserted = true;
        
        final Iterator<ArrayList<TripletDTO>> iteratorTripletsGroups = triplets.iterator();
        while (iteratorTripletsGroups.hasNext() && tripletInserted) {
            ArrayList<TripletDTO> tripletsGroup = iteratorTripletsGroups.next();
            //insert group in triplestore
            POSTResultsReturn insertionGroupResult = insertTripletsGroup(tripletsGroup, graphUri);
            if (insertionGroupResult.getInsertState()) { //insertion has been done
                createdResourcesUris.addAll(insertionGroupResult.getCreatedResources());
                insertStatus.addAll(insertionGroupResult.getStatusList());
            } else {
                tripletInserted = false;
            }
        }
        
        POSTResultsReturn insertResult = new POSTResultsReturn(tripletInserted, tripletInserted, true);
        insertResult.setStatusList(insertStatus);
        if (tripletInserted && !createdResourcesUris.isEmpty()) {
            insertResult.setCreatedResources(createdResourcesUris);
            insertResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.ERR, createdResourcesUris.size() + " " + StatusCodeMsg.RESOURCES_CREATED));
        }
        
        return insertResult;
    }
    
    /**
     * check if there are some errors in the triplets list when no error founded
     * insert them in the triplestore
     * @param triplets
     * @see POSTResultsReturn
     * @see TripletDAOSesame#check(java.util.ArrayList) 
     * @see TripletDAOSesame#insert(java.util.ArrayList)
     * @return the list of errors founded or the list of created resources
     */
    public POSTResultsReturn checkAndInsert(ArrayList<ArrayList<TripletDTO>> triplets, String graphUri) {
        POSTResultsReturn checkResults = check(triplets);
        if (checkResults.getDataState()) { //valid data
            return insert(triplets, graphUri);
        } else { //errors founded
            return checkResults; 
        }
    }
}
