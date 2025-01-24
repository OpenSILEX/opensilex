 /*
 * *****************************************************************************
 *                         UpdateFacilitiesWithLocationObservationCollectionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 22/10/2024 11:49
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

 package org.opensilex.migration;

 import com.mongodb.client.MongoCollection;
 import com.mongodb.client.MongoDatabase;
 import com.mongodb.client.model.Filters;
 import com.mongodb.client.model.geojson.Geometry;
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
 import org.opensilex.core.external.geocoding.GeocodingService;
 import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
 import org.opensilex.core.geospatial.dal.GeospatialDAO;
 import org.opensilex.core.geospatial.dal.GeospatialModel;
 import org.opensilex.core.location.dal.LocationModel;
 import org.opensilex.core.location.dal.LocationObservationCollectionModel;
 import org.opensilex.core.location.dal.LocationObservationDAO;
 import org.opensilex.core.location.dal.LocationObservationModel;
 import org.opensilex.core.ontology.Oeso;
 import org.opensilex.core.ontology.SOSA;
 import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
 import org.opensilex.core.organisation.dal.OrganizationModel;
 import org.opensilex.core.organisation.dal.facility.FacilityAddressModel;
 import org.opensilex.core.organisation.dal.facility.FacilityModel;
 import org.opensilex.nosql.mongodb.MongoDBService;
 import org.opensilex.nosql.mongodb.MongoModel;
 import org.opensilex.sparql.deserializer.SPARQLDeserializers;
 import org.opensilex.sparql.exceptions.SPARQLException;
 import org.opensilex.sparql.model.SPARQLResourceModel;
 import org.opensilex.sparql.service.SPARQLQueryHelper;
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

 import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

 public class UpdateFacilitiesWithLocationObservationCollectionModel implements OpenSilexModuleUpdate {

     private OpenSilex opensilex;
     private SPARQLService sparql;
     private MongoDBService mongodb;
     private final Logger logger = LoggerFactory.getLogger(getClass());

     @Override
     public String getDescription() {
         return "In MongoDB, get facilities from the Geospatial Collection to the new Location Collection with the new model and observationCollection URI. In RDF4J, add ObservationCollection properties for each Site with address or with geometry. ";
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

             // 1 Mongo : get all facilities with geometry or address geometry
             List<GeospatialModel> facilityPositionList = mongoGetFacilitiesFromGeospatial();
             // 2 RDF4J : add observation collection to facilities with address or with geometry (mongo - geospatial)
             Map<URI,URI> facilityCollectionMap = sparqlAddObservationCollectionToFacilityList(facilityPositionList);
             // 3 RDF4J : get facility addresses
             Map<URI, FacilityAddressModel> facilityAddressMap = sparqlgetAddressToFacilityList(facilityPositionList);
             // 3 Mongo : update facility geometry in geospatial collection and copy in location collection
             mongoFacilitiesFromGeospatialToLocationCollection(facilityPositionList,facilityCollectionMap, facilityAddressMap);

             sparql.commitTransaction();
             mongodb.commitTransaction();
             logger.info("Migration successfully completed");

         } catch (Exception e){
             try {
                 sparql.rollbackTransaction();
                 mongodb.rollbackTransaction();
                 logger.error("error while migrate facility locations. No changes was saved on databases", e);
             } catch (Exception exception) {
                 throw new OpensilexModuleUpdateException("error while migrate facility locations. No changes was saved on databases", exception);
             }
         }
     }

     private List<GeospatialModel> mongoGetFacilitiesFromGeospatial() throws SPARQLException {
         MongoDatabase db = mongodb.getDatabase();

         SelectBuilder select = new SelectBuilder().addWhere(new TriplePath(makeVar(FacilityModel.TYPE_FIELD), Ontology.subClassAny, Oeso.Facility.asNode()));
         List<URI> facilityRdfType = sparql.executeSelectQueryAsStream(select).map(sparqlResult -> URI.create(SPARQLDeserializers.getExpandedURI(sparqlResult.getStringValue(FacilityModel.TYPE_FIELD)))).collect(Collectors.toList());

         // Get Facilities from Geospatial Collection
         MongoCollection<GeospatialModel> geospatialCollection = db.getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
         return geospatialCollection.find(Filters.in(SPARQLResourceModel.TYPE_FIELD, facilityRdfType)).into(new ArrayList<>());
     }

     /**
      * First migration in RDF4J to add a ObservationCollection to all sites with an address or a geometry.
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
      *          ?uri sosa:hasFeatureOfInterest ?facilities .
      *      }
      * }
      * WHERE {
      *   GRAPH <http://opensilex.test/set/organization> {
      *   	    ?facilities rdfs:label ?name
      *   	    FILTER ( ?facilities IN (
      *          <http://opensilex.test/id/organization/facility.facility_with_address>,
      *          <http://opensilex.test/id/organization/facility.facility_geom_adress>
      *          ))
      *   }
      *   BIND(REPLACE(?name, " ", "") AS ?nameFormated)
      *   BIND(ROUND(RAND()*100) AS ?random)
      *   BIND (URI(CONCAT("http://opensilex.test/id/observationCollection/facility",STR(?nameFormated),"/",replace(str(?random),".0",""))) AS ?uri)
      * }
      * </pre>
      */


     private Map<URI,URI> sparqlAddObservationCollectionToFacilityList(List<GeospatialModel> facilityPositionList) throws OpensilexModuleUpdateException {

         SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
         sparql = factory.provide();

         Map<URI,URI> facilityCollectionMap;

         try {
             List<URI> facilityList = facilityPositionList.stream().map(MongoModel::getUri).collect(Collectors.toList());

             //Graph
             Node graphOrganization = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(OrganizationModel.class));
             Node graphObservationCollection = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(LocationObservationCollectionModel.class));

             //Variables
             Var observationCollectionURIVar = makeVar(LocationObservationCollectionModel.URI_FIELD);
             Var facilitiesVar = makeVar(OrganizationModel.FACILITIES_FIELD);
             Var nameVar = makeVar(FacilityModel.NAME_FIELD);
             Var nameFormatedVar = makeVar("nameFormated");
             Var randomVar = makeVar("random");
             Var subject = makeVar("subject");

             // insert CollectionObservation
             UpdateBuilder query = new UpdateBuilder()
                     .addInsert(graphObservationCollection, observationCollectionURIVar, RDF.type.asNode(), SOSA.ObservationCollection.asNode())
                     .addInsert(graphObservationCollection, observationCollectionURIVar, SOSA.hasFeatureOfInterest.asNode(), facilitiesVar);


             //where clause of the update Query
             //for graph organization
             WhereBuilder whereOrganization = new WhereBuilder()
                     .addWhere(facilitiesVar, RDFS.label.asNode(),nameVar)
                     .addFilter(SPARQLQueryHelper.inURIFilter(facilitiesVar, facilityList));

             //add not exists filter to avoid duplicates
             WhereBuilder whereObservationCollection = new WhereBuilder();
             whereObservationCollection.addGraph(graphObservationCollection, subject, SOSA.hasFeatureOfInterest.asNode(), facilitiesVar);
             whereOrganization.addFilter(new E_NotExists(whereObservationCollection.getHandlerBlock().getWhereHandler().getElement()));

             WhereBuilder insertWhereOrganization = new WhereBuilder().addGraph(graphOrganization, whereOrganization);
             WhereBuilder where = new WhereBuilder().addWhere(insertWhereOrganization);

             //Bind functions of the where clause
             ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

             Expr exprNameFormated = exprFactory.replace(nameVar.asNode()," ","");
             where.addBind(exprNameFormated, nameFormatedVar);
             where.addBind(exprFactory.rand(), randomVar);

             Expr exprURI = exprFactory.iri(new ExprFactory().concat(
                     "http://opensilex.test/id/observationCollection/facility/",
                     exprFactory.str(nameFormatedVar.asNode()),
                     "/",
                     exprFactory.replace(new ExprFactory().str(randomVar.asNode()),"0.",""))
             );
             where.addBind(exprURI, observationCollectionURIVar);

             //Add the where clause with the graph
             query.addWhere(where);

             sparql.executeUpdateQuery(query);

             // Get Facility and associated observation collection
             /**
              *  Select *
              *  WHERE {
              *            ?s  sosa:hasFeatureOfInterest ?facilities
              *            FILTER( ?facilities IN (
              *            <http://opensilex.test/id/organization/facility.facility_with_address>,
              *            <http://opensilex.test/id/organization/facility.facility_geom_adress>
              *                ))
              *      }
              */
             SelectBuilder select = new SelectBuilder()
                     .addWhere(makeVar(LocationObservationModel.OBSERVATION_COLLECTION_FIELD),SOSA.hasFeatureOfInterest,facilitiesVar)
                     .addFilter(SPARQLQueryHelper.inURIFilter(facilitiesVar, facilityList));

             facilityCollectionMap = sparql.executeSelectQueryAsStream(select).collect(Collectors.toMap(
                     sparqlResult -> URI.create(sparqlResult.getStringValue(OrganizationModel.FACILITIES_FIELD)) , //key
                     sparqlResult ->  URI.create(sparqlResult.getStringValue(LocationObservationModel.OBSERVATION_COLLECTION_FIELD)) //value
                     )
             );
         } catch (Exception e) {
             throw new OpensilexModuleUpdateException("error while adding observation collections. No changes was saved on the rdf database", e);
         }

         logger.info("Observation collections were added and saved in the rdf database");

         return facilityCollectionMap;
     }

     private Map<URI, FacilityAddressModel> sparqlgetAddressToFacilityList(List<GeospatialModel> facilityPositionList){
         Map<URI,FacilityAddressModel> facilityAddressMap = new HashMap<>();

         facilityPositionList.forEach(position-> {
                     try {
                         FacilityModel facility = sparql.getByURI(FacilityModel.class, position.getUri(), "en");
                         if(Objects.nonNull(facility.getAddress())){
                             facilityAddressMap.put(position.getUri(),facility.getAddress());
                         }
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 }
         );

         return facilityAddressMap;
     }

     private void mongoFacilitiesFromGeospatialToLocationCollection(List<GeospatialModel> geospatialFacilityList, Map<URI,URI> facilityCollectionMap, Map<URI, FacilityAddressModel> facilityAddressMap) throws SPARQLException {
         MongoCollection<LocationObservationModel> locationCollection = mongodb.getDatabase().getCollection(LocationObservationDAO.LOCATION_COLLECTION_NAME, LocationObservationModel.class);

         // 1- Check existing locations to avoid duplicates
         List<LocationObservationModel> existingLocations = locationCollection.find(Filters.empty()).into(new ArrayList<>());
         if(!existingLocations.isEmpty()){
             List<URI> existingLocationURI = existingLocations.stream().map(LocationObservationModel::getObservationCollection).collect(Collectors.toList());
             facilityCollectionMap.forEach((feature,collection) ->{
                 boolean match = existingLocationURI.stream().anyMatch(uri -> SPARQLDeserializers.compareURIs(uri, collection));
                 if (match) {
                     // Exclude Location to the geospatial collection list
                     List<GeospatialModel> facilityToExclude = geospatialFacilityList.stream().filter(geospatialFacility -> SPARQLDeserializers.compareURIs(geospatialFacility.getUri(), feature)).collect(Collectors.toList());
                     if(!facilityToExclude.isEmpty()){
                             geospatialFacilityList.remove(facilityToExclude.get(0));
                     }
                 }
             });
         }

         // 2- Format Facilities according to the new model
         List<LocationObservationModel> locationObservationModelList = geospatialFacilityList.stream().map(geospatialFacility -> {
             LocationObservationModel locationObservationModel = new LocationObservationModel();

             URI observationCollectionURI = facilityCollectionMap.get(URI.create(SPARQLDeserializers.getExpandedURI(geospatialFacility.getUri())));

             locationObservationModel.setObservationCollection(observationCollectionURI);
             locationObservationModel.setFeatureOfInterest(geospatialFacility.getUri());
             locationObservationModel.setHasGeometry(true);

             // Add endDate (default date) only if the geometry is from the geospatial coordinates
             FacilityAddressModel address =  facilityAddressMap.get(geospatialFacility.getUri());

             if(Objects.isNull(address)){
                 locationObservationModel.setEndDate(Instant.parse("1970-01-01T00:00:00.00Z"));
             } else {
                 FacilityAddressDTO addressDto = new FacilityAddressDTO();
                 addressDto.fromModel(address);

                 GeocodingService geocodingService = new OpenStreetMapGeocodingService();
                 Geometry geometry = geocodingService.getPointFromAddress(addressDto.toReadableAddress());

                 if(!geometry.equals(geospatialFacility.getGeometry())){
                     locationObservationModel.setEndDate(Instant.parse("1970-01-01T00:00:00.00Z"));
                 }
             }

             LocationModel locationModel = new LocationModel();

             locationModel.setGeometry(geospatialFacility.getGeometry());
             locationObservationModel.setLocation(locationModel);

             String randomNumber = Integer.toString(new Random().nextInt(100));
             locationObservationModel.setUri(URI.create(observationCollectionURI.toString()+ "/" + randomNumber));

             return locationObservationModel;
         }).collect(Collectors.toList());

         // 3- Insert Sites into new location Collection
         if(!locationObservationModelList.isEmpty()){
             locationCollection.insertMany(locationObservationModelList);
         }
     }
 }
