package org.opensilex.core.location.dal;

import com.mongodb.client.model.geojson.Geometry;

public class LocationModel {
    private Geometry geometry;
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
