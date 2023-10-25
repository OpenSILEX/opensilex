package org.opensilex.core.metrics.dal;


import java.net.URI;
import java.util.Collection;

public class ExperimentSummarySearchFilter extends GlobalSummarySearchFilter {

    private Collection<URI> experiments;

    public Collection<URI> getExperiments() {
        return experiments;
    }

    public ExperimentSummarySearchFilter setExperiments(Collection<URI> experiments) {
        this.experiments = experiments;
        return this;
    }
}
