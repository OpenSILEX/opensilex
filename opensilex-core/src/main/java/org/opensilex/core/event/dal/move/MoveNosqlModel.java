package org.opensilex.core.event.dal.move;

import org.bson.codecs.pojo.annotations.BsonId;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.List;

public class MoveNosqlModel extends MongoModel {

    public final static String ID_FIELD = "_id";
    public final static String COORDINATES_FIELD = "targetPositions.position.coordinates";

    private List<TargetPositionModel> targetPositions;

    @Override
    @BsonId
    public URI getUri() {
        return super.getUri();
    }

    public List<TargetPositionModel> getTargetPositions() {
        return targetPositions;
    }

    public void setTargetPositions(List<TargetPositionModel> targetPositions) {
        this.targetPositions = targetPositions;
    }
}
