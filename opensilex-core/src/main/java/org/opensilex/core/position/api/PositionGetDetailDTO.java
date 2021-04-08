package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.Point;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

public class PositionGetDetailDTO {

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

    public PositionGetDetailDTO(PositionModel model) throws JsonProcessingException {

        com.mongodb.client.model.geojson.Point coordinatesModel = model.getPoint();
        if(coordinatesModel != null){
            String geoJSON = coordinatesModel.toJson();
            point  = ObjectMapperContextResolver.getObjectMapper().readValue(geoJSON, Point.class);
        }

        x = model.getX();
        y = model.getY();
        z = model.getZ();
        description = model.getDescription();
    }

    public PositionGetDetailDTO() {
    }

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
}
