package org.opensilex.nosql.mongodb;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.MongoInsertOptions;

/**
 * @author rcolin
 */
public class DefaultMongoInserter implements MongoInserter{

    @Override
    public <T extends MongoModel> void create(MongoInsertOptions<T> options) throws Exception {

        if(! options.commitTransaction()){
            options.getCollection().insertMany(options.getSession(),options.getModels());
            return;
        }

        ClientSession session = options.getSession();
        try {
            session.startTransaction();
            options.getCollection().insertMany(session,options.getModels());

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
