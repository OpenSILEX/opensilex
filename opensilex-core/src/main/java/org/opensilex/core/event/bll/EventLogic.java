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

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

public class EventLogic<T extends EventModel> {

    SPARQLService sparql;
    AccountModel currentUser;

    public EventLogic(SPARQLService sparql, AccountModel currentUser) {
        this.sparql = sparql;
        this.currentUser = currentUser;
    }

    //#region PUBLIC METHODS

    public List<EventModel> createEvents(List<EventModel> models) throws Exception {
        models.forEach(model -> model.setPublisher(currentUser.getUri()));
        for (var move : models) {
            if (move.getFrom() != null && move.getTo() == null) {
                throw new BadRequestException("Cannot declare a move with a 'From' value but without a 'To' value.");
            }
        }
        List<MoveEventNoSqlModel> noSqlModels = new ArrayList<>();

        check(models, true);

        // build streams of sparql and no models from main model stream
        for (MoveModel model : models) {

            URI uri = model.getUri();
            if (uri == null) {
                uri = model.generateURI(dao.getGraphAsNode().toString(), model, 0);
                model.setUri(uri);
            }

            // set noSql model uri and update noSql model list
            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            if (noSqlModel != null) {
                String shortEventUri = URIDeserializer.getShortURI(model.getUri().toString());
                noSqlModel.setUri(new URI(shortEventUri));
                noSqlModels.add(noSqlModel);
            }

        }
        return new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(session->{
            //insert into sparql graph
            dao.create(models);
            if (!noSqlModels.isEmpty()) {
                //TODO create MongoReadWriteDao ? Concerns geospatial so will it change soon anyway
                dao.getMoveEventCollection().insertMany(noSqlModels);
            }
            return models;
        });

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
