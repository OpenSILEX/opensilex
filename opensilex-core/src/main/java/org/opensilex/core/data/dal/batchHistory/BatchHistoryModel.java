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
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";

    private URI documentUri;

    public void setDocumentUri(URI uri) {
        this.documentUri = uri;
    }

    public URI getDocumentUri() {
        return documentUri;
    }
    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
