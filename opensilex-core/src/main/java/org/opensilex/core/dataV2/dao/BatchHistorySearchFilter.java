package org.opensilex.core.dataV2.dao;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

/**
 * @author MKourdi
 */
public class BatchHistorySearchFilter extends MongoSearchFilter {
    private String batchId;
    private String userName;

    public String getBatchId() {
        return batchId;
    }

    public BatchHistorySearchFilter setBatchId(String batchId) {
        this.batchId = batchId;
        return this;

    }

    public String getUserName() {
        return userName;
    }

    public BatchHistorySearchFilter setUserName(String userName) {
        this.userName = userName;
        return this;
    }
}
