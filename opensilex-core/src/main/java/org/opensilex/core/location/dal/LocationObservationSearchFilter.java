package org.opensilex.core.location.dal;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class LocationObservationSearchFilter extends MongoSearchFilter {
    private List<URI> observationCollectionList;
    private URI observationCollection;
    private Instant date;
    private boolean hasGeometry;
    private List<Document> locationObservationModelList;

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

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public boolean isHasGeometry() {
        return hasGeometry;
    }

    public void setHasGeometry(boolean hasGeometry) {
        this.hasGeometry = hasGeometry;
    }

    public List<Document> getLocationObservationModelList() {
        return locationObservationModelList;
    }

    public void setLocationObservationModelList(List<Document> locationObservationModelList) {
        this.locationObservationModelList = locationObservationModelList;
    }
}
