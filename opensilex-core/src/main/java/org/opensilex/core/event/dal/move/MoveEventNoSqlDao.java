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

import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

public class MoveEventNoSqlDao extends MongoReadWriteDao<MoveEventNoSqlModel, MongoSearchFilter> {
    public static final String COLLECTION_NAME = "move";

    public MoveEventNoSqlDao(MongoDBServiceV2 mongodb) {
        super(mongodb, MoveEventNoSqlModel.class, COLLECTION_NAME, COLLECTION_NAME, false, false);
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
