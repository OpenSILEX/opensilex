package org.opensilex.core.data.dal.batchHistory;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import java.time.Instant;

/**
 * @author MKourdi
 */
public class BatchHistorySearchFilter extends MongoSearchFilter {
    private Instant startDate;
    private Instant endDate;


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
