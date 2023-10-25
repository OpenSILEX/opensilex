package org.opensilex.core.metrics.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.time.Instant;

public class GlobalSummarySearchFilter extends MongoSearchFilter {
    private String type;
    private Instant startInstant;
    private Instant endInstant;

    public String getType() {
        return type;
    }

    public GlobalSummarySearchFilter setType(String type) {
        this.type = type;
        return this;
    }

    public Instant getStartInstant() {
        return startInstant;
    }

    public GlobalSummarySearchFilter setStartInstant(Instant startInstant) {
        this.startInstant = startInstant;
        return this;
    }

    public Instant getEndInstant() {
        return endInstant;
    }

    public GlobalSummarySearchFilter setEndInstant(Instant endInstant) {
        this.endInstant = endInstant;
        return this;
    }
}
