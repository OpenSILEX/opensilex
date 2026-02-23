/*
 * *****************************************************************************
 *                         UpdateSOWithLocationObservationCollectionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/11/2024 17:46
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.migration.one_point_five_ALL;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.bson.Document;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.*;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel {

    private SPARQLService sparql;
    private MongoDBService mongodb;
    private final Logger logger;

    private static final String OLD_MOVE_COLLECTION = "move";
    //Because the old geospatial position attribute of ScientificObjects had no date associated to it, we take some random one
    private static final String defaultDateISOString = "2017-05-19T00:00:00Z";

    public static String DESCRIPTION = "Update ScientificObjects and Devices to use the new Location system. Do this by reading old Moves (includes the isHosted property for ScientificObjects as a move was created for each isHosted) and geospatial mongo collection.";

    public UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel(SPARQLService sparql, MongoDBService mongodb, Logger logger) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.logger = logger;
    }

    public void execute() throws Exception {
        try {
            //1 - For every existing ScientificObject  URI fetch all old GeospatialModels, create new LocationObservationModels,
            //placed in a Map of Format URI -> List(LocationObservationModels)
            StringUriMap<List<LocationObservationModel>> locationObservationsPerURIFromGeospatial = makeNewLocationObservationsFromGeospatialModels();
            //2 - Create a move in RDF4J for each location from geospatial Collection
            List<MoveModel> newMoves = sparqlCreateSoMoves(locationObservationsPerURIFromGeospatial);

            //3 - Now Fetch all old Moves for every ScientificObject but also other stuff, normally it should just be Devices
            Stream<SPARQLResult> moveDetailsList = sparqlGetMoveDetails();
            //4 - Make new Location Observations for old existing Moves, combine them with locationObservationsPerURIFromGeospatial and return all in a new Map
            StringUriMap<List<LocationObservationModel>> locationObservationsPerURI = makeLocationObservationsFromExistingMoves(locationObservationsPerURIFromGeospatial, moveDetailsList);
            //5 - Add collections for each OS that has at least one LocationObservation. Return in a Map of format OS URI -> ObservationCOLLECTION URI
            StringUriMap<URI> soCollectionMap = sparqlAddLocationCollection(locationObservationsPerURI);
            //6 - Complete location observation models for each SO and insert to Location collection
            setObservationCollectionAndMoveUrisAndInsertLocationObservations(locationObservationsPerURI, soCollectionMap, newMoves);

            //7 - Deletion of old stuff:
            // Mongo move collection, Oeev:to and Oeev:from in sparql,
            // and documents from Geospatial collection that have for rdfType any type that is subclass of ScientificObject
            deleteStuff();

        } catch (Exception e) {
            logger.warn("Something went wrong in the UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel part of the migration!");
            throw e;
        }
    }

    /**
     * Deletion of old stuff:
     *  Mongo move collection, Oeev:to and Oeev:from on moves
     * and documents from Geospatial collection that have for rdfType any type that is subclass of ScientificObject
     * (at the time of writing this we can not simply delete entire geospatial collection as Area's still use it)
     */
    private void deleteStuff() throws Exception{
        //From and too in rdf (as they now go in the mongo Location collection)
        deleteTooAndFrom();
        //Delete the old mongo moves collection
        MongoDatabase db = mongodb.getDatabase();
        MongoCollection<?> collection = db.getCollection(OLD_MOVE_COLLECTION, MoveNosqlModel.class);
        collection.drop();
        //Delete documents from geospatial collection that have for rdfType a subclass of ScientificObject
        List<URI> soRdfType =  getOsSubTypes();
        Document geospatFilter = new Document();
        geospatFilter.append(SPARQLResourceModel.TYPE_FIELD, new Document("$in", soRdfType));
        mongodb.deleteOnCriteria(GeospatialModel.class, GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, geospatFilter);
    }

    private void deleteTooAndFrom() throws SPARQLException {

        Var moveVar = makeVar("move");
        Var fromFacility = makeVar("fromFacility");
        Var tooFacility = makeVar("tooFacility");

        UpdateBuilder deletionBuilder = new UpdateBuilder();
        deletionBuilder.addDelete(moveVar, Oeev.from, fromFacility);
        deletionBuilder.addDelete(moveVar, Oeev.to, tooFacility);
        deletionBuilder.addWhere(moveVar, RDF.type, Oeev.Move);
        deletionBuilder.addOptional(moveVar, Oeev.from, fromFacility);
        deletionBuilder.addOptional(moveVar, Oeev.to, tooFacility);

        sparql.executeUpdateQuery(deletionBuilder);
    }

    /**
     * Checks if this migration was most likely already run. Does this by looking to see if any OS is already a feature of interest of a LocationObservationCollection.
     * @return true if any OS's are feature of interest, false if nay
     */
    protected boolean wasMigrationPreviouslyRun() throws SPARQLException {
        AskBuilder osLocationSelect = new AskBuilder();
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var collectionVar = makeVar("collection");
        Var typeVar = makeVar("type");

        osLocationSelect.addWhere(typeVar, Ontology.subClassAny, Oeso.ScientificObject);
        osLocationSelect.addWhere(uriVar, RDF.type, typeVar);
        osLocationSelect.addWhere(collectionVar, RDF.type, SOSA.ObservationCollection);
        osLocationSelect.addWhere(collectionVar, SOSA.hasFeatureOfInterest, uriVar);

        boolean doHaveOSLocations = sparql.executeAskQuery(osLocationSelect);
        //If we have OS locations, no need to check for devices, we can leave
        if(doHaveOSLocations){
            return true;
        }

        //Else do same check for devices
        AskBuilder deviceLocationSelect = new AskBuilder();

        deviceLocationSelect.addWhere(typeVar, Ontology.subClassAny, Oeso.Device);
        deviceLocationSelect.addWhere(uriVar, RDF.type, typeVar);
        deviceLocationSelect.addWhere(collectionVar, RDF.type, SOSA.ObservationCollection);
        deviceLocationSelect.addWhere(collectionVar, SOSA.hasFeatureOfInterest, uriVar);

        return  sparql.executeAskQuery(deviceLocationSelect);
    }

    private List<URI> getOsSubTypes() throws SPARQLException {
        SelectBuilder select = new SelectBuilder().addWhere(new TriplePath(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, Oeso.ScientificObject.asNode()));
        return sparql.executeSelectQueryAsStream(select)
                .map(sparqlResult -> URI.create(SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(ScientificObjectModel.TYPE_FIELD))))
                .collect(Collectors.toList());
    }

    /**
     *
     * @return a Map of format ScientificObjectURI -> List(LocationObservationModel), corresponding to the list of found
     * locations (in correct new format) for every existing ScientificObject.
     *
     */
    private StringUriMap<List<LocationObservationModel>> makeNewLocationObservationsFromGeospatialModels() throws SPARQLException {
        //Get OS subClasses
        List<URI> soRdfType =  getOsSubTypes();

        MongoDatabase db = mongodb.getDatabase();

        //Get SO from geospatial collection
        MongoCollection<GeospatialModel> geospatialCollection = db.getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
        List<GeospatialModel> soFromGeospatial = geospatialCollection.find(Filters.in(SPARQLResourceModel.TYPE_FIELD, soRdfType)).into(new ArrayList<>());

        StringUriMap<List<LocationObservationModel>> soLocationListMap = new StringUriMap<>();

        // Mapping FeatureOfInterest and locations
        // from geospatial collection
        soFromGeospatial.forEach(geo ->{
            //build location observation
            LocationModel location = new LocationModel();
            location.setGeometry(geo.getGeometry());

            LocationObservationModel locationObservation = new LocationObservationModel();
            locationObservation.setLocation(location);
            locationObservation.setEndDate(Instant.parse(defaultDateISOString));
            locationObservation.setHasGeometry(true);
            locationObservation.setFeatureOfInterest(geo.getUri());

            try {
                if(!SPARQLDeserializers.compareURIs(geo.getGraph(), sparql.getDefaultGraphURI(ScientificObjectModel.class))){
                    locationObservation.setExperimentUri(geo.getGraph());
                }
            } catch (SPARQLException ignore) {
                //There should always be a default OS graph right?
            }

            soLocationListMap.put(geo.getUri(), List.of(locationObservation));
        });

        return soLocationListMap;
    }

    /**
     * <pre>
     *     PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *     PREFIX  org:  <http://www.w3.org/ns/org#>
     *     PREFIX  sosa: <http://www.w3.org/ns/sosa/>
     *     PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     *
     * SELECT *
     * WHERE {
     * ?featureOfInterest a ?rdfType.
     * ?rdfType rdfs:subClassOf* vocabulary:ScientificObject .
     *  GRAPH <http://opensilex.test/set/event> {
     *
     *           ?objects rdf:type oeev:Move.
     *   	    ?objects time:hasEnd ?end.
     *   	    ?end time:inXSDDateTimeStamp ?endDate.
     *   	    ?objects oeev:concerns ?featureOfInterest.
     *   	    OPTIONAL
     *   	        { ?objects time:hasBeginning ?start.
     *   	        ?start time:inXSDDateTimeStamp ?startDate}
     *   	    OPTIONAL
     *   	        { ?objects oeev:to ?facilityTo }
     *   	    OPTIONAL
     *        	   { ?objects oeev:from ?facilityFrom }
     *   	    }
     *       }
     * }
     * </pre>
     * */
    private Stream<SPARQLResult> sparqlGetMoveDetails() throws SPARQLException {
        //Graph
        Node eventGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(EventModel.class));

        //Variables
        Var eventVar = makeVar(EventModel.URI_FIELD);
        Var endVar = makeVar(EventModel.END_FIELD);
        Var endDateVar = makeVar(LocationObservationModel.END_DATE_FIELD);
        Var startVar = makeVar(EventModel.START_FIELD);
        Var startDateVar = makeVar(LocationObservationModel.START_DATE_FIELD);
        Var toVar = makeVar(LocationModel.TO_FIELD);
        Var fromVar = makeVar(LocationModel.FROM_FIELD);
        Var featureVar = makeVar(LocationObservationModel.FEATURE_OF_INTEREST_FIELD);
        //Var soTypeVar = makeVar(ScientificObjectModel.TYPE_FIELD);

        //where SO clause
        /*WhereBuilder whereSO = new WhereBuilder()
                .addWhere(featureVar, RDF.type, soTypeVar)
                .addWhere(new TriplePath(soTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode()));*/

        //where event clause
        //start optional clause
         WhereBuilder whereStart =  new WhereBuilder();
         whereStart.addWhere(eventVar, Time.hasBeginning, startVar).addWhere(startVar, Time.inXSDDateTimeStamp, startDateVar);

        WhereBuilder whereEvent = new WhereBuilder()
                .addWhere(eventVar, RDF.type, Oeev.Move)
                .addWhere(eventVar, Time.hasEnd, endVar)
                .addWhere(endVar, Time.inXSDDateTimeStamp, endDateVar)
                .addWhere(eventVar, Oeev.concerns, featureVar)
                .addOptional(whereStart)
                .addOptional(eventVar, Oeev.to, toVar)
                .addOptional(eventVar, Oeev.from, fromVar);

        WhereBuilder where = new WhereBuilder().addGraph(eventGraph,whereEvent);
        SelectBuilder select = new SelectBuilder()
                //.addWhere(whereSO)
                .addWhere(where)
                .addVar(eventVar)
                .addVar(endVar)
                .addVar(endDateVar)
                .addVar(startVar)
                .addVar(startDateVar)
                .addVar(toVar)
                .addVar(fromVar);
                //.addVar(soTypeVar);

        SPARQLQueryHelper.appendGroupConcatAggregator(select, featureVar, true);
        select.addGroupBy(eventVar);
        select.addGroupBy(endVar);
        select.addGroupBy(endDateVar);
        select.addGroupBy(startVar);
        select.addGroupBy(startDateVar);
        select.addGroupBy(toVar);
        select.addGroupBy(fromVar);
        //select.addGroupBy(soTypeVar);

        return sparql.executeSelectQueryAsStream(select);
    }

    private List<MoveModel> sparqlCreateSoMoves(StringUriMap<List<LocationObservationModel>> soLocationListMap){
        try {
            //build move
            MoveEventDAO moveDao = new MoveEventDAO(sparql, mongodb);

            List<MoveModel> moveModels = new ArrayList<>();

            soLocationListMap.values().stream()
                    .flatMap(Collection::stream)
                    .forEach(location -> {
                        InstantModel defaultEnd = new InstantModel();
                        defaultEnd.setType(org.opensilex.sparql.model.time.Time.InstantURI);
                        defaultEnd.setDateTimeStamp(OffsetDateTime.parse(defaultDateISOString));

                        if (location.getEndDate() != null && location.getUri() == null) {
                            MoveModel moveModel = new MoveModel();
                            moveModel.setEnd(defaultEnd);
                            moveModel.setIsInstant(true);
                            moveModel.setTargets(Collections.singletonList(location.getFeatureOfInterest()));
                            moveModels.add(moveModel);
                        }
                    });

            List<MoveModel> createdMoves = moveDao.create(moveModels);
            return createdMoves;

        } catch (SPARQLException e) {
            throw new RuntimeException(e);
        } catch (SPARQLDeserializerNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Creates new LocationObservationModels by looking that old existing Moves (from the SPARQLResults and by looking
     *  at the corresponding Mongo MoveNosqlModels that we will fetch). Then adds them to locationObservationsPerScientificObjectUri.
     */
    private StringUriMap<List<LocationObservationModel>> makeLocationObservationsFromExistingMoves(
            StringUriMap<List<LocationObservationModel>> locationObservationsPerScientificObjectUri,
            Stream<SPARQLResult> existingMoveDetailsList
    ){
        //Get ScientificObjects from old Move mongo collection (containing MoveNosqlModels)
        MongoDatabase db = mongodb.getDatabase();
        MongoCollection<MoveNosqlModel> moveCollection = db.getCollection(OLD_MOVE_COLLECTION, MoveNosqlModel.class);

        Map<URI,SPARQLResult> sparqlMoveDetailsPerMoveURI = existingMoveDetailsList.collect(
                Collectors.toMap(
                        sparqlResult -> URI.create(sparqlResult.getStringValue(EventModel.URI_FIELD)),
                        sparqlResult -> sparqlResult,
                        (oldValue, newValue) -> oldValue
                )
        );

        List<MoveNosqlModel> soFromMove = moveCollection.find(Filters.in("_id", new ArrayList<>(sparqlMoveDetailsPerMoveURI.keySet()))).into(new ArrayList<>());

        //Make mapping from sparql move URI -> Mongo move
        StringUriMap<MoveNosqlModel> noSqlMovePerMoveURI = new StringUriMap<>();
        soFromMove.forEach(moveNosqlModel -> noSqlMovePerMoveURI.put(moveNosqlModel.getUri(), moveNosqlModel));

        //Iterate over SPARQL moves whiles creating our new LocationObservations (iterating over the noSqlMoves would leave out facility stuff)
        sparqlMoveDetailsPerMoveURI.forEach((moveUri, moveDetailsAsSPARQLResult) -> {
            //Extract features of interest
            List<URI> featuresOfInterest = Arrays.stream(moveDetailsAsSPARQLResult.getStringValue(SPARQLQueryHelper.getConcatVarName(LocationObservationModel.FEATURE_OF_INTEREST_FIELD)).split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR)).map(URI::create).toList();

            //Save the common variables (StartDate and things)
            Instant endDate = Instant.parse(moveDetailsAsSPARQLResult.getStringValue(LocationObservationModel.END_DATE_FIELD));

            //Others can be null so need to null check
            Instant startDate = null;
            URI too= null;
            boolean tooFacilityHasGeometry = false;
            URI from = null;

            if (moveDetailsAsSPARQLResult.getStringValue(LocationObservationModel.START_DATE_FIELD) != null) {
                startDate = Instant.parse(moveDetailsAsSPARQLResult.getStringValue(LocationObservationModel.START_DATE_FIELD));
            }
            if (moveDetailsAsSPARQLResult.getStringValue(LocationModel.TO_FIELD) != null) {
                too = URI.create(moveDetailsAsSPARQLResult.getStringValue(LocationModel.TO_FIELD));
                LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
                //Create a temporary model just to be able to call checkHasGeom
                LocationObservationModel temp =  new LocationObservationModel();
                LocationModel tempLocationModel = new LocationModel();
                tempLocationModel.setTo(too);
                temp.setLocation(tempLocationModel);
                tooFacilityHasGeometry = observationLogic.checkHasGeometry(temp, startDate, endDate);
            }
            if (moveDetailsAsSPARQLResult.getStringValue(LocationModel.FROM_FIELD) != null) {
                from = URI.create(moveDetailsAsSPARQLResult.getStringValue(LocationModel.FROM_FIELD));
            }

            //Create a LocationObservation for each featureOfInterest
            for(URI singleFeature : featuresOfInterest){
                //Initialize LocationObservationModel
                LocationObservationModel locationObservation =  initializeLocationObservationFromSparqlFields(
                        moveUri,
                        startDate,
                        endDate,
                        singleFeature,
                        tooFacilityHasGeometry,
                        from,
                        too
                );

                //Now that we've sorted the common stuff from sparql we can look at Mongo
                MoveNosqlModel noSqlMove = noSqlMovePerMoveURI.get(moveUri);

                //If noSqlMove not null then we can add some stuff to the Location
                if(noSqlMove!=null){
                    TargetPositionModel correctTargetPos = noSqlMove.getTargetPositions().stream().filter(targetPositionModel -> SPARQLDeserializers.compareURIs(targetPositionModel.getTarget(), singleFeature)).findFirst().orElse(null);
                    if(correctTargetPos != null){
                        PositionModel positionModel = correctTargetPos.getPosition();
                        if(positionModel != null) {
                            LocationModel location = locationObservation.getLocation();
                            if(positionModel.getCoordinates() != null) {
                                location.setGeometry(positionModel.getCoordinates());
                                locationObservation.setHasGeometry(true);
                            }
                            if(positionModel.getTextualPosition() != null) {
                                location.setTextualPosition(positionModel.getTextualPosition());
                            }
                            if(positionModel.getX() != null) {
                                location.setX(positionModel.getX());
                            }
                            if(positionModel.getY() != null) {
                                location.setY(positionModel.getY());
                            }
                            if(positionModel.getZ() != null) {
                                location.setZ(positionModel.getZ());
                            }
                        }
                    }
                }

                //if a location from the geospatial collection already exists, add it to the existing list, otherwise create a new entry
                List<LocationObservationModel> list = locationObservationsPerScientificObjectUri.get(singleFeature);
                if(Objects.isNull(list)) {
                    locationObservationsPerScientificObjectUri.put(singleFeature, List.of(locationObservation));
                }else{
                    List<LocationObservationModel> mutableList = new ArrayList<>(list);
                    mutableList.add(locationObservation);

                    locationObservationsPerScientificObjectUri.put(singleFeature, mutableList);
                }
            }

        });

        return locationObservationsPerScientificObjectUri;
    }

    private LocationObservationModel initializeLocationObservationFromSparqlFields(
            URI moveUri,
            Instant startDate,
            Instant endDate,
            URI featurOfInterest,
            boolean tooFacilityHasGeometry,
            URI from,
            URI too
    ){
        LocationObservationModel result =  new LocationObservationModel();
        result.setMoveUri(moveUri);
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setFeatureOfInterest(featurOfInterest);
        result.setHasGeometry(tooFacilityHasGeometry);
        LocationModel singleLocationModel = new LocationModel();
        singleLocationModel.setFrom(from);
        singleLocationModel.setTo(too);
        result.setLocation(singleLocationModel);
        return result;
    }

    private StringUriMap<URI> sparqlAddLocationCollection(StringUriMap<List<LocationObservationModel>> soLocationListMap) throws OpensilexModuleUpdateException {

        StringUriMap<URI> soCollectionMap = new StringUriMap<>();

        try {
            List<URI> soURIlist = soLocationListMap.keySet().stream().map(URI::create).toList();
            List<LocationObservationCollectionModel> collectionsToInsert = soURIlist.stream().map(e->{
                LocationObservationCollectionModel nextCollection = new LocationObservationCollectionModel();
                nextCollection.setFeatureOfInterest(e);
                return nextCollection;
            }).toList();

            LocationObservationCollectionDAO locationObservationCollectionDAO = new LocationObservationCollectionDAO(sparql);
            locationObservationCollectionDAO.createList(collectionsToInsert);

            for(LocationObservationCollectionModel nextCollection : collectionsToInsert){
                soCollectionMap.put(nextCollection.getFeatureOfInterest(), nextCollection.getUri());
            }

        } catch (Exception e) {
            throw new OpensilexModuleUpdateException("error while adding observation collections. No changes was saved on the rdf database", e);
        }
        logger.info("Observation collections were added and saved in the rdf database");

        return soCollectionMap;
    }

    private void setObservationCollectionAndMoveUrisAndInsertLocationObservations(StringUriMap<List<LocationObservationModel>> soLocationListMap, StringUriMap<URI> soCollectionMap, List<MoveModel> newMoves ) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        MongoCollection<LocationObservationModel> locationCollection = mongodb.getDatabase().getCollection(LocationObservationDAO.LOCATION_COLLECTION_NAME, LocationObservationModel.class);

        soLocationListMap.forEach((feature, locationList) -> locationList.forEach(location -> {
            location.setObservationCollection(soCollectionMap.get(URI.create(feature)));

            //If a location's MoveUri is null, then it means it was created from the geospatial coordinates of an OS (and not a generic Move)
            // The corresponding Moves that were created earlier in migration are in newMoves, there will only be one per OS as there was also only one
            // geospatial position per OS.
            if(location.getMoveUri() == null) {
                 URI moveUri = newMoves.stream()
                         .filter(move -> move.getTargets().stream().anyMatch(target -> SPARQLDeserializers.compareURIs(target, location.getFeatureOfInterest())))
                         .map(SPARQLResourceModel::getUri)
                         .findFirst().orElseThrow();
                location.setMoveUri(moveUri);
            }
        }));

        LocationObservationDAO locationObservationDAO = new LocationObservationDAO(mongodb.getServiceV2());
        locationObservationDAO.create(soLocationListMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));

    }
}
