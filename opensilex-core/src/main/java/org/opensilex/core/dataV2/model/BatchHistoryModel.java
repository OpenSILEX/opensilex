package org.opensilex.core.dataV2.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.util.UUID;

/**
 * BatchHistory model used to track data insertion for batch import (csv,json)
 *
 * @author MKOURDI
 */
public class BatchHistoryModel extends MongoModel {
    String batchId;
    String userName;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
