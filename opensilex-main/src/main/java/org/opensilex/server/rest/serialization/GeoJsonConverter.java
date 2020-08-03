package org.opensilex.server.rest.serialization;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.ModelImpl;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
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
@SwaggerDefinition
public class GeoJsonConverter implements MessageBodyReader<GeoJsonObject>, MessageBodyWriter<GeoJsonObject>, ReaderListener {
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }
    
    @Override
    public GeoJsonObject readFrom(Class<GeoJsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return ObjectMapperContextResolver.getObjectMapper().readValue(entityStream, type);
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }
    
    @Override
    public void writeTo(GeoJsonObject t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        ObjectMapperContextResolver.getObjectMapper().writeValue(entityStream, t);
    }
    
    @Override
    public void beforeScan(Reader reader, Swagger swagger) {
    }
    
    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        ModelImpl geoJsonModel = new ModelImpl();
        
        Map<PropertyBuilder.PropertyId, Object> typeProperties = new HashMap<>();
        typeProperties.put(PropertyBuilder.PropertyId.ENUM, Arrays.asList(
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
        typeProperties.put(PropertyBuilder.PropertyId.REQUIRED, true);
        
        Property typeProperty = PropertyBuilder.build("string", null, typeProperties);
        
        ArrayProperty bboxProperty = new ArrayProperty();
        bboxProperty.setItems(PropertyBuilder.build("number", "double", new HashMap<>()));
        
        ArrayProperty coordinatesProperty = new ArrayProperty();
        coordinatesProperty.setItems(PropertyBuilder.build("number", "double", new HashMap<>()));
        
        ObjectProperty geometryProperty = new ObjectProperty();
        geometryProperty.setType("GeoJsonObject");
        
        ObjectProperty crsProperty = new ObjectProperty();
        crsProperty.setType("Crs");
        
        Point geoJsonExample = new Point() {
            private final String type = "Point";
            
            public String getType() {
                return type;
            }
        };
        geoJsonExample.setBbox(null);
        geoJsonExample.setCrs(null);
        geoJsonExample.setCoordinates(new LngLatAlt(43.618316, 3.856912));
        
        geoJsonModel
                .name("GeoJsonObject")
                .type("object")
                .property("type", typeProperty)
                .required("type")
                .property("bbox", bboxProperty)
                .property("crs", crsProperty)
                .property("coordinates", coordinatesProperty)
                .property("geometry", geometryProperty)
                .example(geoJsonExample);
        
        if (swagger.getDefinitions() == null) {
            swagger.setDefinitions(new HashMap<>());
        }
        swagger.getDefinitions().put("GeoJsonObject", geoJsonModel);
    }
    
}
