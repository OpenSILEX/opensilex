package org.opensilex.core.dataV2.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.util.UUID;

/**
 * BatchHistory model used to track data insertion for batch import (csv,json)
 *
 * @author MKOURDI
 */
public class BatchHistoryModel extends MongoModel {
    public static final String BATCH_ID_FIELD = "batchId";
    public static final String USERNAME = "username";

    private String batchId;
    private String username;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
