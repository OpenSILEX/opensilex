package org.opensilex.core.event.dal.move;

import com.mongodb.client.model.geojson.Point;

public class PositionModel {

    public static final String COORDINATES_FIELD = "coordinates";
    public static final String X_FIELD = "x";
    public static final String Y_FIELD = "y";
    public static final String Z_FIELD = "z";
    public static final String TEXTUAL_POSITION_FIELD = "textualPosition";

    private Point coordinates;
    private Integer x;
    private Integer y;
    private Integer z;
    private String textualPosition;

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    public String getTextualPosition() {
        return textualPosition;
    }

    public void setTextualPosition(String textualPosition) {
        this.textualPosition = textualPosition;
    }
}
