/*
 * *****************************************************************************
 *                         MoveLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 17/06/2024 16:02
 * Contact: maximilian.hart@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.event.bll;

import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveEventNoSqlDao;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveLogic extends EventLogic<MoveModel> {

    private MoveEventNoSqlDao noSqlDao;

    public MoveLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, currentUser, new MoveEventDAO(sparql, mongodb));
        this.noSqlDao = new MoveEventNoSqlDao(mongodb.getServiceV2());
    }

    //#region PUBLIC METHODS

    @Override
    public MoveModel updateModel(MoveModel model) throws Exception{
        check(Collections.singletonList(model), false);
        return new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(session -> {
            dao.update(model);
            URI modelUri = model.getUri();
            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            boolean moveExistInMongo = noSqlDao.exists(session, modelUri);

            // the new move event has no data model in mongodb, so we need to delete the old if exists
            if (noSqlModel == null) {
                if (moveExistInMongo) {
                    noSqlDao.delete(session, modelUri);
                }
            } else {
                noSqlModel.setUri(model.getUri());
                noSqlDao.upsert(session, noSqlModel);
            }
            return model;
        });
    }

    @Override
    public void delete(URI uri) throws Exception{

        new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(session -> {
            dao.delete(uri);
            boolean moveExistInMongo = noSqlDao.exists(session, uri);

            if (moveExistInMongo) {
                noSqlDao.delete(session, uri);
            }
            return 0;
        });
    }

    @Override
    public MoveModel get(URI uri) throws Exception {
        MoveModel model = (MoveModel)(dao.get(uri, currentUser));
        if (model == null) {
            throw new NotFoundURIException(uri);
        }
        MoveEventNoSqlModel noSqlModel = noSqlDao.get(uri);
        if (noSqlModel != null) {
            noSqlModel.setUri(uri);
            model.setNoSqlModel(noSqlModel);
        }
        return model;
    }

    public List<MoveModel> createMoves(List<MoveModel> models) throws Exception {
        models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
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
                ((MoveEventDAO)dao).getMoveEventCollection().insertMany(noSqlModels);
            }
            return models;
        });

    }

    //#endregion
}
