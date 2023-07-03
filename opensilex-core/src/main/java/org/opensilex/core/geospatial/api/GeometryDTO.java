package org.opensilex.core.geospatial.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;

import java.net.URI;

@JsonPropertyOrder({"uri", "geometry"})
public class GeometryDTO {


    @ApiModelProperty(value = "Object URI")
    protected URI uri;

    protected GeoJsonObject geometry;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }
}
