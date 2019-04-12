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
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Rdfs;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.resources.dto.sensor.SensorProfileDTO;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyPostDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
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
    
    //a sensor uri (e.g. http://www.phenome-fppn.fr/diaphen/2018/s18001)
    public String uri;
    
    //The following attributes are used to search sensors in the triplestore
    private final String RELATION = "relation";
    private final String PROPERTY = "property";
    
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        query.appendSelect("?" + RELATION + " ?" + PROPERTY);
        query.appendTriplet("<" + uri + ">", "?" + RELATION, "?" + PROPERTY, null);
        query.appendTriplet("<" + uri + ">", Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_SENSING_DEVICE.toString(), null);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
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
                    
                    if (!uriDaoSesame.isSubClassOf(rdfType, Oeso.CONCEPT_SENSING_DEVICE.toString())) {
                        validData = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "The type of the given uri is not a Sensing Device"));
                    }
                    
                    //3. check the given properties 
                    for (PropertyPostDTO propertyDTO : sensorProfile.getProperties()) {
                        //3.1 check if the property exist
                        uriDaoSesame.uri = propertyDTO.getRelation();
                        ArrayList<Ask> uriExistance = uriDaoSesame.askUriExistance();
                        if (uriExistance.get(0).getExist()) {
                            //3.2 check the domain of the property
                            String propertyRelationUri = propertyDTO.getRelation();
                            propertyDAO.setRelation(propertyRelationUri);
                            ArrayList<String> propertyDomains = propertyDAO.getPropertyDomain(propertyRelationUri);
                            
                            if (propertyDomains != null && propertyDomains.size() > 0) { //the property has a specific domain
                                boolean domainOk = false;
                                for (String propertyDomain : propertyDomains) {
                                    if (uriDaoSesame.isSubClassOf(rdfType, propertyDomain)) {
                                        domainOk = true;
                                        }
                                }
                                
                                if (!domainOk) {
                                    validData = false;
                                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, 
                                            "the type of the given uri is not in the domain of the relation " + propertyDTO.getRelation()));
                                }
                            }
                            
                        } else {
                            validData = false;
                            checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.UNKNOWN_URI + " " + propertyDTO.getRelation()));
                        }
                    }
                    
                    //4. check the properties cardinalities
                    POSTResultsReturn propertyCheckResult = propertyDAO.checkCardinalities(sensorProfile.getProperties(), sensorProfile.getUri(), rdfType);
                    
                    if (!propertyCheckResult.getDataState()) {
                        validData = false;
                        checkStatus.addAll(propertyCheckResult.statusList);
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
     *  GRAPH <http://www.opensilex.org/vocabulary/oeso#sensors> {
     *      <http://www.phenome-fppn.fr/diaphen/2019/s18143> <http://www.opensilex.org/vocabulary/oeso#wavelength> "150" . 
     * ....
     *  }
     * }
     */
    private UpdateRequest prepareInsertQuery(SensorProfile sensorProfile) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.SENSORS.toString());
        Resource sensorProfileUri = ResourceFactory.createResource(sensorProfile.getUri());
        
        for (Property property : sensorProfile.getProperties()) {
            org.apache.jena.rdf.model.Property propertyRelation = ResourceFactory.createProperty(property.getRelation());
            
            if (property.getRdfType() != null) {
                Node propertyValue = NodeFactory.createURI(property.getValue());
                spql.addInsert(graph, sensorProfileUri, propertyRelation, propertyValue);
                spql.addInsert(graph,propertyValue, RDF.type, property.getRdfType());
            } else {
                Literal propertyValue = ResourceFactory.createStringLiteral(property.getValue());
                spql.addInsert(graph, sensorProfileUri, propertyRelation, propertyValue);
            }
        }
        
        UpdateRequest query = spql.buildRequest();
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        
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
            UpdateRequest query = prepareInsertQuery(sensorProfileDTO.createObjectFromDTO());
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
    
    /**
     * get a sensor property from a given binding set/
     * Assume that the following attributes exist : relation, property
     * @param bindingSetProperty a binding set from a sensor profile search query
     * @return a sensor property
     */
    private PropertyPostDTO getPropertyFromBingingSet(BindingSet bindingSetProperty) {
        PropertyPostDTO property = new PropertyPostDTO();
        
        property.setRelation(bindingSetProperty.getValue(RELATION).stringValue());
        property.setValue(bindingSetProperty.getValue(PROPERTY).stringValue());
        
        return property;        
    }
    
    /**
     * search all the sensors profiles corresponding to the given sensor uri
     * @return the list of the sensors profiles which match the given uri.
     */
    public ArrayList<SensorProfileDTO> allPaginate() {        
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<SensorProfileDTO> sensorsProfiles = new ArrayList<>();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            SensorProfileDTO sensorProfile = new SensorProfileDTO();
            while (result.hasNext()) {
                if (sensorProfile.getUri() == null) {
                    sensorProfile.setUri(uri);
                }
                BindingSet bindingSet = result.next();
                sensorProfile.addProperty(getPropertyFromBingingSet(bindingSet));
            }
            
            if (sensorProfile.getUri() != null) {
                sensorsProfiles.add(sensorProfile);
            }
        }
        
        return sensorsProfiles;
    }

    @Override
    public List<SensorProfile> create(List<SensorProfile> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<SensorProfile> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<SensorProfile> update(List<SensorProfile> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorProfile find(SensorProfile object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SensorProfile findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
