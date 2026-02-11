package org.opensilex.core.location.dal;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LocationObservationDAO extends MongoReadWriteDao<LocationObservationModel, LocationObservationSearchFilter> {
    public static final String LOCATION_COLLECTION_NAME = "location";

    //#region constructor
    public LocationObservationDAO(MongoDBServiceV2 mongodb) {
        super(mongodb, LocationObservationModel.class, LOCATION_COLLECTION_NAME, LOCATION_COLLECTION_NAME);
    }
    //#endregion

    //#region public

    /**
     *
     * @param searchQuery the Search filter
     * @return filter list
     */
    @Override
    public List<Bson> getBsonFilters(LocationObservationSearchFilter searchQuery) {
        List<Bson> filters = new ArrayList<>();

        //Collection URI
        if (Objects.nonNull(searchQuery.getObservationCollection())) {
            filters.add(Filters.eq(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, searchQuery.getObservationCollection()));
        }
        //Feature of interest URI
        if (Objects.nonNull(searchQuery.getFeatureOfInterest())) {
            filters.add(Filters.eq(LocationObservationModel.FEATURE_OF_INTEREST_FIELD, searchQuery.getFeatureOfInterest()));
        }
        //Move URIs
        if (!CollectionUtils.isEmpty(searchQuery.getMoveUris())) {
            filters.add(Filters.in(LocationObservationModel.MOVE_URI_FIELD, searchQuery.getMoveUris()));
        }
        //Collection List
        if (Objects.nonNull(searchQuery.getObservationCollectionList())  && !searchQuery.getObservationCollectionList().isEmpty()) {
            filters.add(Filters.in(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, searchQuery.getObservationCollectionList()));
        }

        appendHasGeometryFilters(filters,searchQuery);
        appendDateFilters(filters,searchQuery);

        if (searchQuery.getIntersection() != null) {
            filters.add(Filters.exists(LocationModel.GEOMETRY_FIELD, true));
            filters.add(Filters.geoWithin(LocationModel.GEOMETRY_FIELD, searchQuery.getIntersection()));
        }

        if(Objects.nonNull(searchQuery.getTo())){
            filters.add(Filters.eq(LocationObservationModel.LOCATION_TO_FIELD, searchQuery.getTo()));
        }

        return filters;
    }

    //#endregion

    /**
     * Filters are added if we want to get object locations for display on a map:
     * - get only locations with "geometry" field filled in
     * - locations before the end-date filter (and after start-date filter) - interval or instant
     * - for locations from address (without date) - sites and facilities with address only - get the unique location
     *
     */
    private void appendHasGeometryFilters(List<Bson> filters, LocationObservationSearchFilter searchQuery){
        //Has Geometry - if equal at "true", get only objects that can be displayed on a map (with a geometry)
        if (searchQuery.isHasGeometry()) {
            filters.add(Filters.eq(LocationObservationModel.HAS_GEOMETRY_FIELD, searchQuery.isHasGeometry()));

            //No end Date - case date no exists = address
            Bson filterNoEndDate = Filters.exists(LocationObservationModel.END_DATE_FIELD, false);
            //End Date
            if (Objects.nonNull(searchQuery.getEndDate())) {
                Bson filterOnEndDate = Filters.and(
                        Filters.exists(LocationObservationModel.END_DATE_FIELD, true),
                        Filters.lte(LocationObservationModel.END_DATE_FIELD, searchQuery.getEndDate()));

                Bson filterOnStartDate = Filters.and(
                        Filters.exists(LocationObservationModel.START_DATE_FIELD, true),
                        Filters.lte(LocationObservationModel.START_DATE_FIELD, searchQuery.getEndDate()));

                Bson filtersEnDate = Filters.or(filterOnEndDate, filterOnStartDate);

                filters.add(Filters.or(filtersEnDate, filterNoEndDate));
            }
            // Start Date
            if (Objects.nonNull(searchQuery.getStartDate())) {
                Bson filterStartDate = Filters.and(
                        Filters.exists(LocationObservationModel.END_DATE_FIELD, true),
                        Filters.gte(LocationObservationModel.END_DATE_FIELD, searchQuery.getStartDate()));

                filters.add(Filters.or(filterStartDate, filterNoEndDate));
            }
        }
    }

    /**
     * get all locations between date filters
     */
    private void appendDateFilters(List<Bson> filters, LocationObservationSearchFilter searchQuery){
        if (!searchQuery.isHasGeometry()) {
            filters.add(Filters.exists(LocationObservationModel.END_DATE_FIELD, true));
            //End Date
            if (Objects.nonNull(searchQuery.getEndDate())) {
                Bson filterOnEndDate = Filters.lte(LocationObservationModel.END_DATE_FIELD, searchQuery.getEndDate());

                Bson filterOnStartDate = Filters.and(
                        Filters.exists(LocationObservationModel.START_DATE_FIELD, true),
                        Filters.lte(LocationObservationModel.START_DATE_FIELD, searchQuery.getEndDate()));

                Bson filtersEndDate = Filters.or(filterOnEndDate, filterOnStartDate);

                filters.add(filtersEndDate);
            }
            // Start Date
            if (Objects.nonNull(searchQuery.getStartDate())) {
                Bson filterStartDate = Filters.and(
                        Filters.gte(LocationObservationModel.END_DATE_FIELD, searchQuery.getStartDate()));

                filters.add(filterStartDate);
            }
        }

    }

    @Override
    public String idField() {
        return MongoModel.MONGO_ID_FIELD;
    }

    @Override
    public void upsert(ClientSession session, LocationObservationModel instance) throws MongoException {
        Bson filter = Filters.eq(MongoModel.MONGO_ID_FIELD, instance.getUri());
        upsert(instance, filter, session);
    }

    public List<LocationObservationModel> getSpecificLocation(URI collectionURI, Instant end, Instant start){
        Bson filter = specificFilters(collectionURI, end, start);
        return aggregate(Collections.singletonList(Aggregates.match(filter)), LocationObservationModel.class);
    }

    public void upsertSpecificLocation(ClientSession session,  LocationObservationModel existingObservation, LocationObservationModel newObservation) throws MongoException {
        Document filter = specificFilters(existingObservation.getObservationCollection(), existingObservation.getEndDate(), existingObservation.getStartDate());
        filter.put(MongoModel.MONGO_ID_FIELD, existingObservation.getUri());

        upsert(newObservation, filter, session);
    }

    public void deleteSpecificLocation(ClientSession session, URI collectionURI, Instant end, Instant start){
        Bson filter = specificFilters(collectionURI, end, start);
        deleteMany(session,filter);
    }

    /**
     *
     * "location.to":"http://opensilex.test/id/organization/facility.corroy_18-19", "location.geometry" :{$exists : false}
     */
    public List<LocationObservationModel> searchLocationsWithGeomLinkedToFacility(URI facility) {
        //
        Document filter = new Document();
        Document noExistingGeomFilter = new Document("$exists", false);

        filter.put("location.to", facility);
        filter.put("location.geometry", noExistingGeomFilter);

        return aggregate(Collections.singletonList(Aggregates.match(filter)), LocationObservationModel.class);
    }
    //#endregion

    //#region private

    /**
     *
     *$match
     *  {
     *        	observationCollection : "http://opensilex.dev/id/...",
     *           endDate: ISODate("2022-05-31T11:26:16.856Z",
     *         }
     */
    private Document specificFilters(URI collectionURI, Instant end, Instant start){

        Document filter = new Document();
        filter.put(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, collectionURI);
        filter.put(LocationObservationModel.END_DATE_FIELD, end);
        if (Objects.nonNull(start)) {
            filter.put(LocationObservationModel.START_DATE_FIELD, start);
        }

        return filter;
    }
    //#endregion
}
