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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 * @param <T> Model class that this logic class deals with
 * @param <F> Filter class used for searching
 *
 * This class contains all logic for basic Events.
 */
public class EventLogic<T extends EventModel, F extends EventSearchFilter> {

    SPARQLService sparql;
    MongoDBService mongodb;
    AccountModel currentUser;
    EventDAO<T, F> dao;

    /**
     * Basic constructor, dao will be set to default EventDao
     */
    public EventLogic(
            SPARQLService sparql,
            MongoDBService mongodb,
            AccountModel currentUser,
            Class<T> clazz
            ) throws SPARQLDeserializerNotFoundException, SPARQLException {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.currentUser = currentUser;
        this.dao = new EventDAO<>(sparql, mongodb, clazz);
    }

    /**
     * Constructor used by children Logic classes in case they have a unique Dao
     */
    public EventLogic(
            SPARQLService sparql,
            MongoDBService mongodb,
            AccountModel currentUser,
            EventDAO<T, F> dao) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.currentUser = currentUser;
        this.dao = dao;
    }

    //#region PUBLIC METHODS

    /**
     *
     * @param models : Events to be created
     * @return the new models with metadata and uri set, performs che"cks beforehand
     * @throws Exception if validation fails
     */
    public List<T> create(List<T> models, boolean validationOnly) throws Exception {
        check(models, true);
        if(!validationOnly){
            models.forEach(model -> model.setPublisher(currentUser.getUri()));
        }
        return dao.create(models);
    }

    public T create(T model) throws Exception {
        model.setPublisher(currentUser.getUri());
        check(Collections.singletonList(model), true);
        return dao.create(model);
    }

    public int countForTargets(List<URI> targets) throws Exception {
        return dao.countForTargets(targets);
    }

    public int countForTarget(URI target) throws Exception{
        return dao.countForTarget(target);
    }

    /**
     *
     * @param model to update if relations are valid
     * @param relations set of triplets to validate then apply
     * @param eventType so that we can detect if the relations are valid
     * @param typeCache can be null, to remember what uri points to what type. Will add to the cache if it wasn't null
     * @return updated model TODO global SPARQLNamedRessourceModel relation setter in an ontology dao ??
     */
    public T setEventRelations(T model, List<RDFObjectRelationDTO> relations, URI eventType, Map<URI, ClassModel> typeCache) throws InvalidValueException, URISyntaxException, SPARQLException {

        if(CollectionUtils.isEmpty(relations) || eventType == null){
            return model;
        }

        ClassModel eventClassModel = null;
        if(!MapUtils.isEmpty(typeCache) && typeCache.containsKey(eventType)){
            eventClassModel = typeCache.get(eventType);
        }
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        if(eventClassModel == null){
            eventClassModel = ontologyDAO.getClassModel(eventType, new URI(Oeev.Event.getURI()), currentUser.getLanguage());
            if(typeCache != null){
                typeCache.put(eventType, eventClassModel);
            }
        }
        Map<URI,URI> shortPropertiesUris = new HashMap<>();

        for (int i = 0; i < relations.size(); i++) {
            RDFObjectRelationDTO relation = relations.get(i);
            if (relation == null) {
                throw new IllegalArgumentException("Relation at index " + i + " is null");
            }

            if (relation.getProperty() == null || relation.getValue() == null) {
                throw new IllegalArgumentException("Relation at index " + i + " has null property or value");
            }

            URI shortPropUri = shortPropertiesUris.get(relation.getProperty());
            if(shortPropUri == null){
                shortPropUri = new URI(SPARQLDeserializers.getShortURI(relation.getProperty()));
                shortPropertiesUris.put(relation.getProperty(),shortPropUri);
            }

            if (!ontologyDAO.validateThenAddObjectRelationValue(dao.getGraphUri(), eventClassModel, shortPropUri, relation.getValue(), model)) {
                throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
            }
        }
        return model;
    }

    /**
     *
     * @param model to update in graph
     * @return updated model after sparql update operation
     */
    public void updateModel(T model) throws Exception {
        check(Collections.singletonList(model), false);
        dao.update(model);
    }

    public void delete(URI uri) throws Exception{
        dao.delete(uri);
    }

    public T get(URI uri) throws Exception {
        T res = (T)dao.get(uri, currentUser);
        if (res == null) {
            throw new NotFoundURIException(uri);
        }
        return res;
    }

    public ListWithPagination<T> search(F filter) throws Exception {
        return dao.search(filter);
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
    //#endregion

}
