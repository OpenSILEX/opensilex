package org.opensilex.core.location.dal;

import org.bson.codecs.pojo.annotations.BsonId;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class LocationObservationModel extends MongoModel {

    public static final String OBSERVATION_COLLECTION_FIELD = "observationCollection";
    private URI observationCollection;
    public static final String DATE_FIELD = "time";
    private Instant date;
    public static final String HAS_GEOMETRY_FIELD = "hasGeometry";
    private boolean hasGeometry;
    private LocationModel location;

    @Override
    @BsonId
    public URI getUri() {
        return super.getUri();
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

    public LocationModel getLocation() {
        return location;
    }

    public void setLocation(LocationModel location) {
        this.location = location;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
