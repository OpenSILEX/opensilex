/*
 * *******************************************************************************
 *                     PositionDao.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */

package org.opensilex.core.position.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.move.MoveEventDao;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.mongodb.client.model.Projections.excludeId;

/**
 * @author Renaud COLIN
 */
public class PositionDao {

    /**
     * EventDao which allow to retrieve {@link MoveModel} URI and location {@link MoveModel#getTo()} from SPARQL repository.
     */
    private final MoveEventDao moveEventDao;

    private final MongoCollection<MoveEventNoSqlModel> moveEventCollection;

    public final static String POSITION_ARRAY_FIELD = "itemPositions";
    public final static String CONCERNED_ITEM_FIELD = "concernedItem";

    /**
     * The exclusion of itemPositions.concernedItem values from collection.
     * When searching the position for a given concerned item, we don't need to fetch it from mongodb, since we already known the value.
     */
    private final static Bson concernedItemFieldExclusion = Projections.exclude(POSITION_ARRAY_FIELD +"."+ CONCERNED_ITEM_FIELD);

    public PositionDao(MoveEventDao eventDao, MongoCollection<MoveEventNoSqlModel> moveEventCollection) {
        this.moveEventDao = eventDao;
        this.moveEventCollection = moveEventCollection;
    }

    public PositionDao(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException {
        moveEventDao = new MoveEventDao(sparql,mongodb);
        moveEventCollection = moveEventDao.getMoveEventCollection();
    }

    /**
     *
     * @param uri the event uri
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression between {@link MoveEventNoSqlModel#ID_FIELD} and the prefixed version of the given uri
     * @throws URISyntaxException when trying to create a prefixed version of the given uri
     * @apiNote the method will search on collection with a prefixed version of the given uri as id
     *
     */
    public static Bson getEventIdFilter(URI uri) throws URISyntaxException {
        Objects.requireNonNull(uri);
        URI shortUri = new URI(URIDeserializer.getShortURI(uri.toString()));
        return Filters.eq(MoveEventNoSqlModel.ID_FIELD, shortUri);
    }


    /**
     *
     * @param concernedItemUri the URI of the item concerned by a move event.
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression on itemPositions.concernedItem property and the given URI
     *
     * @see MoveEventNoSqlModel#getItemPositions()
     * @see ConcernedItemPositionModel#getPosition()
     */
    private Bson getConcernedItemArrayItemProjection(URI concernedItemUri){
        return Filters.elemMatch(POSITION_ARRAY_FIELD,
                Filters.eq(CONCERNED_ITEM_FIELD,concernedItemUri)
        );
    }

    /**
     *
     * @param eventUris the {@link Stream} of event URI
     * @return a {@link Bson} with a {@link Filters#in(String, Object[])} with {@link MoveEventNoSqlModel#ID_FIELD} and the given URI stream
     */
    private Bson getEventIdInFilter(Stream<URI> eventUris) {

        return Filters.in(MoveEventNoSqlModel.ID_FIELD, eventUris.map(uri -> {
            try {
                return new URI(URIDeserializer.getShortURI(uri.toString()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        })::iterator);
    }

    private void setPositionNoSql(PositionModel position, MoveEventNoSqlModel moveEventNoSqlModel){

        if(moveEventNoSqlModel == null){
            return;
        }

        List<ConcernedItemPositionModel> itemPositions = moveEventNoSqlModel.getItemPositions();
        if(CollectionUtils.isEmpty(itemPositions)){
            return;
        }
        position.setPositionNoSqlModel(itemPositions.get(0).getPosition());
    }


    private PositionModel getPositionModelFromSparqlResult(MoveModel move) throws URISyntaxException {

        PositionModel position = new PositionModel();

        position.setEventUri(move.getUri());
        position.setFrom(move.getFrom());
        position.setTo(move.getTo());
        position.setMoveTime(move.getEnd().getDateTimeStamp());

        return position;
    }

    /**
     * @param objectUri the object on which we get the position
     * @param time the time at which we search the position
     * @return the position of the given object at a given time, null if no move event was found.
     *
     * @apiNote if time is null, then the last known position will be returned
     */
    public PositionModel getPosition(URI objectUri, OffsetDateTime time) throws Exception {

        Objects.requireNonNull(objectUri);


        // URI facility = scientificObjectDAO.getFacilityUri(objectUri,objectGraph);
        MoveModel move = moveEventDao.getMoveEvent(objectUri, time);
        if (move == null) {
            return null;
        }

        // fetch event uri and location from SPARQL result
        PositionModel position = getPositionModelFromSparqlResult(move);

        Bson eventIdFilter = getEventIdFilter(position.getEventUri());
        Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(objectUri);

        MoveEventNoSqlModel moveNoSqlModel = moveEventCollection
                .find(eventIdFilter)
                .projection(
                        Projections.fields(
                                excludeId(),  // don't fetch position _id field
                                concernedItemPositionProjection  //  don't fetch concernedItem and position of other item
                        )
                )
                .first();

        setPositionNoSql(position,moveNoSqlModel);
        return position;
    }

    /**
     * @param concernedItem the object on which we get the position
     * @param start the time at which we search the position
     * @return the position of the given object during a time interval with a descending sort on the move end.
     *
     * @apiNote
     * The method run in two times :
     * <ul>
     *     <li> Search corresponding move URI and location from the SPARQL repository </li>
     *     <li> Then for each move URI, get the corresponding {@link MoveEventNoSqlModel} from the mongodb collection.
     *          <ul>
     *              <li> This last operation is done with a single query with a IN filter on {@link MoveEventNoSqlModel#ID_FIELD} field. </li>
     *              <li> A {@link Map} between move URI and {@link PositionModel} is maintained in order to associated data from the two databases</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     *
     * @see MoveEventDao#searchMoveEvents(URI, String, OffsetDateTime, OffsetDateTime, List, Integer, Integer)
     */
    public LinkedHashMap<URI, PositionModel> getPositionsHistory(URI concernedItem,
                                                                 String descriptionPattern,
                                                                 OffsetDateTime start, OffsetDateTime end,
                                                                 List<OrderBy> orderByList,
                                                                 Integer page, Integer pageSize) throws Exception {

        Objects.requireNonNull(concernedItem);

        // Index of position by uri, sorted by event end time
        LinkedHashMap<URI, PositionModel> positionsByEventUri = new LinkedHashMap<>();

        // search move history sorted with DESC order on move end, from SPARQL repository
        Stream<MoveModel> locationHistory = moveEventDao.searchMoveEvents(concernedItem, descriptionPattern,start, end, orderByList,page, pageSize);

        // for each location, create a position model initialized with the location and update position index
        locationHistory.forEach(result -> {
            try {
                PositionModel position = getPositionModelFromSparqlResult(result);
                positionsByEventUri.put(position.getEventUri(), position);

            } catch (URISyntaxException e) {
               throw new RuntimeException(e);
            }
        });

        if(start != null){
            MoveModel lastMove = moveEventDao.getMoveEvent(concernedItem, start);
//            SPARQLResult lastLocationResult = moveEventDao.getLocationResult(objectUri,start);
            if(lastMove != null){
                PositionModel position = getPositionModelFromSparqlResult(lastMove);
                positionsByEventUri.put(position.getEventUri(), position);
            }
        }


        if(positionsByEventUri.isEmpty()){
            return positionsByEventUri;
        }

        Bson eventInIdFilter = getEventIdInFilter(positionsByEventUri.keySet().stream());
        Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(concernedItem);

        // found all positions from mongodb collection according the filter
        moveEventCollection
                .find(eventInIdFilter)
                .skip(page*pageSize)
                .limit(pageSize)
                .projection(concernedItemPositionProjection)//  don't fetch concernedItem and position of other item
                .forEach(moveNoSqlModel -> {

                    // update position model from index, with the position fetch from mongo
                    PositionModel position = positionsByEventUri.get(moveNoSqlModel.getUri());
                    setPositionNoSql(position,moveNoSqlModel);
                });

        return positionsByEventUri;
    }

}
