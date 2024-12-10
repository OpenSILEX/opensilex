package org.opensilex.core.location.dal;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.deserializer.URIDeserializer;

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
    @Override
    public List<Bson> getBsonFilters(LocationObservationSearchFilter searchQuery) {
        List<Bson> filters = new ArrayList<>();

        //Collection URI
        if (Objects.nonNull(searchQuery.getObservationCollection())) {
            filters.add(Filters.eq(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, searchQuery.getObservationCollection()));
        }
        //Collection List
        if (Objects.nonNull(searchQuery.getObservationCollectionList())  && !searchQuery.getObservationCollectionList().isEmpty()) {
            filters.add(Filters.in(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, searchQuery.getObservationCollectionList()));
        }

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
        } else {
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
        return filters;
    }

    @Override
    public String idField() {
        return LocationObservationModel.OBSERVATION_COLLECTION_FIELD;
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
    //#endregion

    //#region private
    private Document specificFilters(URI collectionURI, Instant end, Instant start){
        //$match
        //{
        //	observationCollection : "http://opensilex.dev/id/...",
        //  endDate: ISODate("2022-05-31T11:26:16.856Z",
        //}

        Document filter = new Document();
        filter.put(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, collectionURI);
        filter.put(LocationObservationModel.END_DATE_FIELD, end);
        if (start != null) {
            filter.put(LocationObservationModel.START_DATE_FIELD, start);
        }

        return filter;
    }
    //#endregion
}
