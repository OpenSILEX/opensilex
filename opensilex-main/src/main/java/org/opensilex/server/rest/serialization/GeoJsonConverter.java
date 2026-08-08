package org.opensilex.server.rest.serialization;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;

@Provider
@OpenAPIDefinition
public class GeoJsonConverter implements MessageBodyReader<GeoJsonObject>, MessageBodyWriter<GeoJsonObject> {
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return GeoJsonObject.class.isAssignableFrom(type);
    }
    
    @Override
    public GeoJsonObject readFrom(Class<GeoJsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return ObjectMapperContextResolver.getObjectMapper().readValue(entityStream, type);
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return GeoJsonObject.class.isAssignableFrom(type);
    }
    
    @Override
    public void writeTo(GeoJsonObject t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        ObjectMapperContextResolver.getObjectMapper().writeValue(entityStream, t);
    }

    public static void injectGeoJsonSchema(OpenAPI openAPI) {
        if (openAPI == null) {
            return;
        }
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        
        ObjectSchema geoJsonSchema = new ObjectSchema();
        geoJsonSchema.setName("GeoJsonObject");

        StringSchema typeSchema = new StringSchema();
        typeSchema.setEnum(Arrays.asList(
                "Feature",
                "Polygon",
                "MultiPolygon",
                "FeatureCollection",
                "Point",
                "MultiPoint",
                "MultiLineString",
                "LineString",
                "GeometryCollection"
        ));
        geoJsonSchema.addProperty("type", typeSchema);

        ArraySchema bboxSchema = new ArraySchema();
        bboxSchema.setItems(new NumberSchema());
        geoJsonSchema.addProperty("bbox", bboxSchema);

        ObjectSchema crsSchema = new ObjectSchema();
        crsSchema.setName("Crs");
        geoJsonSchema.addProperty("crs", crsSchema);

        ArraySchema coordinatesSchema = new ArraySchema();
        coordinatesSchema.setItems(new NumberSchema());
        geoJsonSchema.addProperty("coordinates", coordinatesSchema);

        ObjectSchema geometrySchema = new ObjectSchema();
        geometrySchema.setName("GeoJsonObject");
        geoJsonSchema.addProperty("geometry", geometrySchema);

        Point geoJsonExample = new Point() {
            private final String type = "Point";
            
            public String getType() {
                return type;
            }
        };
        geoJsonExample.setBbox(null);
        geoJsonExample.setCrs(null);
        geoJsonExample.setCoordinates(new LngLatAlt(43.618316, 3.856912));
        
        geoJsonSchema.setExample(geoJsonExample);

        openAPI.getComponents().addSchemas("GeoJsonObject", geoJsonSchema);
    }
}
