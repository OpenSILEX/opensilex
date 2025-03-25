package org.opensilex.core.data.dal.batchHistory;

import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.util.UUID;

/**
 * BatchHistory model used to track data insertion for batch import (csv,json)
 *
 * @author MKOURDI
 */
public class BatchHistoryModel extends MongoModel {
    public static final String BATCH_ID_FIELD = "batchId";
    public static final String USERNAME = "username";
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";

    private String batchId;
    private String username;
    private URI setDocumentUri;

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

    public void setDocumentUri(URI uri) {
        this.setDocumentUri = uri;
    }

    public URI getDocumentUri() {
        return setDocumentUri;
    }
    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
