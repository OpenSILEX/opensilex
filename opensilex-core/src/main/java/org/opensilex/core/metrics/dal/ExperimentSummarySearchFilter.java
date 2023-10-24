package org.opensilex.core.metrics.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class ExperimentSummarySearchFilter extends MongoSearchFilter {

    private List<URI> experiments;
    private Instant start;
    private Instant end;

    public List<URI> getExperiments() {
        return experiments;
    }

    public ExperimentSummarySearchFilter setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public Instant getStart() {
        return start;
    }

    public ExperimentSummarySearchFilter setStart(Instant start) {
        this.start = start;
        return this;
    }

    public Instant getEnd() {
        return end;
    }

    public ExperimentSummarySearchFilter setEnd(Instant end) {
        this.end = end;
        return this;
    }
}
