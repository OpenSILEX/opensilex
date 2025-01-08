/*
 * *****************************************************************************
 *                         UpdateSitesWithLocationObservationCollectionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 29/07/2024 09:27
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.ORG;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.conversions.Bson;
import org.opensilex.OpenSilex;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAllowedException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class UpdateSitesWithLocationObservationCollectionModel implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;
    private SPARQLService sparql;
    private MongoDBService mongodb;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getDescription() {
        return "In RDF4J, add ObservationCollection properties for each Site with address. In MongoDB, get sites from the Geospatial Collection to the new Location Collection with the new model and observationCollection URI.";
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

            sparqlAddObservationCollectionToSitesWithAddress();
            Map<URI, URI> siteObservationCollectionMap = getSiteObservationCollectionMap();
            mongoSitesFromGeospatialToLocationCollection(siteObservationCollectionMap);

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

    /**
     * First migration in RDF4J to add a ObservationCollection to all sites that had an address.
     * <p>
     * The request to do that is :
     * <pre>
     *     PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *     PREFIX  org:  <http://www.w3.org/ns/org#>
     *     PREFIX  sosa: <http://www.w3.org/ns/sosa/>
     *     PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     *
     *  INSERT {
     *      GRAPH <http://www.w3.org/ns/sosa/ObservationCollection> {
     *          ?newURI a sosa:ObservationCollection .
     *          ?newURI sosa:hasFeatureOfInterest ?site .
     *      }
     * }
     * WHERE {
     *   GRAPH <http://opensilex.test/set/organization> {
     *   	    ?site a org:Site ;
     *   		      org:siteAddress ?address ;
     *       		  a ?type ;
     *       		  rdfs:label ?name .
     *       	FILTER NOT EXISTS {
     *          	GRAPH <http://opensilex.test/set/ObservationCollection>
     *                     {?s sosa:hasFeatureOfInterest ?site}
     *                }
     *   }
     *   BIND(REPLACE(?name, " ", "") AS ?nameFormated)
     *   BIND (URI(CONCAT("http://opensilex.test/id/ObservationCollection/site/",STR(?nameFormated))) AS ?newURI)
     * }
     * </pre>
     */
    private void sparqlAddObservationCollectionToSitesWithAddress() throws OpensilexModuleUpdateException {

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();

        try {
            //Graph
            Node graphOrganization = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(OrganizationModel.class));
            Node graphObservationCollection = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(LocationObservationCollectionModel.class));

            //Variables
            Var observationCollectionURIVar = makeVar(LocationObservationCollectionModel.URI_FIELD);
            Var siteVar = makeVar(OrganizationModel.SITE_FIELD);
            Var addressVar = makeVar(SiteModel.ADDRESS_FIELD);
            Var typeVar = makeVar(SiteModel.TYPE_FIELD);
            Var nameVar = makeVar(SiteModel.NAME_FIELD);
            Var subject = makeVar("subject");
            Var nameFormatedVar = makeVar("nameFormated");
            Var randomVar = makeVar("random");

            // insert CollectionObservation
            UpdateBuilder query = new UpdateBuilder()
                    .addInsert(graphObservationCollection, observationCollectionURIVar, RDF.type.asNode(), SOSA.ObservationCollection.asNode())
                    .addInsert(graphObservationCollection, observationCollectionURIVar, SOSA.hasFeatureOfInterest.asNode(), siteVar);


            //where clause of the update Query
            //for graph organization
            WhereBuilder whereOrganization = new WhereBuilder()
                    .addWhere(siteVar, RDF.type.asNode(), ORG.Site)
                    .addWhere(siteVar, ORG.siteAddress, addressVar)
                    .addWhere(siteVar, RDF.type.asNode(), typeVar)
                    .addWhere(siteVar, RDFS.label.asNode(), nameVar);

            //add not exists filter to avoid duplicates
            WhereBuilder whereObservationCollection = new WhereBuilder();
            whereObservationCollection.addGraph(graphObservationCollection, subject, SOSA.hasFeatureOfInterest.asNode(), siteVar);
            whereOrganization.addFilter(new E_NotExists(whereObservationCollection.getHandlerBlock().getWhereHandler().getElement()));

            WhereBuilder insertWhereOrganization = new WhereBuilder().addGraph(graphOrganization, whereOrganization);
            WhereBuilder where = new WhereBuilder().addWhere(insertWhereOrganization);

            //Bind functions of the where clause
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            Expr exprNameFormated = exprFactory.replace(nameVar.asNode(), " ", "");
            where.addBind(exprNameFormated, nameFormatedVar);
            where.addBind(exprFactory.rand(), randomVar);

            Expr exprURI = exprFactory.iri(new ExprFactory().concat(
                    "http://opensilex.test/id/observationCollection/site/",
                    new ExprFactory().str(nameFormatedVar.asNode()),
                    "/",
                    exprFactory.replace(new ExprFactory().str(randomVar.asNode()), "0.", ""))
            );
            where.addBind(exprURI, observationCollectionURIVar);

            //Add the where clause with the graph
            query.addWhere(where);

            sparql.executeUpdateQuery(query);

        } catch (Exception e) {
            throw new OpensilexModuleUpdateException("error while adding observation collections. No changes was saved on the rdf database", e);
        }

        logger.info("Observation collections were added and saved in the rdf database");
    }

    /**
     * Select all site with observation collection.
     * <p>
     * The request to do that is :
     * <pre>
     *     PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
     *     PREFIX  org:  <http://www.w3.org/ns/org#>
     *     PREFIX  sosa: <http://www.w3.org/ns/sosa/>
     *     PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     *
     * SELECT *
     * WHERE {
     *   	    ?site a org:Site.
     *   		?s sosa:hasFeatureOfInterest ?site.
     * }
     * </pre>
     */
    private Map<URI, URI> getSiteObservationCollectionMap() throws SPARQLException {

        //Variables
        Var siteVar = makeVar(OrganizationModel.SITE_FIELD);
        Var uriVar = makeVar(LocationObservationModel.OBSERVATION_COLLECTION_FIELD);

        //Select
        SelectBuilder select = new SelectBuilder();

        //Add the where clause
        select.addWhere(uriVar, SOSA.hasFeatureOfInterest, siteVar);
        select.addWhere(siteVar, RDF.type.asNode(), ORG.Site);

        Stream<SPARQLResult> stream = sparql.executeSelectQueryAsStream(select);

        Map<URI, URI> resultMap = stream.collect(Collectors.toMap(
                result -> URI.create(result.getStringValue(OrganizationModel.SITE_FIELD)),
                result -> URI.create(result.getStringValue(LocationObservationModel.OBSERVATION_COLLECTION_FIELD))));

        stream.close();

        return resultMap;
    }

    /**
     * Second migration in mongo to migrate all sites stored in the "Geospatial" collection into a "location" collection with a new model.
     * <p>
     * New model in the "location" collection  :
     * <pre>
     *     {
     *       hasGeometry: true,
     *       location : {
     *           geometry: {
     *               type: "Point",
     *               coordinates : []
     *           }
     *       },
     *       collectionObservation : site observationCollection
     *       featureOfInterest: site uri
     *     }
     *
     * </pre>
     */
    private void mongoSitesFromGeospatialToLocationCollection(Map<URI, URI> sparqlSiteObservationCollectionMap) {
        MongoDatabase db = mongodb.getDatabase();
        MongoCollection<LocationObservationModel> locationCollection = db.getCollection(LocationObservationDAO.LOCATION_COLLECTION_NAME, LocationObservationModel.class);

        // 1- Get Sites from Geospatial Collection
        MongoCollection<GeospatialModel> geospatialCollection = db.getCollection(GeospatialDAO.GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
        List<GeospatialModel> geospatialSiteList = geospatialCollection.find(Filters.eq("rdfType", "http://www.w3.org/ns/org#Site")).into(new ArrayList<>());

        // 2- Check existing locations to avoid duplicates
        List<LocationObservationModel> locationsToUpdate = new ArrayList<>();
        List<LocationObservationModel> existingLocations = locationCollection.find(Filters.empty()).into(new ArrayList<>());
        if (!existingLocations.isEmpty()) {
            List<URI> existingLocationURI = existingLocations.stream().map(LocationObservationModel::getObservationCollection).collect(Collectors.toList());
            sparqlSiteObservationCollectionMap.forEach((feature, collection) -> {
                boolean match = existingLocationURI.stream().anyMatch(uri -> SPARQLDeserializers.compareURIs(uri, collection));
                if (match) {
                    // Exclude Location to the geospatial collection list
                    List<GeospatialModel> siteToExclude = geospatialSiteList.stream().filter(geospatialSite -> SPARQLDeserializers.compareURIs(geospatialSite.getUri(), feature)).collect(Collectors.toList());
                    if (siteToExclude.size() > 1) {
                        throw new NotAllowedException("Site can't have multiple geometry");
                    } else {
                        if(!siteToExclude.isEmpty()){
                            geospatialSiteList.remove(siteToExclude.get(0));
                        }
                    }


                    // Add to Locations to update
                    List<LocationObservationModel> locationToUpdate = existingLocations.stream().filter(location -> SPARQLDeserializers.compareURIs(location.getObservationCollection(), collection)).collect(Collectors.toList());
                    if (locationToUpdate.size() > 1) {
                        throw new NotAllowedException("Site can't have multiple geometry");
                    } else {
                        if (Objects.isNull(locationToUpdate.get(0).getFeatureOfInterest())) {
                            locationsToUpdate.add(locationToUpdate.get(0));
                        }
                    }
                }
            });
        }
        // 3- Format Sites from geospatial according to the new model
        List<LocationObservationModel> locationObservationModelList = geospatialSiteList.stream().map(geospatialSite -> {
            LocationObservationModel locationObservationModel = new LocationObservationModel();

            URI observationCollectionURI = sparqlSiteObservationCollectionMap.get(URI.create(SPARQLDeserializers.getExpandedURI(geospatialSite.getUri())));

            locationObservationModel.setObservationCollection(observationCollectionURI);
            locationObservationModel.setFeatureOfInterest(geospatialSite.getUri());
            locationObservationModel.setHasGeometry(true);

            LocationModel locationModel = new LocationModel();

            locationModel.setGeometry(geospatialSite.getGeometry());
            locationObservationModel.setLocation(locationModel);
            locationObservationModel.setUri(observationCollectionURI);

            return locationObservationModel;
        }).collect(Collectors.toList());

        // 4- Add featureOfInterest in location collection
        List<Bson> locationObservationModelUpdateList = new ArrayList<>();

        locationsToUpdate.forEach(locationToUpdate -> {
            for (Map.Entry<URI, URI> entry : sparqlSiteObservationCollectionMap.entrySet()) {
                if (SPARQLDeserializers.compareURIs(entry.getValue(), locationToUpdate.getObservationCollection())) {
                    Bson locationUpdated = Aggregates.addFields(new Field<>(LocationObservationModel.FEATURE_OF_INTEREST_FIELD, entry.getKey()));
                    locationObservationModelUpdateList.add(locationUpdated);
                }
            }
        });

        // 5- Insert/update Sites into new location Collection
        if (!locationObservationModelList.isEmpty()) {
            locationCollection.insertMany(locationObservationModelList);
        }

        if (!locationObservationModelUpdateList.isEmpty()) {
            Bson filter = Filters.and(
                    Filters.in(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, locationsToUpdate.stream().map(LocationObservationModel::getObservationCollection).collect(Collectors.toList())));
            locationCollection.updateMany(filter, locationObservationModelUpdateList);
        }

        logger.info("Locations were added and saved in the mongo database");
    }
}
