package org.opensilex.core.geospatial.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.geojson.GeoJsonObject;

import java.net.URI;

@JsonPropertyOrder({"uri", "geometry"})
public class GeometryDTO {


    @Schema(description = "Object URI")
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
