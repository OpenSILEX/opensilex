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

            //1 - Get List SO with geospatial and move from mongo (subclass voc:so)--> get map = list URI (key) map with list of geospatial/move locations (values)
            Map<URI, List<LocationObservationModel>> soLocationListMap = mongoGetGeospatialAndMoveFromMongo();
            //2 - Add collection for each SO (key) --> map featureOfInterest URI/Collection URI
            Map<URI,URI> soCollectionMap= sparqlAddLocationCollection(soLocationListMap);
            // 3 - Filter each move : map uri so with move details : endDate, startDate?, To?, From?
            Stream<SPARQLResult> moveDetailsList = sparqlGetMoveDetails(soLocationListMap);
            //4 - Create a move in RDF4J for each location from geospatial Collection
            sparqlCreateSoMoves(soLocationListMap);
            //5 - Complete location models for each SO and insert to Location collection
            mongoSoToLocationCollection(soLocationListMap,soCollectionMap,moveDetailsList);
            /*
             Move:
             {
  "_id": "http://opensilex.test/set/event/062c0909-d7ed-4005-9b0f-20bcf5254d40", --> id event
  "targetPositions": [
    {
      "position": {},
      "target": "http://www.phenome-fppn.fr/m3p/id/dyn/2009/sa090006" --> URI feature of interest
    }
  ]
}

        Geospatial:
        {
  "_id": {
    "$oid": "6241658160db9b71238bb9c0"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": []
  },
  "graph": "http://www.phenome-fppn.fr/diaphen/DIA2017-05-19", --> URI XP
  "name": "1/DZ_PG_67/ZM4394/WW/1/DIA2017-05-19", --> SO Name
  "rdfType": "http://www.opensilex.org/vocabulary/oeso#Plot", --> SO type
  "uri": "http://www.phenome-fppn.fr/diaphen/2017/o17000001" --> URI feature of interest
}
          */

            sparql.commitTransaction();
            mongodb.commitTransaction();
            logger.info("Migration successfully completed");

        } catch (Exception e) {
            try {
                sparql.rollbackTransaction();
                mongodb.rollbackTransaction();
                logger.error("error while migrate site locations. No changes was saved on databases", e);
            } catch (Exception exception) {
                throw new OpensilexModuleUpdateException("error while migrate site locations. No changes was saved on databases", exception);
            }
        }
    }

    private Map<URI, List<LocationObservationModel>> mongoGetGeospatialAndMoveFromMongo() throws SPARQLException {
        Map<URI, List<LocationObservationModel>> soLocationListMap = new HashMap<>();
        MongoDatabase db = mongodb.getDatabase();

        //Get OS subClass
        SelectBuilder select = new SelectBuilder().addWhere(new TriplePath(makeVar(ScientificObjectModel.TYPE_FIELD), Ontology.subClassAny, Oeso.ScientificObject.asNode()));
        List<URI> soRdfType = sparql.executeSelectQueryAsStream(select)
                .map(sparqlResult -> URI.create(SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(ScientificObjectModel.TYPE_FIELD))))
                .collect(Collectors.toList());

        //Get SO from geospatial collection
        MongoCollection<GeospatialModel> geospatialCollection = db.getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
        List<GeospatialModel> soFromGeospatial = geospatialCollection.find(Filters.in(SPARQLResourceModel.TYPE_FIELD, soRdfType)).into(new ArrayList<>());

        //Get SO from move collection
        MongoCollection<MoveNosqlModel> moveCollection = db.getCollection(MoveEventNoSqlDao.COLLECTION_NAME, MoveNosqlModel.class);
        List<MoveNosqlModel> soFromMove = moveCollection.find(Filters.in(SPARQLResourceModel.TYPE_FIELD, soRdfType)).into(new ArrayList<>());

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

            soLocationListMap.put(geo.getUri(), Collections.singletonList(locationObservation));
        });
        //from move collection
        soFromMove.forEach(move ->{
            move.getTargetPositions().forEach(position -> {
                //build location observation
                LocationObservationModel locationObservation = new LocationObservationModel();
                LocationModel location = new LocationModel();
                URI target = position.getTarget();

                if(position.getPosition() != null) {
                    if(position.getPosition().getCoordinates() != null) {
                        location.setGeometry(position.getPosition().getCoordinates());
                        locationObservation.setHasGeometry(true);
                    }
                    if(position.getPosition().getTextualPosition() != null) {
                        position.getPosition().setTextualPosition(position.getPosition().getTextualPosition());
                    }
                    if(position.getPosition().getX() != null) {
                        position.getPosition().setX(position.getPosition().getX());
                    }
                    if(position.getPosition().getY() != null) {
                        position.getPosition().setY(position.getPosition().getY());
                    }
                    if(position.getPosition().getZ() != null) {
                        position.getPosition().setZ(position.getPosition().getZ());
                    }
                }

                locationObservation.setLocation(location);
                locationObservation.setFeatureOfInterest(target);
                locationObservation.setUri(move.getUri()); //store (temporary?) event URI to get it in RDF4J

                //if a location from the geospatial collection already exists, add it to the existing list, otherwise create a new entry
                List<LocationObservationModel> list = soLocationListMap.get(target);
                if(Objects.isNull(list)) {
                    soLocationListMap.put(target, Collections.singletonList(locationObservation));
                }else{
                   list.add(locationObservation);
                }
            });
        });

        return soLocationListMap;
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
                            sparqlResult -> URI.create(sparqlResult.getStringValue(LocationObservationModel.OBSERVATION_COLLECTION_FIELD)) //value
                    )
            );
        } catch (Exception e) {
            throw new OpensilexModuleUpdateException("error while adding observation collections. No changes was saved on the rdf database", e);
        }
        logger.info("Observation collections were added and saved in the rdf database");

        return soCollectionMap;
    }

    private Stream<SPARQLResult> sparqlGetMoveDetails(Map<URI, List<LocationObservationModel>> soCollectionMap) throws SPARQLException {
        List<URI> eventURIList = soCollectionMap.values().stream()
                .flatMap(Collection::stream)
                .filter(loc -> Objects.nonNull(loc.getUri()))
                .map(loc -> loc.getUri())
                .collect(Collectors.toList());

        /**
         * <pre>
         *     PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         *     PREFIX  org:  <http://www.w3.org/ns/org#>
         *     PREFIX  sosa: <http://www.w3.org/ns/sosa/>
         *     PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         *
         * SELECT *
         * WHERE {
         *  GRAPH <http://opensilex.test/set/event> {
         *   	    ?objects time:hasEnd ?end.
         *   	    ?objects oeev:concerns ?featureOfInterest.
         *   	    OPTIONAL
         *   	        { ?objects time:hasBeginning ?start}
         *   	    OPTIONAL
         *   	        { ?objects oeev:to ?facilityTo }
         *   	    OPTIONAL
         *        	   { ?objects oeev:from ?facilityFrom }
         *   	    }
         *   	    FILTER ( ?objects IN (
         *          <http://opensilex....>,
         *          <http://opensilex....>
         *          ))
         *       }
         * }
         * </pre>
         * */

        //Graph
        Node eventGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(EventModel.class));

        //Variables
        Var eventVar = makeVar(EventModel.GRAPH);
        Var endVar = makeVar(EventModel.END_FIELD);
        Var startVar = makeVar(EventModel.START_FIELD);
        Var toVar = makeVar(MoveModel.TO_FIELD);
        Var fromVar = makeVar(MoveModel.FROM_FIELD);
        Var featureVar = makeVar(LocationObservationModel.FEATURE_OF_INTEREST_FIELD);

        //where clause
        WhereBuilder whereEvent = new WhereBuilder()
                .addWhere(eventVar, Time.hasEnd, endVar)
                .addWhere(eventVar, Oeev.concerns, featureVar)
                .addOptional(eventVar, Time.hasBeginning, startVar)
                .addOptional(eventVar, Oeev.to, toVar)
                .addOptional(eventVar, Oeev.from, fromVar);

        whereEvent.addFilter(SPARQLQueryHelper.inURIFilter(eventVar, eventURIList));

        WhereBuilder where = new WhereBuilder().addGraph(eventGraph,whereEvent);
        SelectBuilder select = new SelectBuilder().addWhere(where);

        return sparql.executeSelectQueryAsStream(select);
    }

    private void sparqlCreateSoMoves(Map<URI, List<LocationObservationModel>> soLocationListMap){
        try {
            MoveEventDAO moveDao = new MoveEventDAO(sparql, mongodb);

        List<MoveModel> moveModels = new ArrayList<>();
        InstantModel defaultEnd = new InstantModel();
        defaultEnd.setDateTimeStamp(OffsetDateTime.parse("1970-01-01T00:00:00.000Z"));

        soLocationListMap.values().stream()
                .flatMap(Collection::stream)
                .forEach(location ->{
                    if(location.getEndDate() != null && location.getUri() == null){
                        MoveModel moveModel = new MoveModel();
                        moveModel.setEnd(defaultEnd);
                        moveModel.setIsInstant(true);
                        moveModel.setTargets(Collections.singletonList(location.getFeatureOfInterest()));
                        moveModels.add(moveModel);
                    }
                });

        moveDao.create(moveModels);
        //TODO: Add event URI as id Location i mongo??

        } catch (SPARQLException e) {
            throw new RuntimeException(e);
        } catch (SPARQLDeserializerNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void mongoSoToLocationCollection(Map<URI, List<LocationObservationModel>> soLocationListMap, Map<URI, URI> soCollectionMap, Stream<SPARQLResult> moveDetailsList) {
        MongoCollection<LocationObservationModel> locationCollection = mongodb.getDatabase().getCollection(LocationObservationDAO.LOCATION_COLLECTION_NAME, LocationObservationModel.class);
        Random random = new Random();

        soLocationListMap.forEach((feature, locationList) -> {
            locationList.forEach(location -> {
                location.setObservationCollection(soCollectionMap.get(feature));

                if(location.getUri() == null) {   //location from geospatial
                    String randomNumber = Integer.toString(random.nextInt(100));
                    location.setUri(URI.create(soCollectionMap.get(feature).toString() + "/" + randomNumber));
                } else{                                // location from move
                    SPARQLResult moveDetails = moveDetailsList.filter(move -> SPARQLDeserializers
                                    .compareURIs(URI.create(move.getStringValue(EventModel.GRAPH)), location.getUri()))
                                    .findFirst().get();
                    location.setEndDate(Instant.parse(moveDetails.getStringValue(EventModel.END_FIELD)));

                    if(moveDetails.getStringValue(EventModel.START_FIELD) != null) {
                        location.setStartDate(Instant.parse(moveDetails.getStringValue(EventModel.START_FIELD)));
                    }
               /*     if(moveDetails.getStringValue(MoveModel.TO_FIELD) != null) {
                        location.getLocation().setTo(URI.create(moveDetails.getStringValue(MoveModel.TO_FIELD)));
                    }
                    if(moveDetails.getStringValue(MoveModel.FROM_FIELD) != null) {
                        location.getLocation().setFrom(URI.create(moveDetails.getStringValue(MoveModel.FROM_FIELD)));
                    }*/
                }
            });
        });

        locationCollection.insertMany(soLocationListMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
