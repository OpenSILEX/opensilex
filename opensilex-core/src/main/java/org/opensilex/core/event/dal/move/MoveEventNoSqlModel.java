package org.opensilex.core.event.dal.move;

import org.bson.codecs.pojo.annotations.BsonId;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.List;

public class MoveEventNoSqlModel extends MongoModel {

    public final static String ID_FIELD = "_id";

    private List<ConcernedItemPositionModel> itemPositions;

    @Override
    @BsonId
    public URI getUri() {
        return super.getUri();
    }

    public List<ConcernedItemPositionModel> getItemPositions() {
        return itemPositions;
    }

    public void setItemPositions(List<ConcernedItemPositionModel> itemPositions) {
        this.itemPositions = itemPositions;
    }
}
