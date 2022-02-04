/*
 * *******************************************************************************
 *                     GeospatialDAO.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 24, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */
package org.opensilex.core.geospatial.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import org.bson.BsonReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.GeometryCollection;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * Geospatial DAO
 *
 * @author Jean Philippe VERT
 */
public class GeospatialDAO {

    private final MongoCollection<GeospatialModel> geometryCollection;

    public static final String GEOSPATIAL_COLLECTION_NAME = "geospatial";

    public GeospatialDAO(MongoDBService nosql) {
        MongoDatabase db = nosql.getDatabase();
        geometryCollection = db.getCollection(GEOSPATIAL_COLLECTION_NAME, GeospatialModel.class);
    }

    public static Geometry geoJsonToGeometry(GeoJsonObject geo) throws JsonProcessingException {

        if (geo instanceof Feature) {
            Feature feature = (Feature) geo;
            geo = feature.getGeometry();
        } else if (geo instanceof FeatureCollection) {
            FeatureCollection featureCol = (FeatureCollection) geo;
            GeometryCollection geoCol = new GeometryCollection();
            List<Feature> features = featureCol.getFeatures();
            for (Feature feature : features) {
                geoCol.add(feature.getGeometry());
            }
            geo = geoCol;
        }

        String geoJSON = ObjectMapperContextResolver.getObjectMapper().writeValueAsString(geo);
        BsonReader jsonReader = new JsonReader(geoJSON);

        CodecRegistry geoJsonCodecRegistry = fromProviders(new GeoJsonCodecProvider());
        Codec<Geometry> geocodec = geoJsonCodecRegistry.get(Geometry.class);

        return geocodec.decode(jsonReader, DecoderContext.builder().build());
    }

    public static Geometry wktToGeometry(String wktString) throws ParseException, JsonProcessingException {
        final WKTReader reader = new WKTReader();
        org.locationtech.jts.geom.Geometry geom = reader.read(wktString);
        final GeoJsonWriter writer = new GeoJsonWriter();
        writer.setEncodeCRS(false);
        String geoJSON = writer.write(geom);
        return geoJsonToGeometry(ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class));
    }

    public static GeoJsonObject geometryToGeoJson(Geometry geometry) throws JsonProcessingException {
        Feature geo = new Feature();
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);
        geo.setGeometry(geoJsonGeometry);

        return geo;
    }

    public static String geometryToWkt(Geometry geometry) throws JsonProcessingException, ParseException {
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);
        final GeoJsonReader reader = new GeoJsonReader();
        String geoJSONString = ObjectMapperContextResolver.getObjectMapper().writeValueAsString(geoJsonGeometry);
        org.locationtech.jts.geom.Geometry geom = reader.read(geoJSONString);
        WKTWriter writer = new WKTWriter();
        return writer.write(geom);
    }

    public GeospatialModel create(GeospatialModel instanceGeospatial) throws MongoWriteException {
        if (instanceGeospatial.getGeometry() != null) {
            // the verification of the existence of the URI is done by mongoDB thanks to the uri_1_graph_1 index.
            addIndex();
            geometryCollection.insertOne(instanceGeospatial);
        }

        return instanceGeospatial;
    }

    public GeospatialModel getGeometryByURI(URI uri, URI graph) {
        Document filter = getFilter(uri, graph);

        return geometryCollection.find(filter).first();
    }

    private void addIndex() {
        // db.Geospatial.createIndex( { uri: 1, graph: 1}, { unique: true } )
        Document indexURI = new Document("uri", 1).append("graph", 1);
        // db.Geospatial.createIndex( { "geometry" : "2dsphere" } )
        Document indexGeometry = new Document("geometry", "2dsphere");

        geometryCollection.createIndex(indexURI, new IndexOptions().unique(true));
        geometryCollection.createIndex(indexGeometry);
    }

    public GeospatialModel update(GeospatialModel geospatial, URI uri, URI graph) throws MongoWriteException {
        if (geospatial.getGeometry() != null) {
            Document filter = getFilter(uri, graph);

            // the verification of the existence of the URI is done by mongoDB thanks to the uri_1_graph_1 index.
            return geometryCollection.findOneAndReplace(filter, geospatial, new FindOneAndReplaceOptions().upsert(true));
        }
        return geospatial;
    }

    private Document getFilter(URI uri, URI graph) {
        if (graph != null) {
            return new Document("uri", SPARQLDeserializers.getExpandedURI(uri)).append("graph", SPARQLDeserializers.getExpandedURI(graph));
        }
        return new Document("uri", SPARQLDeserializers.getExpandedURI(uri));
    }

    public void delete(URI uri, URI graph) {
        Document filter = getFilter(uri, graph);

        geometryCollection.deleteOne(filter);
    }

    private ListWithPagination<GeospatialModel> getGeospatialModelListWithPagination(Integer page, Integer pageSize, FindIterable<GeospatialModel> geospatialFindIterable) {
        List<GeospatialModel> geospatialListWithPagination = new ArrayList<>();
        int total = 0;

        for (GeospatialModel results : geospatialFindIterable) {
            geospatialListWithPagination.add(results);
            total++;
        }

        return new ListWithPagination<>(geospatialListWithPagination, page, pageSize, total);
    }

    public HashMap<String, Geometry> getGeometryByUris(URI experimentURI, List<URI> objectsURI) {
        Document filter = new Document();
        if (experimentURI != null) {
            filter = new Document("graph", SPARQLDeserializers.getExpandedURI(experimentURI));
        }

        FindIterable<GeospatialModel> modelList = geometryCollection.find(filter).filter(Filters.in("uri", objectsURI));

        return createGeometryMap(modelList);
    }

    private HashMap<String, Geometry> createGeometryMap(FindIterable<GeospatialModel> modelList) {
        HashMap<String, Geometry> mapGeo = new HashMap<>();

        for (GeospatialModel geospatialModel : modelList) {
            mapGeo.put(URIDeserializer.formatURIAsStr(geospatialModel.getUri().toString()), geospatialModel.getGeometry());
        }

        return mapGeo;
    }

    public HashMap<String, Geometry> getGeometryByGraph(URI experimentURI) {
        if (experimentURI != null) {
            FindIterable<GeospatialModel> modelList = geometryCollection.find(new Document("graph", SPARQLDeserializers.getExpandedURI(experimentURI)));

            return createGeometryMap(modelList);
        } else {
            return null;
        }
    }

    public FindIterable<GeospatialModel> getGeometryByGraphList(URI experimentURI) {
        if (experimentURI != null) {
            return geometryCollection.find(new Document("graph", SPARQLDeserializers.getExpandedURI(experimentURI)));
        } else {
            return null;
        }
    }

    // All of the following methods required the presence of a 2dsphere or 2s index to support geospatial queries.
    public FindIterable<GeospatialModel> searchIntersectsArea(Geometry geometry, UserModel currentUser, SPARQLService sparql) throws Exception {
        if (geometry != null) {
            OntologyDAO ontologyDAO = new OntologyDAO(sparql);

            SPARQLTreeListModel<ClassModel> tree = ontologyDAO.searchSubClasses(new URI(Oeso.Area.getURI()), ClassModel.class, null,
                    currentUser.getLanguage(),
                    true,
                    null);
            List<ResourceTreeDTO> resourceTreeDTOS = ResourceTreeDTO.fromResourceTree(tree);
            List<String> ontologyAreaURI = new LinkedList<>();

            resourceTreeDTOS.forEach(resourceTreeDTO -> {
                ontologyAreaURI.add(SPARQLDeserializers.getExpandedURI(resourceTreeDTO.getUri().toString()));
                List<ResourceTreeDTO> childrenList = resourceTreeDTO.getChildren();
                if (childrenList != null)
                    extractedChildren(ontologyAreaURI, childrenList);
            });

            return geometryCollection.find(and(Filters.geoIntersects("geometry", geometry), Filters.in("rdfType", ontologyAreaURI)));
        } else {
            return null;
        }
    }

    private void extractedChildren(List<String> ontologyAreaURI, List<ResourceTreeDTO> children) {
        children.forEach(child -> {
            String expandedURI = SPARQLDeserializers.getExpandedURI(child.getUri().toString());
            ontologyAreaURI.add(expandedURI);
            List<ResourceTreeDTO> childrenList = child.getChildren();
            if (childrenList != null) {
                extractedChildren(ontologyAreaURI, childrenList);
            }
        });
    }

    public ListWithPagination<GeospatialModel> searchIntersects(URI rdfType, Geometry geometry, Integer page, Integer pageSize) {
        Document filter = null;
        if (rdfType != null) {
            filter = new Document("rdfType", rdfType);
        }

        // searches all documents containing a field with geospatial data that intersects the specified shape + filtering by rdfType.
        FindIterable<GeospatialModel> geospatialFindIterable;
        if (filter != null) {
            geospatialFindIterable = geometryCollection.find(and(Filters.geoIntersects("geometry", geometry), filter));
        } else {
            geospatialFindIterable = geometryCollection.find(Filters.geoIntersects("geometry", geometry));
        }

        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchIntersects(GeospatialModel geometryI, Integer page, Integer pageSize) {
        return searchIntersects(geometryI.getRdfType(), geometryI.getGeometry(), page, pageSize);
    }

    public ListWithPagination<GeospatialModel> searchWithin(URI rdfType, Geometry geometry, Integer page, Integer pageSize) {
        Document filter = null;
        if (rdfType != null) {
            filter = new Document("rdfType", rdfType);
        }

        // search in all documents containing a field with geospatial data that is contained in the past form + filtering by rdfType.
        FindIterable<GeospatialModel> geospatialFindIterable;
        if (filter != null) {
            geospatialFindIterable = geometryCollection.find(and(Filters.geoWithin("geometry", geometry), filter));
        } else {
            geospatialFindIterable = geometryCollection.find(Filters.geoWithin("geometry", geometry));
        }

        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchWithin(GeospatialModel geometryI, Integer page, Integer pageSize) {
        return searchWithin(geometryI.getRdfType(), geometryI.getGeometry(), page, pageSize);
    }

    public ListWithPagination<GeospatialModel> searchNear(URI rdfType, Point geometry, Double maxDistanceMeters, Double minDistanceMeters, Integer page, Integer pageSize) {
        Document filter = null;
        if (rdfType != null) {
            filter = new Document("rdfType", rdfType);
        }

        // searches for documents between the range of distance around the point + filtering by rdfType.
        FindIterable<GeospatialModel> geospatialFindIterable;
        if (filter != null) {
            geospatialFindIterable = geometryCollection.find(and(Filters.nearSphere("geometry", geometry, maxDistanceMeters, minDistanceMeters), filter));
        } else {
            geospatialFindIterable = geometryCollection.find(Filters.nearSphere("geometry", geometry, maxDistanceMeters, minDistanceMeters));
        }

        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchNear(GeospatialModel geometryI, Double maxDistanceMeters, Double minDistanceMeters, Integer page, Integer pageSize) {
        return searchNear(geometryI.getRdfType(), (Point) geometryI.getGeometry(), maxDistanceMeters, minDistanceMeters, page, pageSize);
    }

    public void createAll(List<GeospatialModel> geospatialModels) {
        addIndex();
        geometryCollection.insertMany(geospatialModels);
    }
}