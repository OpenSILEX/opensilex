/*
 * *****************************************************************************
 *                         ScientificObjectLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 19/11/2024 16:05
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.scientificObject.bll;


import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.graph.Node;
import org.geojson.GeoJsonObject;
import org.opensilex.core.data.api.CriteriaDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.exception.DuplicateNameListException;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.api.GeometryDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.api.*;
import org.opensilex.core.scientificObject.dal.*;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.sparql.csv.export.CsvExporter;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ExcludableUriList;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ScientificObjectLogic {

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final ScientificObjectDAO dao;
    private final Node defaultGraphNode;
    private final URI defaultGraphURI;

    public static final String NON_UNIQUE_NAME_INTO_GRAPH_ERROR_MSG = "Object name <%s> must be unique onto the graph <%s>. %s has the same name";
    public static final String NON_UNIQUE_NAME_ERROR_MSG = "Object name <%s> must be unique. %s has the same name";
    public static final String DELETE_ERROR_TITLE ="Scientific object can't be deleted";

    /**
     * Name of the parameter used into translate-key about scientific object deletion error.
     * This key is related to message-en.yml and message-fr.yml translation files (located in opensilex-front)
     */
    public static final String DELETE_ERROR_KEY_PARAMETER ="scientific_object";

    //#region CONSTRUCTOR
    public ScientificObjectLogic(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;

        this.dao = new ScientificObjectDAO(sparql);

        try{
            defaultGraphURI = sparql.getDefaultGraphURI(ScientificObjectModel.class);
            defaultGraphNode = sparql.getDefaultGraph(ScientificObjectModel.class);
        }catch (SPARQLException e){
            throw new RuntimeException(e);
        }
    }
    //#endregion

    //#region PUBLIC METHODS
    /**
     * @param contextURI  object graph
     * @param relations   list of relations
     * @param currentUser current user
     * @return the URI of the created object
     * @throws DuplicateNameException if some object with the same name exist into the given graph
     */
    public URI createScientificObject(
            ScientificObjectModel model,
            URI contextURI,
            List<RDFObjectRelationDTO>  relations,
            MoveModel move,
            AccountModel currentUser
    ) throws Exception {
        // Define the graph (global or XP)
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        ExperimentModel experiment;
        boolean globalCopy;
        URI context;

        validateContextAccess(contextURI, currentUser);

        if (Objects.isNull(contextURI)) {
            experiment = null;
            globalCopy = false;
            context = defaultGraphURI;
        } else {
            context = contextURI;
            globalCopy = true;
            experiment = experimentDAO.get(context, currentUser);
            if (Objects.isNull(experiment)) {
                throw new NotFoundURIException("Unknown experiment", context);
            }
        }

        checkFactorLevelsBelongsToExperiment(relations, experiment);
        checkUniqueNameByGraph(context, model.getName(), null, true);
        Node graphNodeContext = SPARQLDeserializers.nodeURI(context);

        // Set the model
        ScientificObjectModel object = initObject(context, model, relations, currentUser);
        object.setPublisher(currentUser.getUri());
        object.setPublicationDate(OffsetDateTime.now());

        return new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {
            // experimental context + no URI set
            if (!SPARQLDeserializers.compareURIs(defaultGraphNode.getURI(), context) && Objects.isNull(model.getCreationDate())) {

                // generate a globally unique URI
                // (by taking account of all OS into global graph, which also includes OS from any xp)
                sparql.generateUniqueURI(defaultGraphNode, object, object, true);
            }

            // if URI is already set, the service will check that URI is unique inside the provided graph
            // if the graph is global : check if OS is unique inside global graph
            // if the graph is an experiment : check if OS is unique inside experiment graph

            // if the graph is an experiment and the OS already exist into global graph -> OK, since here we consider
            // that we reuse this OS inside the experiment, so no need to performs additional checking
            dao.create(graphNodeContext, object);

            URI soURI = object.getUri();
            //create a move
           if(Objects.nonNull(move)){
                MoveLogic moveLogic = new MoveLogic(sparql,nosql,currentUser);
                move.setTargets(Collections.singletonList(soURI));
                move.setIsInstant(Objects.isNull(move.getStart()));
                moveLogic.createNoTransaction(move, session);
            }

            //If OS in experiment --> update species of XP
            if (Objects.nonNull(experiment)) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(context);
            }

            //Copy OS in global graph
            Node graphNodeDefault = SPARQLDeserializers.nodeURI(defaultGraphURI);
            if (globalCopy && !sparql.uriExists(graphNodeDefault, soURI)) {
                copyIntoGlobalGraph(Stream.of(object));
            }

            return soURI;
        });
    }

    public void create(List<ScientificObjectModel> models, URI contextURI) throws Exception {
        Objects.requireNonNull(contextURI);

        boolean useDefaultGraph = SPARQLDeserializers.compareURIs(defaultGraphNode.getURI(),contextURI);
        Node graphNode = useDefaultGraph ? defaultGraphNode : SPARQLDeserializers.nodeURI(contextURI);

        dao.create(graphNode,models);
    }

    public ScientificObjectModel getObjectByURI(URI objectURI, URI contextURI, AccountModel currentUser) throws Exception {
        validateContextAccess(contextURI, currentUser);

        if (Objects.isNull(contextURI)) {
            contextURI = defaultGraphURI;
        }

        return dao.getObjectByURI(objectURI, contextURI, currentUser.getLanguage());
    }

    public Map<ScientificObjectModel, LocationObservationModel> searchByURIs(URI contextURI, List<URI> objectsURI, AccountModel currentUser) throws Exception {

        if (Objects.isNull(objectsURI)) {
            objectsURI = new ArrayList<>();
        }

        validateContextAccess(contextURI, currentUser);

        if (Objects.isNull(contextURI)) {
            contextURI = defaultGraphURI;
        }

        List<ScientificObjectModel> soList = searchByURIs(contextURI, objectsURI, currentUser,false);

        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());

        return locationObservationLogic.generateModelObservationCollectionMap(
                soList,
                (ScientificObjectModel model) -> (model.getLocationObservationCollection() == null ? null : model.getLocationObservationCollection().getUri()),
                Instant.now(),
                null,
                null
        );
    }

    public List<ScientificObjectModel> searchByURIs(URI contextURI, List<URI> objectsURI, AccountModel currentUser, boolean loadChildren) throws Exception {

        Set<String> fieldsToFetch = new HashSet<>();
        fieldsToFetch.add(ScientificObjectModel.FACTOR_LEVEL_FIELD);

        if(loadChildren){
            fieldsToFetch.add(SPARQLTreeModel.CHILDREN_FIELD);
        }

        return dao.searchByURIs(
                contextURI,
                objectsURI,
                currentUser,
                loadChildren,
                fieldsToFetch
        );
    }

    public Map<ScientificObjectModel, ExperimentModel> getScientificObjectDetailByExperiments(URI objectURI, AccountModel currentUser) throws Exception {
        URI globalScientificObjectGraph = new URI(SPARQLDeserializers.getShortURI(defaultGraphURI));
        Map<ScientificObjectModel,ExperimentModel> modelsMap = new HashMap<>();

        List<URI> contexts = dao.getObjectContexts(objectURI);

        for (URI contextURI : contexts) {
            ExperimentModel experiment;
            //assign the global uri "dev:set/scientific-object" when the OS is not linked to an experiment
            if(contextURI.equals(globalScientificObjectGraph)){
                experiment = new ExperimentModel();
                experiment.setUri(globalScientificObjectGraph);
            }
            else{
                experiment = getExperiment(contextURI, currentUser);
            }

            ScientificObjectModel model = dao.getObjectByURI(objectURI, contextURI, currentUser.getLanguage());
            if(Objects.nonNull(model)){
                modelsMap.put(model,experiment);
            }
        }

        return modelsMap;
    }

    public List<SPARQLNamedResourceModel<ScientificObjectModel>> getUsedTypes(URI experimentURI, AccountModel currentUser) throws Exception {
        validateContextAccess(experimentURI, currentUser);
        Set<URI> graphFilterURIs = new HashSet<>();

        if(!currentUser.isAdmin()){
            ExperimentDAO xpDO = new ExperimentDAO(sparql, nosql);
            graphFilterURIs = xpDO.getUserExperiments(currentUser);

            if (CollectionUtils.isEmpty(graphFilterURIs)) {
                return Collections.emptyList();
            }
        }

        return dao.getTypes(experimentURI,graphFilterURIs, currentUser);
    }

    public ListWithPagination<ScientificObjectNodeWithChildrenDTO> searchChildren(ScientificObjectSearchFilter searchFilter, AccountModel currentUser) throws Exception {
        validateContextAccess(searchFilter.getExperiment(), currentUser);

        if (Objects.isNull(searchFilter.getExperiment())) {
            searchFilter.setExperiment(defaultGraphURI);
        }

        if(
                !CollectionUtils.isEmpty(searchFilter.getRdfTypes())
                || !CollectionUtils.isEmpty(searchFilter.getFactorLevels())
                || (searchFilter.getPattern()!=null && !searchFilter.getPattern().isEmpty() && !searchFilter.getPattern().equals(".*") )
                || searchFilter.getFacility() != null
        ) {
            searchFilter.setOnlyFetchOsWithNoParent(false);
        } else {
            searchFilter.setOnlyFetchOsWithNoParent(true);
        }

        ListWithPagination<ScientificObjectNodeDTO> results = dao.searchAsDto(searchFilter);

        if(CollectionUtils.isEmpty(results.getList())){
            return new ListWithPagination<>(Collections.emptyList());
        }

        List<URI> resultsUri = new ArrayList<>();
        results.getList().forEach(result ->
                resultsUri.add(result.getUri())
        );

        Map<String, Integer> childCountByParent = dao.searchChildren(searchFilter, resultsUri);

        return results.convert(ScientificObjectNodeWithChildrenDTO.class, nodeDTO -> {
            ScientificObjectNodeWithChildrenDTO dto = new ScientificObjectNodeWithChildrenDTO();
            dto.setUri(nodeDTO.getUri());
            dto.setName(nodeDTO.getName());
            dto.setType(nodeDTO.getType());
            dto.setTypeLabel(nodeDTO.getTypeLabel());
            dto.setCreationDate(nodeDTO.getCreationDate());
            dto.setDestructionDate(nodeDTO.getDestructionDate());

            Integer childCount = childCountByParent.get(nodeDTO.getUri().toString());

            if (Objects.isNull(childCount)) {
                childCount = 0;
            }
            dto.setChildCount(childCount);

            return dto;
        });
    }

    /**
     *
     * @param objectName name
     * @param objectGraph graph
     * @return a ScientificObjectModel which have the given name and which is stored into objectGraph, return null if no object is found
     * @throws Exception if some Exception is encountered during SPARQL query execution
     * @throws DuplicateNameException if multiple object are found with objectName as name into objectGraph
     */
    public ScientificObjectModel getByNameAndContext(String objectName, URI objectGraph) throws Exception, DuplicateNameException {
        return dao.getByNameAndContext(objectName, objectGraph);
    }

    public ListWithPagination<ScientificObjectNodeDTO> searchScientificObjects(ScientificObjectSearchFilter searchFilter, CriteriaDTO criteriaDTO, List<URI> variables, List<URI> devices, AccountModel currentUser) throws Exception {
        ListWithPagination<ScientificObjectNodeDTO> emptyResult = new ListWithPagination<>(Collections.emptyList(), searchFilter.getPage(), searchFilter.getPageSize(), 0);

        if (Objects.nonNull(searchFilter.getExperiment())) {
            if (sparql.uriExists(ExperimentModel.class, searchFilter.getExperiment())) {
                ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
                xpDAO.validateExperimentAccess(searchFilter.getExperiment(), currentUser);
            } else {
                throw new NotFoundURIException("Experiment URI not found:", searchFilter.getExperiment());
            }
        }

        //Get all object uris that has at least one data validating each all the criteria
        //This is a boolean to not bother applying other filters if criteria search returned 0 results
        boolean applyNonCriteriaFilters = true;
        if(Objects.nonNull(criteriaDTO) && !CollectionUtils.isEmpty(criteriaDTO.getCriteriaList())){

            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
            ExcludableUriList criteriaFilteredObjects = dataLogic.getScientificObjectsThatMatchDataCriteria(criteriaDTO, searchFilter.getExperiment());
            if(Objects.nonNull(criteriaFilteredObjects)){
                if(criteriaFilteredObjects.excludeResults){
                    searchFilter.setExcludedUris(criteriaFilteredObjects.result);
                }else{
                    searchFilter.setUris(criteriaFilteredObjects.result);
                    applyNonCriteriaFilters = !criteriaFilteredObjects.result.isEmpty();
                }
            }
        }

        if(!applyNonCriteriaFilters){
            return emptyResult;
        }else{
            if (CollectionUtils.isNotEmpty(variables) || CollectionUtils.isNotEmpty(devices)) {
                DataLogic dataLogic = new DataLogic(sparql, nosql, fs, currentUser);
                var targets = dataLogic.getUsedTargets(devices, variables, null, URI.create(Oeso.ScientificObject.getURI()));

                if (CollectionUtils.isEmpty(targets)) {
                    return emptyResult;
                }
                searchFilter.intersectionOnUris(targets);
            }
        }

        return dao.searchAsDto(searchFilter);
    }

    /**
     *
     * @param contextURI the experiment
     * @param startDate start date filter
     * @param endDate end date filter
     * @param currentUser user
     * @return Get the last location of each scientific object in an experiment
     * @throws Exception
     */
    public Map<ScientificObjectModel, LocationObservationModel> getSOWithPosition(URI contextURI, String startDate, String endDate, AccountModel currentUser) throws Exception {
        validateContextAccess(contextURI, currentUser);
        Map<ScientificObjectModel, LocationObservationModel> soAndLocationsMap = new HashMap<>();
        List<ScientificObjectModel> soList = dao.getScientificObjectsByDate(contextURI, startDate, endDate, currentUser.getLanguage());

        if(!CollectionUtils.isEmpty(soList)) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());

            //Parse endDate or try to parse experiment's endDate if it was null, use current date if both of those things were null
            Instant end;
            if(endDate == null){
                ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
                ExperimentModel experiment = experimentDAO.get(contextURI, currentUser);
                end = experiment.getEndDate() != null ? experiment.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.now();
            } else {
                end = Instant.parse(endDate);
            }

            soAndLocationsMap = locationObservationLogic.generateModelObservationCollectionMap(
                    soList,
                    (ScientificObjectModel model) -> (model.getLocationObservationCollection() == null ? null : model.getLocationObservationCollection().getUri()),
                    end,
                    null,
                    null
            );
        }
        return soAndLocationsMap;
    }

    public LocationObservationModel getLastLocation(ScientificObjectModel model) {
        //Get last location
        LocationObservationModel soLastLocation = new LocationObservationModel();

        if(Objects.nonNull(model.getLocationObservationCollection())){
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());
            List<LocationObservationModel> locationList = locationObservationLogic.getLastLocationObservation(Collections.singletonList(model.getLocationObservationCollection().getUri()),false, Instant.now(),null);
            if(!CollectionUtils.isEmpty(locationList)){
                soLastLocation = locationList.get(0);
            }
        }
        return soLastLocation;
    }

    public Map<URI, LocationObservationModel> getLastLocationByExperiment(Map<ScientificObjectModel, ExperimentModel> soXpMap) {
        //for each XP get last location of the SO with XP date
        Map<URI, LocationObservationModel> xpLocationMap = new HashMap<>();
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());

        List<LocationObservationCollectionModel> collectionModelList = soXpMap.keySet().stream()
                .map(ScientificObjectModel::getLocationObservationCollection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(collectionModelList)) {
            soXpMap.forEach((so,xp) ->{
                List<LocationObservationModel> lastLocation = locationObservationLogic.getLastLocationObservation(
                        Collections.singletonList(so.getLocationObservationCollection().getUri()),
                        false,
                        Objects.nonNull(xp.getEndDate()) ? xp.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now(),
                        null);
                if(!CollectionUtils.isEmpty(lastLocation)){
                    xpLocationMap.put(xp.getUri(), lastLocation.get(0));
                }
            });
        }
        return xpLocationMap;
    }

    /**
     *
     * @param contextURI object graph
     * @param model scientific object
     * @param relations list of relations
     * @param currentUser  current user
     * @return the URI of the created object
     * @throws DuplicateNameException if some object with the same name exist into the given graph
     */
    public URI updateScientificObject(
            ScientificObjectModel model,
            URI contextURI,
            List<RDFObjectRelationDTO> relations,
            UserGetDTO publisher,
            OffsetDateTime publicationDate,
            AccountModel currentUser
    ) throws Exception {
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        boolean hasExperiment = contextURI != null;
        URI context;

        validateContextAccess(contextURI, currentUser);

        // Define the graph (global or XP)
        if (Objects.isNull(contextURI)) {
            context = defaultGraphURI;
        } else {
            context = contextURI;
        }

        checkUniqueNameByGraph(context, model.getName(), model.getUri(), false);
        Node graphNode = SPARQLDeserializers.nodeURI(context);

        // Set the model
        SPARQLResourceModel object = initObject(context, model, relations, currentUser);
        if (Objects.nonNull(publisher) && Objects.nonNull(publisher.getUri())) {
            object.setPublisher(publisher.getUri());
        }
        if (Objects.nonNull(publicationDate)) {
            object.setPublicationDate(publicationDate);
        }
        object.setLastUpdateDate(OffsetDateTime.now());

        return new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {
            dao.update(graphNode, object, currentUser.getLanguage());

            if (hasExperiment) {
                experimentDAO.updateExperimentSpeciesFromScientificObjects(context);
                ExperimentModel experimentModel = experimentDAO.get(context, currentUser);
                checkFactorLevelsBelongsToExperiment(relations, experimentModel);
            }
            return object.getUri();
        });
    }

    /**
     * Updates a list of scientific objects, handles updating of associated move
     *
     * @param models to update
     * @param currentUser user performing the update
     * @param context to update in (an experiment or global)
     * @throws Exception
     */
    public void updateMultipleSOAndMoves(List<ScientificObjectModel> models, AccountModel currentUser, Node context, boolean isGlobalContext) throws Exception{
        new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {

            //Fetch children so we can replace them
            Map<URI, List<URI>> oldChildrenPerOSUri = new HashMap<>();

            //Do a loop on models to update moves and to fetch children if we are in an experiment
            //TODO optimization? is it possible to not update the moves one by one?
            for (ScientificObjectModel model : models) {
                //Only fetch children if we are in an experiment
                if(!isGlobalContext){
                    List<URI> childrenURIs = this.dao.fetchChildrenURIs(model.getUri(), context, currentUser.getLanguage());
                    if (!childrenURIs.isEmpty()) {
                        oldChildrenPerOSUri.put(model.getUri(), childrenURIs);
                    }
                }
                //Always update move
                //TODO MAX the old updateMove method used old target positions and things, i may have to do something different just comment out for now
                //updateMove(model, currentUser, context, session);
            }

            //Perform the actual update of OS's
            sparql.update(models, context);

            //Replace the children
            for(Map.Entry<URI, List<URI>> entry : oldChildrenPerOSUri.entrySet()){
                sparql.insertPrimitive(context, entry.getValue(), Oeso.isPartOf, entry.getKey());
            }
            return 0;
        });
    }

    /**
     * Remove an OS from a graph
     * @param contextURI URI of the experiment in which the object is located. If null then the objectURI is deleted from the global os graph
     * @param objectURI URI of the object to delete
     * @throws Exception if some error is encountered during suppression of the object from the triplestore
     * @throws IllegalArgumentException if objectURI is null
     */
    public void deleteScientificObject(URI contextURI, URI objectURI, AccountModel currentUser) throws Exception {
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        DataDAO dataDAO = new DataDAO(nosql,sparql,null);

        validateContextAccess(contextURI, currentUser);
        Objects.requireNonNull(objectURI);

        new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {
            List<URI> xpList;
            List<URI> osList = Collections.singletonList(objectURI);
            boolean global = contextURI == null;

            if (global) {
                // global OS suppression -> ensure that the OS is not used into any experiment
                if (dao.isInvolvedIntoAnyExperiment(osList.stream(), osList.size())) {
                    throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object is used into an experiment",
                            "component.scientificObjects.error.delete.used-in-experiment",
                            Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER,objectURI.toString())
                    );
                }
                // set empty list to pass to DataDao count and countFiles
                xpList = Collections.emptyList();
            }else{
                // check that the OS has no children
                if (dao.hasChildren(contextURI, osList.stream(), osList.size())) {
                    throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has child",
                            "component.scientificObjects.error.delete.has-child",
                            Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER,objectURI.toString())
                    );
                }
                // set list composed of the experiment, to pass to DataDao count and countFiles
                xpList = Collections.singletonList(contextURI);
            }

            // check that no data are associated (dao handle empty or not list)
            int dataCount = dataDAO.count(null,xpList, osList,null,null,null,null,null,null,null,null, null);
            if(dataCount > 0){
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has associated data",
                        "component.scientificObjects.error.delete.associated-data",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }

            // check that no data file are associated (dao handle empty or not list)
            int dataFileCount = dataDAO.countFiles(null,null,xpList,osList,null,null,null,null,null, null);
            if(dataFileCount > 0){
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has associated data files",
                        "component.scientificObjects.error.delete.associated-data-files",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }

            //check that no moves are associated
            MoveLogic moveLogic = new MoveLogic(sparql,nosql,currentUser);
            int moveCount = moveLogic.countForTarget(objectURI);
            if(moveCount > 0){
                throw new DisplayableBadRequestException(DELETE_ERROR_TITLE+" : object has associated moves",
                        "component.scientificObjects.error.delete.associated-moves",
                        Collections.singletonMap(DELETE_ERROR_KEY_PARAMETER, objectURI.toString())
                );
            }

            // delete OS and OS geometry (dao handle null or not null contextURI)
            dao.delete(contextURI,objectURI);

            if(!global){
                experimentDAO.updateExperimentSpeciesFromScientificObjects(contextURI);
            }

            return null;
        });
    }

    public  Map<String, byte[]> exportFromMapScientificObjects(List<GeometryDTO> selectedObjects, List<URI> selectedProps, URI contextURI, String format, AccountModel currentUser) throws Exception {
        Map<URI, GeoJsonObject> selectedObjectsMap = new HashMap<>();

        //Get OS exported URI
        selectedObjects.forEach(o ->
            selectedObjectsMap.put(URI.create(SPARQLDeserializers.getExpandedURI(o.getUri())), o.getGeometry())
        );

        // Search exported OS detail according the XP and selected uris, fetch os factors
        ScientificObjectSearchFilter searchFilter = new ScientificObjectSearchFilter()
                .setExperiment(contextURI)
                .setUris( new ArrayList<>(selectedObjectsMap.keySet()));

        searchFilter.setLang(currentUser.getLanguage())
                .setPageSize(10000)
                .setPage(0);

        List<ScientificObjectModel> objDetailList = dao.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD)).getList();

        //Convert
        ScientificObjectGeospatialExporter shpExport = new ScientificObjectGeospatialExporter();
        return shpExport.exportFormat(selectedProps, objDetailList, selectedObjectsMap,format);
    }

    public byte[] exportScientificObjects(ScientificObjectExportDTO searchFilter, AccountModel currentUser) throws Exception {
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);

        validateContextAccess(searchFilter.getExperiment(), currentUser);

        searchFilter.setLang(currentUser.getLanguage());

        // search objects according filtering and selected uris, fetch os factors
        ListWithPagination<ScientificObjectModel> objects = dao.search(searchFilter, Collections.singletonList(ScientificObjectModel.FACTOR_LEVEL_FIELD));

        // compute URI list in order to call getGeometryByUris()
        List<URI> objectsUris;
        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            objectsUris = searchFilter.getUris();
        } else {
            objectsUris = objects.getList().stream().map(ScientificObjectModel::getUri).collect(Collectors.toList());
        }

        // get geometry of objects
        HashMap<String, Geometry> geospatialMap = geoDAO.getGeometryByUris(searchFilter.getExperiment(), objectsUris);

        // get last location of objects
        Map<URI, URI> arrivalFacilityByOs = moveLogic.getLastLocations(objectsUris.stream(), objects.getList().size());

        CsvExporter<ScientificObjectModel> csvExporter = new ScientificObjectCsvExporter(
                sparql,
                objects.getList(),
                searchFilter.getExperiment(),
                currentUser.getLanguage(),
                arrivalFacilityByOs,
                geospatialMap
        );

        return csvExporter.exportCSV();
    }

    public ListWithPagination<ScientificObjectModel> search(ScientificObjectSearchFilter searchFilter, Collection<String> fieldsToFetch) throws Exception {
        return dao.search(searchFilter, fieldsToFetch);
    }

    public SPARQLNamedResourceModel<ScientificObjectModel> getUriByNameAndGraph(Node experiment, String objectName) throws SPARQLException {
        return dao.getUriByNameAndGraph(experiment,objectName);
    }

    public List<URI> getScientificObjectUrisAssociatedWithGermplasms(List<URI> experiments, URI germplasmGroupUri, List<URI> passedGermplasms) throws Exception {
        return dao.getScientificObjectUrisAssociatedWithGermplasms(experiments,germplasmGroupUri,passedGermplasms);
    }

    /**
     * Check if an object with the same name already exists into objectGraph graph.
     * @param name name
     * @param graph graph
     * @throws SPARQLException if some Exception is encountered during SPARQL query execution
     * @throws DuplicateNameException if an object with the same name already exists into objectGraph graph.
     */
    public void checkUniqueNameByGraph(URI graph, String name, URI uri, boolean create) throws DuplicateNameException, SPARQLException {
        Objects.requireNonNull(graph);

        // unique name restriction only apply on some experiment graph
        if(SPARQLDeserializers.compareURIs(graph, defaultGraphURI)){
            return;
        }

        SPARQLNamedResourceModel alreadyExistingOs = getUriByNameAndGraph(SPARQLDeserializers.nodeURI(graph), name);
        if(alreadyExistingOs == null){
            return;
        }

        // error on create if -> an already existing os has the same name
        // error on update if -> an already existing os different from <objectUri> has the same name
        if (create || !SPARQLDeserializers.compareURIs(alreadyExistingOs.getUri(), uri)) {
            String errorMsg = String.format(NON_UNIQUE_NAME_INTO_GRAPH_ERROR_MSG, name, graph, alreadyExistingOs);
            throw new DuplicateNameException(errorMsg,name);
        }
    }

    /**
     * Check into objectGraph if it exists any object with a name corresponding with a name from objects, throw {@link DuplicateNameListException} if so
     *
     * @param objects objects to check (need to have a non-null {@link SPARQLNamedResourceModel#getName()}
     * @param objectGraph graph into checking of duplicate name is done
     *
     * @throws DuplicateNameListException if at least one object from objects use a {@link SPARQLNamedResourceModel#getName()} already used by another object into objectGraph.
     * The exception has the {@link DuplicateNameListException#getExistingUriByName()} method which return association between existing name and uri.
     * @throws SPARQLException If some error is encountered during SPARQL query execution
     * @throws IllegalArgumentException if objects is null or empty or if objectGraph is null
     *
     * @apiNote This method performs a SPARQL request with a VALUES clause on each object name
     * A large collection of object could lead to a too big SPARQL query, which may be un-parsable or too slow.
     * Try to split your object' names verification, into multiple call to this method, if you have too much object to handle.
     *
     * The produced query looks like : <br>
     *
     * <pre>
     * {@code
     *
     * SELECT  ?uri ?name
     * WHERE
     * {
     *     ?rdfType  rdfs:subClassOf*  vocabulary:ScientificObject
     *     GRAPH <http://opensilex.dev/id/experiment/experiment1> {
     *           ?uri  a           ?rdfType ;
     *                 rdfs:label  ?name
     *     }
     *     VALUES ?name { "name_1" "name_2"  "name_k"}
     * }
     * }</pre>
     */
    public void checkUniqueNameByGraph(List<ScientificObjectModel> objects, URI objectGraph) throws DuplicateNameListException, SPARQLException {
        Objects.requireNonNull(objectGraph);

        // unique name restriction only apply on some experiment graph
        if(SPARQLDeserializers.compareURIs(objectGraph, defaultGraphURI)){
            return;
        }

        if(CollectionUtils.isEmpty(objects)){
            throw new IllegalArgumentException("objects is null or empty");
        }

        checkLocalDuplicates(objects);

        Map<String,URI> existingUriByName = dao.checkUniqueNameByGraph(objects, objectGraph);

        if(existingUriByName!=null && !existingUriByName.isEmpty()){
            throw new DuplicateNameListException(existingUriByName);
        }
    }

    public static boolean fillFacilityMoveEvent(MoveModel facilityMoveEvent, SPARQLResourceModel object) throws Exception {
        List<URI> targets = new ArrayList<>();
        targets.add(object.getUri());
        facilityMoveEvent.setTargets(targets);

        facilityMoveEvent.setIsInstant(true);

        boolean hasFacility = false;
        for (SPARQLModelRelation relation : object.getRelations()) {
            if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.isHosted.getURI())) {
                FacilityModel infraModel = new FacilityModel();
                infraModel.setUri(new URI(relation.getValue()));
                facilityMoveEvent.getLocationObservation().getLocation().setTo(infraModel.getUri());
                hasFacility = true;
            } else if (SPARQLDeserializers.compareURIs(relation.getProperty().getURI(), Oeso.hasCreationDate.getURI())) {
                InstantModel end = new InstantModel();
                SPARQLDeserializer<LocalDate> dateDeserializer = SPARQLDeserializers.getForClass(LocalDate.class);
                LocalDate date = dateDeserializer.fromString(relation.getValue());
                end.setDateTimeStamp(OffsetDateTime.of(date, LocalTime.MIN, ZoneOffset.UTC));
                facilityMoveEvent.setEnd(end);
            }
        }

        InstantModel end = facilityMoveEvent.getEnd();
        if (end != null) {
            if (end.getDateTimeStamp() == null) {
                end.setDateTimeStamp(OffsetDateTime.now());
            }
        } else if (hasFacility) {
            end = new InstantModel();
            end.setDateTimeStamp(OffsetDateTime.now());
            facilityMoveEvent.setEnd(end);
        }

        return hasFacility;
    }

    public void copyIntoGlobalGraph(Stream<ScientificObjectModel> models) throws SPARQLException {
        Objects.requireNonNull(models);

        dao.copyIntoGlobalGraph(models);
    }

    public Node getDefaultGraphNode() {
        return defaultGraphNode;
    }

    public int getCount(ScientificObjectSearchFilter searchFilter) throws Exception {
        return dao.getCount(searchFilter);
    }
    //#endregion

    //#region PRIVATE METHODS
    private void validateContextAccess(URI contextURI, AccountModel currentUser) throws Exception {
        if (Objects.isNull(contextURI)) {
            // INFO :  no need to validate with no context defined
        } else if (sparql.uriExists(ExperimentModel.class, contextURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);

            xpDAO.validateExperimentAccess(contextURI, currentUser);
        } else {
            throw new NotFoundURIException("Experiment URI not found:", contextURI);
        }
    }

    /**
     * Check that new factor levels we want to add to the OS are associated to the experiment. Throw an exception if not.
     * @param relations to look for the hasFactorLevel relation
     * @param experiment Experiment model that is (or will be) linked to the OS.
     * @throws InvalidValueException if a factor level is not part of the experiment.
     */
    private static void checkFactorLevelsBelongsToExperiment(List<RDFObjectRelationDTO>  relations, ExperimentModel experiment) throws InvalidValueException {
        if (relations == null || relations.isEmpty()) {
            return;
        }
        if (experiment == null){
            throw new InvalidValueException("An OS without experiment can't have factor levels");
        }

        List<URI> experimentFactorLevels = experiment.getFactors().stream()
                .flatMap(factor -> factor.getFactorLevels().stream().map(FactorLevelModel::getUri))
                .toList();
        List<URI> descriptionFactorLevels = relations.stream()
                .filter( relation -> SPARQLDeserializers.compareURIs(relation.getProperty(), Oeso.hasFactorLevel.getURI()))
                .map(relation -> {
                    try {
                        return new URI(relation.getValue());
                    } catch (URISyntaxException e) {
                        throw new InvalidValueException("Invalid factor level URI"+ relation.getValue());
                    }
                }).toList();
        descriptionFactorLevels.forEach(factorLevel -> {
            if (!experimentFactorLevels.contains(factorLevel)) {
                throw new InvalidValueException("Following factor level is not part of the experiment: "+factorLevel);
            }
        });
    }

    private ScientificObjectModel initObject(URI contextURI,ScientificObjectModel object, List<RDFObjectRelationDTO> relations, AccountModel currentUser) throws Exception {
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(object.getType(), new URI(Oeso.ScientificObject.getURI()), currentUser.getLanguage());

        if(!CollectionUtils.isEmpty(relations)){
            RDFObjectDTO.validatePropertiesAndAddToObject(contextURI, model, object, relations, ontologyDAO);
        }

        return object;
    }

    private ExperimentModel getExperiment(URI experimentURI, AccountModel currentUser) throws Exception {
        if (sparql.uriExists(ExperimentModel.class, experimentURI)) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);

            try {
                return xpDAO.get(experimentURI, currentUser);
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    private void checkLocalDuplicates(List<ScientificObjectModel> models) throws DuplicateNameListException{
        Set<String> uniquesNames = new HashSet<>();

        Map<String,URI> localDuplicatesByUri = new HashMap<>();
        models.forEach(model -> {
            // if name already exist, then register it as a duplicate name
            if(! uniquesNames.add(model.getName())){
                localDuplicatesByUri.put(model.getName(),model.getUri());
            }
        });

        if(!CollectionUtils.isEmpty(localDuplicatesByUri.keySet())){
            throw new DuplicateNameListException(localDuplicatesByUri);
        }
    }
    //#endregion
}
