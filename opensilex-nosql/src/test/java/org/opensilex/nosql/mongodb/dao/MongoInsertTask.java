package org.opensilex.nosql.mongodb.dao;

import com.mongodb.client.ClientSession;
import com.mongodb.client.result.InsertManyResult;
import org.opensilex.nosql.mongodb.MongoModel;

import java.util.List;
import java.util.concurrent.Callable;

public class MongoInsertTask<T extends MongoModel, F extends MongoSearchFilter> implements Callable<InsertManyResult> {

    private final MongoReadWriteDao<T, F> dao;
    private final List<T> models;

    public MongoInsertTask(MongoReadWriteDao<T, F> dao, List<T> models) {
        this.dao = dao;
        this.models = models;
    }

    @Override
    public InsertManyResult call() throws Exception {
        // don't provide session, the  MongoDBServiceV2 will auto-create it
        return dao.create(models, null);
    }
}
