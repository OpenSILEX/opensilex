package org.opensilex.nosql.mongodb.dao;

 import com.mongodb.client.result.InsertManyResult;
import org.opensilex.nosql.mongodb.MongoModel;
 import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

 import java.util.List;
import java.util.concurrent.Callable;

public class MongoInsertTask<T extends MongoModel, F extends MongoSearchFilter> implements Callable<InsertManyResult> {

    private final MongoDBServiceV2 mongoDBServiceV2;
    private final MongoReadWriteDao<T, F> dao;
    private final List<T> models;
    private final boolean generateSession;

    public MongoInsertTask(MongoDBServiceV2 mongoDBServiceV2, MongoReadWriteDao<T, F> dao, List<T> models, boolean generateSession) {
        this.mongoDBServiceV2 = mongoDBServiceV2;
        this.dao = dao;
        this.models = models;
        this.generateSession = generateSession;
    }

    @Override
    public InsertManyResult call() throws Exception {

        if(generateSession){
            return  mongoDBServiceV2.computeTransaction((session) -> dao.create(models, null));
        }
        // don't provide session, the  MongoDBServiceV2 will auto-create it
        return dao.create(models, null);
    }
}
