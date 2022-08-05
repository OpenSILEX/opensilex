package org.opensilex.core.event.dal.move;

import com.mongodb.client.model.geojson.Point;

public class PositionModel {

    public static final String COORDINATES_FIELD = "coordinates";
    public static final String X_FIELD = "x";
    public static final String Y_FIELD = "y";
    public static final String Z_FIELD = "z";
    public static final String TEXTUAL_POSITION_FIELD = "textualPosition";

    private Point coordinates;
    private String x;
    private String y;
    private String z;
    private String textualPosition;

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
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
