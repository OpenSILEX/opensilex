/*
 * *****************************************************************************
 *                         EventLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/06/2024 14:08
 * Contact: maximilian.hart@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.event.bll;

import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

public class EventLogic<T extends EventModel> {

    SPARQLService sparql;
    MongoDBService mongodb;
    AccountModel currentUser;
    EventDAO<T> dao;

    /**
     * Basic constructor, dao will be set to default EventDao
     */
    public EventLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser) throws SPARQLDeserializerNotFoundException, SPARQLException {
        this.sparql = sparql;
        //TODO change to mongoServiceV2 at end if we can
        this.mongodb = mongodb;
        this.currentUser = currentUser;
        this.dao = new EventDAO<T>(sparql, mongodb);
    }

    /**
     * Constructor used by children Logic classes in case they have a unique Dao
     */
    public EventLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser, EventDAO<T> dao) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.currentUser = currentUser;
        this.dao = dao;
    }

    //#region PUBLIC METHODS

    /**
     *
     * @param models : Events to be created
     * @return the new models with metadata and uri set
     * @throws Exception if validation fails
     */
    public List<T> createEvents(List<T> models) throws Exception {
        models.forEach(model -> model.setPublisher(currentUser.getUri()));
        check(models, true);
        return dao.create(models);
    }

    //#endregion

    //#region PROTECTED METHODS
    protected void check(List<T> models, boolean checkNewModel) throws Exception {

        final int batchSize = 256;
        int i = 0;

        Collection<URI> targetsBuffer = new HashSet<>(batchSize);
        Collection<URI> urisBuffer = new HashSet<>(batchSize);
        Set<URI> duplicateUris = new HashSet<>();

        // optimize checking time : run query on multiple URIs instead of one query by URI

        for (EventModel model : models) {

            // check if URI is new
            URI uri = model.getUri();
            if (checkNewModel && uri != null) {
                if (model.getUri().toString().isEmpty()) {
                    throw new IllegalArgumentException("Empty model uri at index " + 0);
                }

                if (urisBuffer.contains(model.getUri())) {
                    duplicateUris.add(model.getUri());
                } else {
                    urisBuffer.add(model.getUri());
                }

                if (urisBuffer.size() >= batchSize || i == models.size() - 1) {
                    Set<URI> alreadyExistingUris = sparql.getExistingUris(null, urisBuffer, true);
                    if (!alreadyExistingUris.isEmpty()) {
                        throw new SPARQLAlreadyExistingUriListException("[" + EventModel.class.getSimpleName() + "] already existing URIs : ", alreadyExistingUris, EventModel.URI_FIELD);
                    }

                    urisBuffer.clear();
                }
            }

            // check if target already exists
            targetsBuffer.addAll(model.getTargets());
            if (targetsBuffer.size() >= batchSize || i == models.size() - 1) {
                Set<URI> unknownTargets = new HashSet<>();
                try{
                    unknownTargets = sparql.getExistingUris(null, targetsBuffer, false);
                    if (!unknownTargets.isEmpty()) {
                        throw new Exception();
                    }
                }catch (Exception e){
                    throw new SPARQLInvalidUriListException("[" + EventModel.class.getSimpleName() + "] Unknown targets : ", unknownTargets, EventModel.TARGETS_FIELD);
                }
                targetsBuffer.clear();
            }

            i++;
        }

        if (!duplicateUris.isEmpty()) {
            throw new SPARQLInvalidUriListException("[" + EventModel.class.getSimpleName() + "] Duplicate event URIs : ", duplicateUris, EventModel.URI_FIELD);
        }

    }
}
