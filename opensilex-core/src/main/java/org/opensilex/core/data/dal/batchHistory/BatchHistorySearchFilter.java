package org.opensilex.core.data.dal.batchHistory;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.time.Instant;

/**
 * @author MKourdi
 */
public class BatchHistorySearchFilter extends MongoSearchFilter {
    private URI batchUri;
    private String userName;
    private Instant startDate;
    private Instant endDate;

    public URI getBatchUri() {
        return batchUri;
    }

    public BatchHistorySearchFilter setBatchUri(URI batchUri) {
        this.batchUri = batchUri;
        return this;

    }

    public String getUserName() {
        return userName;
    }

    public BatchHistorySearchFilter setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public BatchHistorySearchFilter setStartDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public BatchHistorySearchFilter setEndDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }
}
