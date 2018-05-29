//******************************************************************************
//                                       SensorProfileDAOSesame.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 28 mai 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  28 mai 2018
// Subject: access to the sensor's profiles in the triplestore
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.PropertyDTO;
import phis2ws.service.resources.dto.SensorProfileDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Property;
import phis2ws.service.view.model.phis.SensorProfile;
import phis2ws.service.view.model.phis.Uri;

/**
 * CRUD method of sensor's profiles, in the triplestore [rdf4j]
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class SensorProfileDAOSesame extends DAOSesame<SensorProfile> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(SensorProfile.class);
    
    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    
    private final static String TRIPLESTORE_CONCEPT_SENSING_DEVICE = NAMESPACES.getObjectsProperty("cSensingDevice");
    
    private final static String TRIPLESTORE_CONTEXT_SENSORS = NAMESPACES.getContextsProperty("sensors");
    
    private final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * check the given sensors profiles
     * @param sensorProfiles
     * @return the result with the list of the founded errors (empty if no errors)
     */
    public POSTResultsReturn check(List<SensorProfileDTO> sensorProfiles) {
        POSTResultsReturn sensorProfilesCheck = null;
        //list of the returned status
        List<Status> checkStatus = new ArrayList<>();
        boolean validData = true;
        
        //1. check if the user is an administrator
        UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
        if (userDAO.isAdmin(user)) {
            UriDaoSesame uriDaoSesame = new UriDaoSesame();
            PropertyDAOSesame propertyDAO = new PropertyDAOSesame();
            for (SensorProfileDTO sensorProfile : sensorProfiles) {
                //2. check if the given uri exist and is a sensor and keep the rdfType
                uriDaoSesame.uri = sensorProfile.getUri();
                ArrayList<Uri> urisTypes = uriDaoSesame.getAskTypeAnswer();
                if (urisTypes.size() > 0) {
                    String rdfType = urisTypes.get(0).getRdfType();
                    
                    if (!uriDaoSesame.isSubClassOf(rdfType, TRIPLESTORE_CONCEPT_SENSING_DEVICE)) {
                        validData = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "The type of the given uri is not a Sensing Device"));
                    }
                    
                    //3. check the given properties 
                    for (PropertyDTO propertyDTO : sensorProfile.getProperties()) {
                        //3.1 check if the property exist
                        uriDaoSesame.uri = propertyDTO.getRelation();
                        ArrayList<Ask> uriExistance = uriDaoSesame.askUriExistance();
                        if (uriExistance.get(0).getExist()) {
                            //3.2 check the domain of the property
                            propertyDAO.relation = propertyDTO.getRelation();
                            String propertyDomain = propertyDAO.getPropertyDomain();
                            
                            if (propertyDomain != null) { //the property has a specific domain
                                if (!uriDaoSesame.isSubClassOf(rdfType, propertyDomain)) {
                                    validData = false;
                                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, 
                                            "the type of the given uri is not in the domain of the relation " + propertyDTO.getRelation() + " (expected type " + propertyDomain + ")"));
                                }
                            }
                            
                        } else {
                            validData = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + propertyDTO.getRelation()));
                        }
                    }
                } else {
                    validData = false;
                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " or bad uri type " + sensorProfile.getUri()));
                }             
            }
        
            
        } else {
            validData = false;
            checkStatus.add(new Status(StatusCodeMsg.ACCESS_DENIED, StatusCodeMsg.ERR, StatusCodeMsg.ADMINISTRATOR_ONLY));
        }
        
        sensorProfilesCheck = new POSTResultsReturn(validData, null, validData);
        sensorProfilesCheck.statusList = checkStatus;
        return sensorProfilesCheck;        
    }
    
    /**
     * generates an insert query for the given sensor profile
     * @param sensorProfile
     * @return the query
     * e.g.
     * INSERT DATA {
     *  GRAPH <http://www.phenome-fppn.fr/vocabulary/2017#sensors> {
     *      <http://www.phenome-fppn.fr/diaphen/2019/s18143> <http://www.phenome-fppn.fr/vocabulary/2017#wavelength> "150" . 
     * ....
     *  }
     * }
     */
    private SPARQLUpdateBuilder prepareInsertQuery(SensorProfile sensorProfile) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();
        
        query.appendGraphURI(TRIPLESTORE_CONTEXT_SENSORS);
        
        for (Property property : sensorProfile.getProperties()) {
            if (property.getRdfType() != null) {
                query.appendTriplet(sensorProfile.getUri(), property.getRelation(), property.getValue(), null);
                query.appendTriplet(property.getValue(), TRIPLESTORE_RELATION_TYPE, property.getRdfType(), null);
            } else {
                query.appendTriplet(sensorProfile.getUri(), property.getRelation(), "\"" + property.getValue() + "\"", null);
            }
        }
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * insert the given sensor profiles in the triplestore. 
     * /!\ we assume that the data has been checked before calling this method
     * @see SensorProfileDAOSesame#check(java.util.List) 
     * @param sensorsProfiles
     * @return the insertion result, with the errors list or the uri of the 
     *         sensors concerned by the sensors profiles
     */
    private POSTResultsReturn insert(List<SensorProfileDTO> sensorsProfiles) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUris = new ArrayList<>();
        
        POSTResultsReturn results;
        boolean resultState = false; 
        boolean annotationInsert = true;
        
        this.getConnection().begin();
        for (SensorProfileDTO sensorProfileDTO : sensorsProfiles) {
            SPARQLUpdateBuilder query = prepareInsertQuery(sensorProfileDTO.createObjectFromDTO());
            Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();
            
            createdResourcesUris.add(sensorProfileDTO.getUri());
        }
        
        if (annotationInsert) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }
        
        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatus;
        if (resultState && !createdResourcesUris.isEmpty()) {
            results.createdResources = createdResourcesUris;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, "sensor(s) profile(s) updated"));
        }
        
        if (getConnection() != null) {
            getConnection().close();
        }
        
        return results;
    }
    
    /**
     * check and insert the given sensor profiles in the triplestore.
     * /!\ the sensor profiles properties depends of the sensor type. The list
     * of expected properties is extracted from the triplestore
     * @param sensorProfiles
     * @return the insertion result. Message error if errors founded in data, 
     *         the list of the uris of the sensors concerned by the given profiles 
     *         if the insertion has been done 
     */
    public POSTResultsReturn checkAndInsert(List<SensorProfileDTO> sensorProfiles) {
        POSTResultsReturn checkResult = check(sensorProfiles);
        if (checkResult.getDataState()) {
            return insert(sensorProfiles);
        } else { //errors founded in data
            return checkResult;
        }
    }
}
