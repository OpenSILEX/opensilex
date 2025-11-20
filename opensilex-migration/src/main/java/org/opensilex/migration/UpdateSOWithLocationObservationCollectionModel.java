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

package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UpdateSOWithLocationObservationCollectionModel implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;
    private SPARQLService sparql;
    private MongoDBService mongodb;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String OLD_MOVE_COLLECTION = "move";

    @Override
    public String getDescription() {
        return "In MongoDB, get scientific objects from the Geospatial and Move collections to the new Location Collection with the new model and observationCollection URI. In RDF4J, add ObservationCollection properties for each scientific objects with location. ";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();
        mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        try {
            sparql.startTransaction();
            mongodb.startTransaction();

            //1 - Get List SO with geospatial from mongo (subclass voc:so)--> get map = list URI (key) map with list of geospatial locations (values)
            Map<URI, List<LocationObservationModel>> soLocationGeospatialListMap = mongoGetGeospatialFromMongo();
            //2 - Get move detail with SO as target in RDF - Filter each move : map uri so with move details : endDate, startDate?, To?, From?
            Stream<SPARQLResult> moveDetailsList = sparqlGetMoveDetails();
            //3 - Create a move in RDF4J for each location from geospatial Collection
            List<MoveModel> newMoves = sparqlCreateSoMoves(soLocationGeospatialListMap);
            //4 - Get List SO move from mongo --> completed map = list URI (key) map with list of move locations (values)
            Map<URI, List<LocationObservationModel>> soLocationListMap = mongoGetMoveFromMongo(soLocationGeospatialListMap, moveDetailsList);
            //5 - Add collection for each SO (key) --> map featureOfInterest URI/Collection URI
            Map<URI,URI> soCollectionMap = sparqlAddLocationCollection(soLocationListMap);
            //6 - Completed location models for each SO and insert to Location collection
            mongoSoToLocationCollection(soLocationListMap, soCollectionMap, newMoves);

            sparql.commitTransaction();
            mongodb.commitTransaction();
            logger.info("Migration successfully completed");

        } catch (Exception e) {
            try {
                sparql.rollbackTransaction();
                mongodb.rollbackTransaction();
                logger.error("error while migrate scientific object locations. No changes was saved on databases", e);
            } catch (Exception exception) {
                throw new OpensilexModuleUpdateException("error while migrate scientific object locations. No changes was saved on databases", exception);
            }
        }
    }

    private Map<URI, List<LocationObservationModel>> mongoGetGeospatialFromMongo() throws SPARQLException {
        //Get OS subClass
        SelectBuilder select = new SelectBuilder().addWhere(new TriplePath(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, Oeso.ScientificObject.asNode()));
        List<URI> soRdfType = sparql.executeSelectQueryAsStream(select)
                .map(sparqlResult -> URI.create(SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(ScientificObjectModel.TYPE_FIELD))))
                .collect(Collectors.toList());

        Map<URI, List<LocationObservationModel>> soLocationListMap = new HashMap<>();
        MongoDatabase db = mongodb.getDatabase();

        //Get SO from geospatial collection
        MongoCollection<GeospatialModel> geospatialCollection = db.getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
        List<GeospatialModel> soFromGeospatial = geospatialCollection.find(Filters.in(SPARQLResourceModel.TYPE_FIELD, soRdfType)).into(new ArrayList<>());

        // Mapping FeatureOfInterest and locations
        // from geospatial collection
        soFromGeospatial.forEach(geo ->{
            //build location observation
            LocationModel location = new LocationModel();
            location.setGeometry(geo.getGeometry());

            LocationObservationModel locationObservation = new LocationObservationModel();
            locationObservation.setLocation(location);
            locationObservation.setEndDate(Instant.parse("1970-01-01T00:00:00.000Z"));
            locationObservation.setHasGeometry(true);
            locationObservation.setFeatureOfInterest(geo.getUri());

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
        Var eventVar = makeVar(EventModel.GRAPH);
        Var endVar = makeVar(EventModel.END_FIELD);
        Var endDateVar = makeVar(LocationObservationModel.END_DATE_FIELD);
        Var startVar = makeVar(EventModel.START_FIELD);
        Var startDateVar = makeVar(LocationObservationModel.START_DATE_FIELD);
        Var toVar = makeVar(LocationModel.TO_FIELD);
        Var fromVar = makeVar(LocationModel.FROM_FIELD);
        Var featureVar = makeVar(LocationObservationModel.FEATURE_OF_INTEREST_FIELD);
        Var soTypeVar = makeVar(ScientificObjectModel.TYPE_FIELD);

        //where SO clause
        WhereBuilder whereSO = new WhereBuilder()
                .addWhere(featureVar, RDF.type, soTypeVar)
                .addWhere(new TriplePath(soTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode()));

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
                .addWhere(whereSO)
                .addWhere(where);

        return sparql.executeSelectQueryAsStream(select);
    }

    private List<MoveModel> sparqlCreateSoMoves(Map<URI, List<LocationObservationModel>> soLocationListMap){
        try {
            //build move
            MoveEventDAO moveDao = new MoveEventDAO(sparql, mongodb);

            List<MoveModel> moveModels = new ArrayList<>();

            soLocationListMap.values().stream()
                    .flatMap(Collection::stream)
                    .forEach(location -> {
                        InstantModel defaultEnd = new InstantModel();
                        defaultEnd.setType(org.opensilex.sparql.model.time.Time.InstantURI);
                        defaultEnd.setDateTimeStamp(OffsetDateTime.parse("2017-05-19T00:00:00Z"));

                        if (location.getEndDate() != null && location.getUri() == null) {
                            MoveModel moveModel = new MoveModel();
                            moveModel.setEnd(defaultEnd);
                            moveModel.setIsInstant(true);
                            moveModel.setTargets(Collections.singletonList(location.getFeatureOfInterest()));
                            moveModels.add(moveModel);
                        }
                    });

            return moveDao.create(moveModels);

        } catch (SPARQLException e) {
            throw new RuntimeException(e);
        } catch (SPARQLDeserializerNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<URI, List<LocationObservationModel>> mongoGetMoveFromMongo(Map<URI, List<LocationObservationModel>> soLocationGeospatialListMap, Stream<SPARQLResult> moveDetailsList){
        //Get SO from move collection
        MongoDatabase db = mongodb.getDatabase();
        MongoCollection<MoveNosqlModel> moveCollection = db.getCollection(OLD_MOVE_COLLECTION, MoveNosqlModel.class);

        Map<URI,SPARQLResult> moveURIs = moveDetailsList.collect(Collectors.toMap(
             sparqlResult -> URI.create(sparqlResult.getStringValue(EventModel.GRAPH)),
                sparqlResult -> sparqlResult,
                (oldValue, newValue) -> oldValue
                ));

        List<MoveNosqlModel> soFromMove = moveCollection.find(Filters.in("_id", new ArrayList<>(moveURIs.keySet()))).into(new ArrayList<>());

        // Mapping FeatureOfInterest and locations
        //from move collection
        soFromMove.forEach(move -> move.getTargetPositions().forEach(position -> {
            //build location observation
            LocationObservationModel locationObservation = new LocationObservationModel();
            LocationModel location = new LocationModel();
            URI target = position.getTarget();

            //mongo
            if(position.getPosition() != null) {
                if(position.getPosition().getCoordinates() != null) {
                    location.setGeometry(position.getPosition().getCoordinates());
                    locationObservation.setHasGeometry(true);
                }
                if(position.getPosition().getTextualPosition() != null) {
                    location.setTextualPosition(position.getPosition().getTextualPosition());
                }
                if(position.getPosition().getX() != null) {
                    location.setX(position.getPosition().getX());
                }
                if(position.getPosition().getY() != null) {
                    location.setY(position.getPosition().getY());
                }
                if(position.getPosition().getZ() != null) {
                    location.setZ(position.getPosition().getZ());
                }
            }

            //rdf
            SPARQLResult moveDetails = moveURIs.values().stream().filter(sparqlResult -> SPARQLDeserializers
                            .compareURIs(URI.create(sparqlResult.getStringValue(EventModel.GRAPH)), move.getUri()))
                    .findFirst().orElse(null);

            if (moveDetails != null) {
                locationObservation.setEndDate(Instant.parse(moveDetails.getStringValue(LocationObservationModel.END_DATE_FIELD)));

                if (moveDetails.getStringValue(LocationObservationModel.START_DATE_FIELD) != null) {
                    locationObservation.setStartDate(Instant.parse(moveDetails.getStringValue(LocationObservationModel.START_DATE_FIELD)));
                }
                if (moveDetails.getStringValue(LocationModel.TO_FIELD) != null) {
                    location.setTo(URI.create(moveDetails.getStringValue(LocationModel.TO_FIELD)));
                }
                if (moveDetails.getStringValue(LocationModel.FROM_FIELD) != null) {
                    location.setFrom(URI.create(moveDetails.getStringValue(LocationModel.FROM_FIELD)));
                }
            }

            locationObservation.setLocation(location);

            //Set hasGeometry at true if the "to" facility has corresponding location
            if(!locationObservation.isHasGeometry() && locationObservation.getLocation().getTo() != null) {
                LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());
                boolean hasGeometry = observationLogic.checkHasGeometry(locationObservation, locationObservation.getStartDate(), locationObservation.getEndDate());
                locationObservation.setHasGeometry(hasGeometry);
            }

            locationObservation.setFeatureOfInterest(target);
            locationObservation.setUri(move.getUri()); //store event URI

            //if a location from the geospatial collection already exists, add it to the existing list, otherwise create a new entry
            List<LocationObservationModel> list = soLocationGeospatialListMap.get(target);
            if(Objects.isNull(list)) {
                soLocationGeospatialListMap.put(target,List.of(locationObservation));
            }else{
                List<LocationObservationModel> mutableList = new ArrayList<>(list);
                mutableList.add(locationObservation);

                soLocationGeospatialListMap.put(target,mutableList);
            }
        }));
        return soLocationGeospatialListMap;
    }

    /**
     * First migration in RDF4J to add a ObservationCollection to all SO from map.
     * <p>
     * The request to do that is :
     * <pre>
     *     PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *     PREFIX  org:  <http://www.w3.org/ns/org#>
     *     PREFIX  sosa: <http://www.w3.org/ns/sosa/>
     *     PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     *
     *  INSERT {
     *      GRAPH  <http://opensilex.test/set/ObservationCollection> {
     *          ?uri a sosa:ObservationCollection .
     *          ?uri sosa:hasFeatureOfInterest ?objects .
     *      }
     * }
     * WHERE {
     *   	    ?objects rdfs:label ?name
     *   	    FILTER ( ?objects IN (
     *          <http://opensilex....>,
     *          <http://opensilex....>
     *          ))
     *   BIND(REPLACE(?name, " ", "") AS ?nameFormated)
     *   BIND(ROUND(RAND()*100) AS ?random)
     *   BIND (URI(CONCAT("http://opensilex.test/id/observationCollection/so",STR(?nameFormated),"/",replace(str(?random),".0",""))) AS ?uri)
     * }
     * </pre>
     */
    private Map<URI,URI> sparqlAddLocationCollection(Map<URI, List<LocationObservationModel>> soLocationListMap) throws OpensilexModuleUpdateException {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();

        Map<URI,URI> soCollectionMap;

        try {
            List<URI> soURIlist = soLocationListMap.keySet().stream().toList();

            //1 - Add Observation Collection
            //Graph
            Node graphObservationCollection = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(LocationObservationCollectionModel.class));

            //Variables
            Var observationCollectionURIVar = makeVar(LocationObservationCollectionModel.URI_FIELD);
            Var objectsVar = makeVar("objects");
            Var nameVar = makeVar(ScientificObjectModel.NAME_FIELD);
            Var subject = makeVar("subject");
            Var nameFormatedVar = makeVar("nameFormated");
            Var randomVar = makeVar("random");

            //Insert CollectionObservation
            UpdateBuilder query = new UpdateBuilder()
                    .addInsert(graphObservationCollection, observationCollectionURIVar, RDF.type.asNode(), SOSA.ObservationCollection.asNode())
                    .addInsert(graphObservationCollection, observationCollectionURIVar, SOSA.hasFeatureOfInterest.asNode(), objectsVar);

            //where clause of the update Query
            WhereBuilder where = new WhereBuilder()
                    .addWhere(objectsVar, RDFS.label.asNode(), nameVar)
                    .addFilter(SPARQLQueryHelper.inURIFilter(objectsVar, soURIlist));

            //add not exists filter to avoid duplicates
            WhereBuilder whereObservationCollection = new WhereBuilder();
            whereObservationCollection.addGraph(graphObservationCollection, subject, SOSA.hasFeatureOfInterest.asNode(), objectsVar);
            where.addFilter(new E_NotExists(whereObservationCollection.getHandlerBlock().getWhereHandler().getElement()));

            //Bind functions of the where clause
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            Expr exprNameFormated = exprFactory.replace(nameVar.asNode(), " ", "");
            where.addBind(exprNameFormated, nameFormatedVar);
            where.addBind(exprFactory.rand(), randomVar);

            Expr exprURI = exprFactory.iri(new ExprFactory().concat(
                    "http://opensilex.test/id/observationCollection/objects/",
                    exprFactory.str(nameFormatedVar.asNode()),
                    "/",
                    exprFactory.replace(new ExprFactory().str(randomVar.asNode()), "0.", ""))
            );
            where.addBind(exprURI, observationCollectionURIVar);

            //Add the where clause with the graph
            query.addWhere(where);

            sparql.executeUpdateQuery(query);

            // Get SO and associated observation collection
            /**
             *  Select *
             *  WHERE {
             *            ?s  sosa:hasFeatureOfInterest ?objects
             *            FILTER( ?objectss IN (
             *            <http://opensilex.test/id/...>,
             *            <http://opensilex.test/id/...>
             *                ))
             *      }
             */
            SelectBuilder select = new SelectBuilder()
                    .addWhere(makeVar(LocationObservationModel.OBSERVATION_COLLECTION_FIELD), SOSA.hasFeatureOfInterest, objectsVar)
                    .addFilter(SPARQLQueryHelper.inURIFilter(objectsVar, soURIlist));

            soCollectionMap = sparql.executeSelectQueryAsStream(select).collect(Collectors.toMap(
                            sparqlResult -> URI.create(sparqlResult.getStringValue("objects")), //key
                            sparqlResult -> URI.create(sparqlResult.getStringValue(LocationObservationModel.OBSERVATION_COLLECTION_FIELD)), //value
                            (oldValue, newValue) -> oldValue
                    )
            );
        } catch (Exception e) {
            throw new OpensilexModuleUpdateException("error while adding observation collections. No changes was saved on the rdf database", e);
        }
        logger.info("Observation collections were added and saved in the rdf database");

        return soCollectionMap;
    }

    private void mongoSoToLocationCollection(Map<URI, List<LocationObservationModel>> soLocationListMap, Map<URI, URI> soCollectionMap, List<MoveModel> newMoves ) {
        MongoCollection<LocationObservationModel> locationCollection = mongodb.getDatabase().getCollection(LocationObservationDAO.LOCATION_COLLECTION_NAME, LocationObservationModel.class);

        soLocationListMap.forEach((feature, locationList) -> locationList.forEach(location -> {
            location.setObservationCollection(soCollectionMap.get(feature));

            if(location.getUri() == null) {   //location from geospatial - add move URI as id_
                 URI moveUri = newMoves.stream()
                         .filter(move -> move.getTargets().contains(location.getFeatureOfInterest()))
                         .map(SPARQLResourceModel::getUri)
                         .findFirst().get();
                location.setUri(moveUri);
            }
        }));

        locationCollection.insertMany(soLocationListMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
