//******************************************************************************
//                                       AnnotationDAOSesame.java
//
// Author(s): Arnaud Charleroy<arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 juin 2018
// Subject:
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
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
import static phis2ws.service.dao.sesame.SensorDAOSesame.TRIPLESTORE_CONCEPT_SENSING_DEVICE;
import static phis2ws.service.dao.sesame.SensorDAOSesame.TRIPLESTORE_CONTEXT_SENSOR;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.model.User;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Annotation;
import phis2ws.service.resources.dto.AnnotationDTO;
import phis2ws.service.resources.dto.SensorDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Annotation;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class AnnotationDAOSesame extends DAOSesame<Annotation> {

    final static Logger LOGGER = LoggerFactory.getLogger(AnnotationDAOSesame.class);

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * check and insert the given annotations in the triplestore
     *
     * @param annotations
     * @return the insertion result. Message error if errors founded in data the
     * list of the generated uri of the sensors if the insertion has been done
     */
    public POSTResultsReturn checkAndInsert(List<AnnotationDTO> annotations) {
        POSTResultsReturn checkResult = check(annotations);
        if (checkResult.getDataState()) {
            return insert(annotations);
        } else { //errors founded in data
            return checkResult;
        }
    }

    /**
     * insert the given sensors in the triplestore
     *
     * @param sensorsDTO
     * @return the insertion result, with the errors list or the uri of the
     * inserted sensors
     */
    public POSTResultsReturn insert(List<AnnotationDTO> annotationsDTO) {
        List<Status> insertStatus = new ArrayList<>();
        List<String> createdResourcesUri = new ArrayList<>();

        POSTResultsReturn results;
        boolean resultState = false;
        boolean annotationInsert = true;

        UriGenerator uriGenerator = new UriGenerator();

        //SILEX:test
        //Triplestore connection has to be checked (this is kind of an hot fix)
        this.getConnection().begin();
        //\SILEX:test

        for (AnnotationDTO annotationDTO : annotationsDTO) {
            Annotation annotation = annotationDTO.createObjectFromDTO();
            annotation.setUri(uriGenerator.generateNewInstanceUri("annotation", null, null));

            SPARQLUpdateBuilder query = prepareInsertQuery(annotation);
            Update prepareUpdate = this.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
            prepareUpdate.execute();

            createdResourcesUri.add(annotation.getUri());
        }

        if (annotationInsert) {
            resultState = true;
            getConnection().commit();
        } else {
            getConnection().rollback();
        }

        results = new POSTResultsReturn(resultState, annotationInsert, true);
        results.statusList = insertStatus;
        results.setCreatedResources(createdResourcesUri);
        if (resultState && !createdResourcesUri.isEmpty()) {
            results.createdResources = createdResourcesUri;
            results.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, createdResourcesUri.size() + " new resource(s) created"));
        }

        if (getConnection() != null) {
            getConnection().close();
        }

        return results;
    }

    /**
     * generates an insert query for sensors. e.g. INSERT DATA { GRAPH
     * <http://www.phenome-fppn.fr/diaphen/sensors> {
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142> rdf:type
     * <http://www.phenome-fppn.fr/vocabulary/2017#Thermocouple> .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142> rdfs:label "par03_p" .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#hasBrand> "Homemade" .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#inServiceDate> "2017-06-15" .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#personInCharge>
     * "morgane.vidal@inra.fr" .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#serialNumber> "A1E345F32" .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#dateOfPurchase> "2017-06-15"
     * .
     * <http://www.phenome-fppn.fr/diaphen/2018/v18142>
     * <http://www.phenome-fppn.fr/vocabulary/2017#dateOfLastCalibration>
     * "2017-06-15" . } }
     *
     * @param sensor
     * @return the query
     */
    private SPARQLUpdateBuilder prepareInsertQuery(Annotation annotation) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();

//        query.appendGraphURI(TRIPLESTORE_CONTEXT_SENSOR);
//        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_TYPE, sensor.getRdfType(), null);
//        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_LABEL, "\"" + sensor.getLabel() + "\"", null);
//        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_BRAND, "\"" + sensor.getBrand() + "\"", null);
//        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_IN_SERVICE_DATE, "\"" + sensor.getInServiceDate() + "\"", null);
//        query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_PERSON_IN_CHARGE, "\"" + sensor.getPersonInCharge() + "\"", null);
//        
//        if (sensor.getSerialNumber() != null) {
//            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_SERIAL_NUMBER, "\"" + sensor.getSerialNumber() + "\"", null);
//        }
//        
//        if (sensor.getDateOfPurchase() != null) {
//            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_DATE_OF_PURCHASE, "\"" + sensor.getDateOfPurchase() + "\"", null);
//        }
//        
//        if (sensor.getDateOfLastCalibration() != null) {
//            query.appendTriplet(sensor.getUri(), TRIPLESTORE_RELATION_DATE_OF_LAST_CALIBRATION, "\"" + sensor.getDateOfLastCalibration() + "\"", null);
//        }
//        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }

    /**
     * check the given sensor's metadata
     *
     * @param annotations
     * @return the result with the list of the errors founded (empty if no error
     * founded)
     */
    public POSTResultsReturn check(List<AnnotationDTO> annotations) {
        POSTResultsReturn check = null;
        //list of the returned results
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;

        UriDaoSesame uriDao = new UriDaoSesame();
        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
        //1. check data
        for (AnnotationDTO annotation : annotations) {
            //1.1 check required fields
            if ((boolean) annotation.isOk().get(AbstractVerifiedClass.STATE)) {
                try {
                    //1.2 check motivation
                    final URINamespaces uriNamespaces = new URINamespaces();
                    if (!uriDao.isInstanceOf(annotation.getMotivatedBy(), uriNamespaces.getObjectsProperty("cMotivation"))) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, StatusCodeMsg.WRONG_VALUE + " for the motivatedBy field"));
                    }

                    //1.3 check if person exist
                    User u = new User(annotation.getCreator());
                    if (!userDao.existInDB(u)) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person email"));
                    }
                    //1.4 check if targets exist
                    for(String target : annotation.getTargets()){
                        if (!target.isEmpty() && !uriDao.existObject(target)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown target uri"));
                        }
                    }

                } catch (Exception ex) {
                    LOGGER.error("Data check error", ex);
                }
            } else { //Missing required fields
                dataOk = false;
                annotation.isOk().remove(AbstractVerifiedClass.STATE);
                checkStatus.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(annotation.isOk()).toString()));
            }
        }

        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }
}
