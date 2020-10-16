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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * Geospatial DAO
 *
 * @author Jean Philippe VERT
 */
public class GeospatialDAO {

    final MongoCollection<GeospatialModel> geometryCollection;

    public GeospatialDAO(NoSQLService nosql) {
        MongoDatabase db = nosql.getMongoDBClient().getDatabase("opensilex");
        String nameCollection = "Geospatial";
        geometryCollection = db.getCollection(nameCollection, GeospatialModel.class);
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
        Geometry geometry = geocodec.decode(jsonReader, DecoderContext.builder().build());

        return geometry;
    }

    public static GeoJsonObject geometryToGeoJson(Geometry geometry) throws JsonProcessingException {
        Feature geo = new Feature();
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);
        geo.setGeometry(geoJsonGeometry);

        return geo;
    }

    public GeospatialModel create(GeospatialModel instanceGeospatial) {
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

    public GeospatialModel update(GeospatialModel geospatial, URI uri, URI graph) {
        if (geospatial.getGeometry() != null) {
            Document filter = getFilter(uri, graph);

            // the verification of the existence of the URI is done by mongoDB thanks to the uri_1_graph_1 index.
            return geometryCollection.findOneAndReplace(filter, geospatial);
        }
        return geospatial;
    }

    private Document getFilter(URI uri, URI graph) {
        if (graph != null)
            return new Document("uri", SPARQLDeserializers.getExpandedURI(uri)).append("graph", SPARQLDeserializers.getExpandedURI(graph));
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

    // All of the following methods required the presence of a 2dsphere or 2s index to support geospatial queries.
    public ListWithPagination<GeospatialModel> searchIntersects(URI type, Geometry geometry, Integer page, Integer pageSize) {
        Document filter = null;
        if (type != null) {
            filter = new Document("type", type);
        }

        // searches all documents containing a field with geospatial data that intersects the specified shape + filtering by type.
        FindIterable<GeospatialModel> geospatialFindIterable = geometryCollection.find(Filters.geoIntersects("geometry", geometry)).filter(filter);

        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchIntersects(GeospatialModel geometryI, Integer page, Integer pageSize) {
        return searchIntersects(geometryI.getType(), geometryI.getGeometry(), page, pageSize);
    }

    public ListWithPagination<GeospatialModel> searchWithin(URI type, Geometry geometry, Integer page, Integer pageSize) {
        Document filter = null;
        if (type != null) {
            filter = new Document("type", type);
        }

        // search in all documents containing a field with geospatial data that is contained in the past form + filtering by type.
        FindIterable<GeospatialModel> geospatialFindIterable = geometryCollection.find(Filters.geoWithin("geometry", geometry)).filter(filter);

        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchWithin(GeospatialModel geometryI, Integer page, Integer pageSize) {
        return searchWithin(geometryI.getType(), geometryI.getGeometry(), page, pageSize);
    }

    public ListWithPagination<GeospatialModel> searchNear(URI type, Point geometry, Double maxDistanceMeters, Double minDistanceMeters, Integer page, Integer pageSize) {
        Document filter = null;
        if (type != null) {
            filter = new Document("type", type);
        }

        // searches for documents between the range of distance around the point + filtering by type.
        FindIterable<GeospatialModel> geospatialFindIterable = geometryCollection.find(Filters.nearSphere("geometry", geometry, maxDistanceMeters, minDistanceMeters)).filter(filter);
        return getGeospatialModelListWithPagination(page, pageSize, geospatialFindIterable);
    }

    public ListWithPagination<GeospatialModel> searchNear(GeospatialModel geometryI, Double maxDistanceMeters, Double minDistanceMeters, Integer page, Integer pageSize) {
        return searchNear(geometryI.getType(), (Point) geometryI.getGeometry(), maxDistanceMeters, minDistanceMeters, page, pageSize);
    }
}
