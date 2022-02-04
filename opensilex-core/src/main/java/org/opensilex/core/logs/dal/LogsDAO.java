/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.dal;

import com.mongodb.MongoWriteException;
import org.opensilex.nosql.mongodb.MongoDBService; 

/**
 *
 * @author charlero
 */
public class LogsDAO {

    protected final MongoDBService nosql;
    
    public static final String LOGS_COLLECTION_NAME = "log";
    public static final String LOG_PREFIX = "log";

    public LogsDAO(MongoDBService nosql) {
        this.nosql = nosql;
    }

    public LogModel create(LogModel instance)  throws Exception, MongoWriteException {
        nosql.create(instance, LogModel.class, LOGS_COLLECTION_NAME, LOG_PREFIX);
        return instance;
    }

}
