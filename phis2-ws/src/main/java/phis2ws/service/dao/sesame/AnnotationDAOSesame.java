//******************************************************************************
//                                       AnnotationDAOSesame.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  21 juin 2018
// Subject: This class manages operation on annotation in database
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.resources.dto.AnnotationDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.utils.sparql.SPARQLUpdateBuilder;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Annotation;

/**
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationDAOSesame extends DAOSesame<Annotation> {

    final static Logger LOGGER = LoggerFactory.getLogger(AnnotationDAOSesame.class);

    private final static URINamespaces NAMESPACES = new URINamespaces();

    final static String TRIPLESTORE_CONTEXT_ANNOTATION = NAMESPACES.getContextsProperty("annotations");

    final static String TRIPLESTORE_RELATION_TYPE = NAMESPACES.getRelationsProperty("type");
    final static String TRIPLESTORE_RELATION_BODYVALUE = NAMESPACES.getRelationsProperty("rOaBodyValue");
    final static String TRIPLESTORE_RELATION_CREATOR = NAMESPACES.getRelationsProperty("rDCCreator");
    final static String TRIPLESTORE_RELATION_CREATED = NAMESPACES.getRelationsProperty("rDCCreated");
    final static String TRIPLESTORE_RELATION_TARGET = NAMESPACES.getRelationsProperty("rOaTarget");
    final static String TRIPLESTORE_RELATION_MOTIVATEDBY = NAMESPACES.getRelationsProperty("rOaMotivatedBy");

    // Search parameters
    public String uri;
    public static final String URI = "uri";
    public String created;
    public static final String CREATED = "created";
    public String bodyValue;
    public static final String BODYVALUE = "bodyValue";
    public String creator;
    public static final String CREATOR = "creator";
    public ArrayList<String> targets = new ArrayList<>();
    public static final String TARGET = "target";
    public String motivatedBy;
    public static final String MOTIVATEDBY = "motivatedBy";

    /**
     * 
     * @inheritdoc 
     */
    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);

        String annotationUri;
        if (uri != null) {
            annotationUri = "<" + uri + ">";
        } else {
            annotationUri = "?" + URI;
            query.appendSelect(annotationUri);
        }

        DateTime stringToDateTime = null;
        if (created != null) {
            stringToDateTime = Dates.stringToDateTime(created);
            if (stringToDateTime != null) {
                query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_CREATED, created, null);
            }
        }
        if (created == null && stringToDateTime == null) {
            query.appendSelect("?" + CREATED);
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_CREATED, "?" + CREATED, null);
        }

        if (creator != null) {
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_CREATOR, creator, null);
        } else {
            query.appendSelect(" ?" + CREATOR);
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_CREATOR, "?" + CREATOR, null);
        }

        if (motivatedBy != null) {
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_MOTIVATEDBY, motivatedBy, null);
        } else {
            query.appendSelect(" ?" + MOTIVATEDBY);
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_MOTIVATEDBY, "?" + MOTIVATEDBY, null);
        }

        if (targets != null && !targets.isEmpty()) {
            UriDaoSesame uriDao = new UriDaoSesame();
            for (String target : targets) {
                if (uriDao.existObject(target)) {
                    query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_TARGET, target, null);
                }

            }
        } else {
            query.appendSelect("?" + TARGET);
            query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_TARGET, "?" + TARGET, null);
        }

        query.appendSelect("?" + BODYVALUE);
        query.appendTriplet(annotationUri, TRIPLESTORE_RELATION_BODYVALUE, "?" + BODYVALUE, null);
        if (bodyValue != null) {
            query.appendFilter("regex(STR(?" + BODYVALUE + "), '" + bodyValue + "', 'i')");
        }

        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize());
        LOGGER.debug(SPARQL_SELECT_QUERY + query.toString());
        return query;

    }

    /**
     * 
     * @inheritdoc 
     */
    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        SPARQLQueryBuilder prepareCount = prepareCount();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }

        return count;
    }

    private SPARQLQueryBuilder prepareCount() {
        SPARQLQueryBuilder query = this.prepareSearchQuery();
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.appendSelect("(count(distinct ?" + URI + ") as ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_SELECT_QUERY + " " + query.toString());
        return query;
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
     * insert the given annotations in the triplestore
     *
     * @param annotationsDTO
     * @return the insertion result, with the errors list or the uri of the
     * inserted annotations
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
            annotation.setUri(uriGenerator.generateNewInstanceUri(NAMESPACES.getObjectsProperty("cAnnotation"), null, null));

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
     * generates an insert query for annotations. e.g. INSERT DATA {
     * <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37>
     * rdf:type  <http://www.w3.org/ns/oa#Annotation> .
     * <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37>
     * <http://purl.org/dc/terms/created>
     * "2018-06-22T15:18:13+0200"^^xsd:dateTime .
     * <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37>
     * <http://purl.org/dc/terms/creator>
     * <http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy> .
     * <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37>
     * <http://www.w3.org/ns/oa#bodyValue> "Ustilago maydis infection" .
     * <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37>
     * <http://www.w3.org/ns/oa#hasTarget>
     * <http://www.phenome-fppn.fr/diaphen/id/agent/acharleroy> . }
     *
     * @param annotation
     * @return the query
     */
    private SPARQLUpdateBuilder prepareInsertQuery(Annotation annotation) {
        SPARQLUpdateBuilder query = new SPARQLUpdateBuilder();

        query.appendGraphURI(TRIPLESTORE_CONTEXT_ANNOTATION);
        query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_TYPE, NAMESPACES.getObjectsProperty("cAnnotation"), null);
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormats.YMDTHMSZ_FORMAT);
        query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_CREATED, "\"" + annotation.getCreated().toString(formatter) + "\"^^xsd:dateTime", null);
        query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_CREATOR, annotation.getCreator(), null);
        query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_BODYVALUE, "\"" + annotation.getBodyValue() + "\"", null);
        query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_MOTIVATEDBY, annotation.getMotivatedBy(), null);

        if (annotation.getTargets() != null && !annotation.getTargets().isEmpty()) {
            for (String targetUri : annotation.getTargets()) {
                query.appendTriplet(annotation.getUri(), TRIPLESTORE_RELATION_TARGET, targetUri, null);
            }
        }
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return query;
    }

    /**
     * check the given annotations's metadata
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
//        UserDaoPhisBrapi userDao = new UserDaoPhisBrapi();
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

                    //1.3 check if person exist // MySQL
//                    User u = new User(annotation.getCreator());
//                    if (!userDao.existInDB(u)) {
//                        dataOk = false;
//                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person email"));
//                    }
                    //1.3 check if person exist // Sesame
                    if (!uriDao.existObject(annotation.getCreator())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown person uri"));
                    }

                    //1.4 check if targets exist
                    for (String target : annotation.getTargets()) {
                        if (target.isEmpty() || !uriDao.existObject(target)) {
                            dataOk = false;
                            checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown target uri"));
                        }
                    }
                    //1.5 check if motivation exists and is valid
                    if (annotation.getMotivatedBy() == null || !uriDao.existObject(annotation.getMotivatedBy())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.UNKNOWN_URI, StatusCodeMsg.ERR, "Unknown motivation"));
                    }

                } catch (Exception ex) {
                    LOGGER.error("Data check error", ex);
                }
            } else { //Missing required fields
                dataOk = false;
                Map<String, Object> fieldsNotValid = annotation.isOk();
                fieldsNotValid.remove(AbstractVerifiedClass.STATE);
                checkStatus.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(fieldsNotValid).toString()));
            }
        }

        check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }

    /**
     * search all the sensors corresponding to the search params given by the
     * user
     *
     * @return the list of the sensors which match the given search params.
     */
    public ArrayList<Annotation> allPaginate() {
        SPARQLQueryBuilder query = prepareSearchQuery();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Annotation> annotations = new ArrayList<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Annotation annotation = getAnnotationFromBindingSet(bindingSet);
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     * get a annotation from a given binding set. Assume that the following
     * attributes exist : uri, creator, created, bodyValue
     *
     * @param bindingSet a bindingSet from a search query
     * @return a annotation with data extracted from the given bindingSet
     */
    private Annotation getAnnotationFromBindingSet(BindingSet bindingSet) {
        Annotation annotation = new Annotation();

        if (uri != null) {
            annotation.setUri(uri);
        } else {
            annotation.setUri(bindingSet.getValue(URI).stringValue());
        }
        // created date
        String creationDate = bindingSet.getValue(CREATED).stringValue();
        DateTime stringToDateTime = Dates.stringToDateTimeWithGivenPattern(creationDate, DateFormats.YMDTHMSZ_FORMAT);
        annotation.setCreated(stringToDateTime);

        if (creator != null) {
            annotation.setCreator(creator);
        } else {
            annotation.setCreator(bindingSet.getValue(CREATOR).stringValue());
        }

        annotation.setBodyValue(bindingSet.getValue(BODYVALUE).stringValue());

        if (motivatedBy != null) {
            annotation.setMotivatedBy(motivatedBy);
        } else {
            annotation.setMotivatedBy(bindingSet.getValue(MOTIVATEDBY).stringValue());
        }

        //SILEX:concpetion
        //For now annotation can takes only one target
        //\SILEX
        annotation.addTarget(bindingSet.getValue(TARGET).stringValue());

        return annotation;
    }
}
