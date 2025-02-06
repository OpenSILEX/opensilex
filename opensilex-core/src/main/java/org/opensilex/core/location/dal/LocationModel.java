package org.opensilex.core.location.dal;

import com.mongodb.client.model.geojson.Geometry;

import java.net.URI;

public class LocationModel {
    public static final String GEOMETRY_FIELD = "location.geometry";

    private Geometry geometry;
    private URI to;
    public static final String TO_FIELD = "to";
    private URI from;
    public static final String FROM_FIELD = "from";
    private String x;
    private String y;
    private String z;
    private String textualPosition;

    public LocationModel createCopy() {
        LocationModel copy = new LocationModel();
        copy.x = this.x;
        copy.y = this.y;
        copy.z = this.z;
        copy.textualPosition = this.textualPosition;
        copy.to = this.to;
        copy.from = this.from;
        copy.geometry = this.geometry;
        return copy;
    }

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
