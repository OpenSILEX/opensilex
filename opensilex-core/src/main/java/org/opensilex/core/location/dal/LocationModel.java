package org.opensilex.core.location.dal;

import com.mongodb.client.model.geojson.Geometry;

import java.net.URI;

public class LocationModel {
    public static final String GEOMETRY_FIELD = "location.geometry";
    public static final String COORDINATES_GEOMETRY_FIELD = "location.geometry.coordinates";

    private Geometry geometry;
    private URI to;
    private URI from;
    private String x;
    private String y;
    private String z;
    private String textualPosition;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }

    public String getTextualPosition() {
        return textualPosition;
    }

    public void setTextualPosition(String textualPosition) {
        this.textualPosition = textualPosition;
    }
}
