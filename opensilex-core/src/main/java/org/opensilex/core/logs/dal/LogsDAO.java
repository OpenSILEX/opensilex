/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.dal;

import com.mongodb.MongoWriteException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

/**
 *
 * @author charlero
 */
public class LogsDAO extends MongoReadWriteDao<LogModel, MongoSearchFilter> {


    public static final String LOGS_COLLECTION_NAME = "log";
    public static final String LOG_PREFIX = "log";

    public LogsDAO(MongoDBService mongodb) {
        super(mongodb, LogModel.class, LOGS_COLLECTION_NAME, LOG_PREFIX);
    }

}
