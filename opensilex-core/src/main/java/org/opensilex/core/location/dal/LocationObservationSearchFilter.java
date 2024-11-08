package org.opensilex.core.location.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class LocationObservationSearchFilter extends MongoSearchFilter {
    private List<URI> observationCollectionList;
    private URI observationCollection;
    private Instant startDate;
    private Instant endDate;
    private boolean hasGeometry;

    public List<URI> getObservationCollectionList() {
        return observationCollectionList;
    }

    public void setObservationCollectionList(List<URI> observationCollectionList) {
        this.observationCollectionList = observationCollectionList;
    }

    public URI getObservationCollection() {
        return observationCollection;
    }

    public void setObservationCollection(URI observationCollection) {
        this.observationCollection = observationCollection;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public boolean isHasGeometry() {
        return hasGeometry;
    }

    public void setHasGeometry(boolean hasGeometry) {
        this.hasGeometry = hasGeometry;
    }
}
