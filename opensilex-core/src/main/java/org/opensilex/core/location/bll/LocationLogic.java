package org.opensilex.core.location.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import org.bson.BsonReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.GeometryCollection;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import java.util.List;
import java.util.Objects;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

public class LocationLogic {

    public static GeoJsonObject geometryToGeoJson(Geometry geometry) throws JsonProcessingException {
        Feature geo = new Feature();
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);
        geo.setGeometry(geoJsonGeometry);

        return geo;
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

    public static LocationModel buildLocationModel(Geometry geometry, String x, String y, String z, String textualPosition) {
        LocationModel locationModel = new LocationModel();

        //build LocationModel
        if (Objects.nonNull(geometry)) {
            locationModel.setGeometry(geometry);
        }
        if (Objects.nonNull(x)) {
            locationModel.setX(x);
        }
        if (Objects.nonNull(y)) {
            locationModel.setY(y);
        }
        if (Objects.nonNull(z)) {
            locationModel.setZ(z);
        }
        if (Objects.nonNull(textualPosition)) {
            locationModel.setTextualPosition(textualPosition);
        }

        return locationModel;
    }
}
