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

public class LocationObservationDAO extends MongoReadWriteDao<LocationObservationModel,LocationObservationSearchFilter> {
    public static final String LOCATION_COLLECTION_NAME = "location";

    //#region constructor
    public LocationObservationDAO(MongoDBServiceV2 mongodb){
        super(mongodb,LocationObservationModel.class,LOCATION_COLLECTION_NAME, LOCATION_COLLECTION_NAME);
    }
    //#endregion

    //#region public
    @Override
    public List<Bson> getBsonFilters(LocationObservationSearchFilter searchQuery) {
        List<Bson> filters = new ArrayList<>();

        //Has Geometry
        filters.add(Filters.eq(LocationObservationModel.HAS_GEOMETRY_FIELD, searchQuery.isHasGeometry()));
        //List URI
        if(searchQuery.getObservationCollection() != null){
            filters.add(Filters.eq(LocationObservationModel.OBSERVATION_COLLECTION_FIELD, searchQuery.getObservationCollection()));
        }
        // Date
        if(searchQuery.getDate() != null){
            filters.add(Filters.lte(LocationObservationModel.DATE_FIELD,searchQuery.getDate()));
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
        upsert(instance,filter,session);
    }
    //#endregion
}
