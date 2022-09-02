package org.opensilex.nosql.insert;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * @author rcolin
 */
public class DefaultMongoInserter extends AbstractMongoInserter {

    public DefaultMongoInserter(MongoClient mongoClient, MongoDBConfig config) {
        super(mongoClient, config);
    }

    @Override
    protected <T extends MongoModel> void insertWithoutTransaction(MongoInsertOptions<T> insertOptions) {
        insertOptions.getCollection().insertMany(insertOptions.getSession(), insertOptions.getModels());
    }

    @Override
    protected <T extends MongoModel> void insertWithTransaction(MongoInsertOptions<T> insertOptions) {
        ClientSession session = insertOptions.getSession();
        try {
            session.startTransaction();
            insertOptions.getCollection().insertMany(session, insertOptions.getModels());

            if(session.hasActiveTransaction()){
                session.commitTransaction();
            }

        }catch (Exception e){
            if(session.hasActiveTransaction()){
                session.abortTransaction();
            }
            throw e;
        }finally {
            session.close();
        }
    }

}
