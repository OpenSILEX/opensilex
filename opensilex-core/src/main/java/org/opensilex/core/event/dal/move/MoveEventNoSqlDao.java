/*
 * *****************************************************************************
 *                         MoveEventNoSqlDao.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 20/06/2024 15:28
 * Contact: maximilian.hart@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.event.dal.move;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.List;

import static com.mongodb.client.model.Filters.and;

public class MoveEventNoSqlDao extends MongoReadWriteDao<MoveNosqlModel, MoveNoSqlSearchFilter> {
    public static final String COLLECTION_NAME = "move";
    public static final String POSITION_ARRAY_FIELD = "targetPositions";
    public static final String TARGET_FIELD = "target";

    public MoveEventNoSqlDao(MongoDBServiceV2 mongodb) {
        super(mongodb, MoveNosqlModel.class, COLLECTION_NAME, COLLECTION_NAME, false, false);
    }

    @Override
    public List<Bson> getBsonFilters(MoveNoSqlSearchFilter filter) {
        List<Bson> result = super.getBsonFilters(filter);
        if(filter.geometry != null){
            result.add(Filters.exists(MoveNosqlModel.COORDINATES_FIELD, true));
            result.add(Filters.geoWithin(MoveNosqlModel.COORDINATES_FIELD, filter.geometry));
        }

        return result;
    }

    /**
     * Override this to be id field as Moves only have an id which is equal to a uri, unlike other places in the application where there is a uri field.
     * This will allow get(URI) to work.
     */
    @Override
    public String idField() {
        return MongoModel.MONGO_ID_FIELD;
    }
}
