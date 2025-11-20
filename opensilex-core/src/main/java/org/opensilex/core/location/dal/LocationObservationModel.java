package org.opensilex.core.location.dal;

import org.bson.codecs.pojo.annotations.BsonId;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class LocationObservationModel extends MongoModel {

    public static final String OBSERVATION_COLLECTION_FIELD = "observationCollection";
    private URI observationCollection;
    public static final String FEATURE_OF_INTEREST_FIELD = "featureOfInterest";
    private URI featureOfInterest;
    public static final String START_DATE_FIELD = "startDate";
    private Instant startDate;
    public static final String END_DATE_FIELD = "endDate";
    private Instant endDate;
    public static final String HAS_GEOMETRY_FIELD = "hasGeometry";
    private boolean hasGeometry;
    private LocationModel location;
    public static final String LOCATION_TO_FIELD = "location.to";
    public static final String GEOMETRY_FIELD = "location.geometry";

    //TODO temporary link to a move, delete this if we ever get rid of moves
    private URI moveUri;


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

    public URI getFeatureOfInterest() {
        return featureOfInterest;
    }

    public void setFeatureOfInterest(URI featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
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

    public LocationModel getLocation() {
        return location;
    }

    public void setLocation(LocationModel location) {
        this.location = location;
    }

    public URI getMoveUri() {
        return moveUri;
    }

    public void setMoveUri(URI moveUri) {
        this.moveUri = moveUri;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
