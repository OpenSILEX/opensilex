package org.opensilex.core.data.dal.aggregations;

import java.net.URI;
import java.util.List;

public class DataTargetAggregateModel {
    List<URI> targets;

    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }
}
