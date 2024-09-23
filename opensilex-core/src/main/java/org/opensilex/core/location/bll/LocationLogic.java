package org.opensilex.core.location.bll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

public class LocationLogic {

    public static GeoJsonObject geometryToGeoJson(Geometry geometry) throws JsonProcessingException {
        Feature geo = new Feature();
        String geoJSON = geometry.toJson();
        GeoJsonObject geoJsonGeometry = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, GeoJsonObject.class);
        geo.setGeometry(geoJsonGeometry);

        return geo;
    }

    public static LocationModel buildLocationModel(Geometry geometry, String x, String y, String z, String textualPosition){
        LocationModel locationModel = new LocationModel();

        //build LocationModel
        if(geometry != null) {
            locationModel.setGeometry(geometry);
        }
        if(x != null) {
            locationModel.setX(x);
        }
        if(y != null) {
            locationModel.setY(y);
        }
        if(z != null) {
            locationModel.setZ(z);
        }
        if(textualPosition != null) {
            locationModel.setTextualPosition(textualPosition);
        }

        return locationModel;
    }
}
