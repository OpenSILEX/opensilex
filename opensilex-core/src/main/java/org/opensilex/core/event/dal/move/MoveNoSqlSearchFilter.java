package org.opensilex.core.event.dal.move;

import com.mongodb.client.model.geojson.Geometry;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

public class MoveNoSqlSearchFilter extends MongoSearchFilter {
    public MoveNoSqlSearchFilter() {
        super();
    }

    Geometry geometry;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
