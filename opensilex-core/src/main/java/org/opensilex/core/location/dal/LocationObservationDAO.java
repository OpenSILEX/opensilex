package org.opensilex.core.location.dal;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;

import java.util.ArrayList;
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

                Bson filtersEnDate = Filters.or(filterOnEndDate, filterOnStartDate);

                filters.add(filtersEnDate);
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
    //#endregion
}
