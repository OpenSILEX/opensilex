package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.Point;
import org.opensilex.core.event.dal.move.PositionModel;

public class PositionCreationDTO {


    private com.mongodb.client.model.geojson.Point pointModel;

    @JsonProperty("point")
    private Point point;

    @JsonProperty("x")
    private Integer x;

    @JsonProperty("y")
    private Integer y;

    @JsonProperty("z")
    private Integer z;

    @JsonProperty("text")
    private String description;

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    @ApiModelProperty(example = "35")
    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    @ApiModelProperty(example = "76")
    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    @ApiModelProperty(example = "8611")
    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }

    @ApiModelProperty(example = "Near the greenhouse door")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPointModel(com.mongodb.client.model.geojson.Point pointModel) {
        this.pointModel = pointModel;
    }

    public PositionModel toModel() {

        PositionModel positionNoSqlModel = new PositionModel();

        if (point != null) {
            positionNoSqlModel.setCoordinates(pointModel);
        }
        positionNoSqlModel.setX(x);
        positionNoSqlModel.setY(y);
        positionNoSqlModel.setZ(z);
        positionNoSqlModel.setTextualPosition(description);

        return positionNoSqlModel;
    }

}
