package org.opensilex.core.position.dal;

import com.mongodb.client.model.geojson.Point;

public class PositionNoSqlModel {

    private Point point;
    private Integer x;
    private Integer y;
    private Integer z;
    private String description;

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point coordinates) {
        this.point = coordinates;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
