//******************************************************************************
//                          EventDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.DateTimeDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;


/**
 * @author Renaud COLIN
 */
public class EventDAO<T extends EventModel> {

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;
    protected final OntologyDAO ontologyDAO;
    protected final Node eventGraph;

    // SPARQL vars
    protected static final Var uriVar;
    protected static final Var descriptionVar;
    protected static final Var isInstantVar;
    public static final Var startInstantVar;
    public static final Var startInstantTimeStampVar;
    public static final Var endInstantVar;
    public static final Var endInstantTimeStampVar;
    protected static final Var fromVar;
    protected static final Var toVar;
    protected static final Var targetVar;

    // SPARQL triple
    protected static final Triple descriptionTriple;
    protected static final Triple isInstantTriple;
    protected static final Triple beginTriple;
    protected static final Triple beginInstantTimeStampTriple;
    protected static final Triple endTriple;
    protected static final Triple endInstantTimeStampTriple;
    protected static final Triple targetTriple;

    protected final DateTimeDeserializer timeDeserializer;

    static {

        uriVar = SPARQLQueryHelper.makeVar(SPARQLResourceModel.URI_FIELD);
        descriptionVar = SPARQLQueryHelper.makeVar(EventModel.DESCRIPTION_FIELD);
        targetVar = SPARQLQueryHelper.makeVar(EventModel.TARGETS_FIELD);
        isInstantVar = SPARQLQueryHelper.makeVar(EventModel.IS_INSTANT_FIELD);
        fromVar = SPARQLQueryHelper.makeVar(MoveModel.FROM_FIELD);
        toVar = SPARQLQueryHelper.makeVar(MoveModel.TO_FIELD);

        descriptionTriple = new Triple(uriVar, RDFS.comment.asNode(), descriptionVar);
        targetTriple = new Triple(uriVar, Oeev.concerns.asNode(), targetVar);

        startInstantVar = SPARQLQueryHelper.makeVar(EventModel.START_FIELD);
        endInstantVar = SPARQLQueryHelper.makeVar(EventModel.END_FIELD);
        startInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.START_FIELD));
        endInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.END_FIELD));

        isInstantTriple = new Triple(uriVar, Oeev.isInstant.asNode(), isInstantVar);
        beginTriple = new Triple(uriVar, Time.hasBeginning.asNode(), startInstantVar);
        beginInstantTimeStampTriple = new Triple(startInstantVar, Time.inXSDDateTimeStamp.asNode(), startInstantTimeStampVar);
        endTriple = new Triple(uriVar, Time.hasEnd.asNode(), endInstantVar);
        endInstantTimeStampTriple = new Triple(endInstantVar, Time.inXSDDateTimeStamp.asNode(), endInstantTimeStampVar);
    }

    public EventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException, SPARQLDeserializerNotFoundException {
        this.sparql = sparql;
        this.mongodb = mongodb;

        ontologyDAO = new OntologyDAO(sparql);
        eventGraph = sparql.getDefaultGraph(EventModel.class);
        timeDeserializer = (DateTimeDeserializer) SPARQLDeserializers.getForClass(OffsetDateTime.class);
    }

    public T create(T model) throws Exception {
        sparql.create(eventGraph, model, false);
        return model;
    }

    public List<T> create(List<T> models) throws Exception {
        check(models, true);
        sparql.create(eventGraph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false);
        return models;
    }

    public URI getGraph() throws URISyntaxException {
        return new URI(eventGraph.toString());
    }

    public T update(T model) throws Exception {
        return update(model, null);
    }

    public T update(T model, ClassModel classModel) throws Exception {

        if (classModel == null || classModel.getRestrictions().isEmpty()) {
            check(Collections.singletonList(model), false);
            sparql.update(model);
            return model;
        }

        check(Collections.singletonList(model), false);

        try {
            sparql.startTransaction();

            // delete relations manually
            sparql.deleteRelations(eventGraph, model.getUri(), classModel.getRestrictions().keySet());
            sparql.update(model);

            sparql.commitTransaction();

            return model;

        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }

    }

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

            // check if target already exist
            targetsBuffer.addAll(model.getTargets());
            if (targetsBuffer.size() >= batchSize || i == models.size() - 1) {

                Set<URI> unknownTargets = sparql.getExistingUris(null, targetsBuffer, false);
                if (!unknownTargets.isEmpty()) {
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

    public void delete(URI uri) throws Exception {

        ClassModel classModel;
        try {
            classModel = ontologyDAO.getClassModelOf(uri, new URI(Oeev.Event.getURI()), null);
            if (classModel.getRestrictions().isEmpty()) {
                sparql.delete(EventModel.class, uri);
                return;
            }
        } catch (NotFoundURIException e) {
            sparql.delete(EventModel.class, uri);
            return;
        }

        try {
            sparql.startTransaction();
            sparql.deleteRelations(eventGraph, uri, classModel.getRestrictions().keySet());
            sparql.delete(EventModel.class, uri);
            sparql.commitTransaction();

        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }
    }

    public EventModel get(URI uri, UserModel user) throws Exception {
        return sparql.loadByURI(eventGraph, EventModel.class, uri, user.getLanguage());
    }

    protected void appendInTargetsValues(SelectBuilder select, Stream<URI> targets, int size) {

        ElementGroup rootElementGroup = select.getWhereHandler().getClause();
        ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);
        eventGraphGroupElem.addTriplePattern(targetTriple);

        SPARQLQueryHelper.addWhereUriValues(select, EventModel.TARGETS_FIELD, targets,size);
    }


    protected void appendTargetEqFilter(ElementGroup eventGraphGroupElem, URI target, List<OrderBy> orderByList) throws Exception {

        boolean targetFiltering = target != null;
        boolean addTargetTriple;

        if (targetFiltering) {
            addTargetTriple = true;
        } else {
            // append triple if a sort on this field is required
            addTargetTriple = orderByList != null && orderByList.stream().anyMatch(order -> order.getFieldName().equalsIgnoreCase(EventModel.TARGETS_FIELD));
        }

        if (!addTargetTriple) {
            return;
        }

        if (targetFiltering) {
            Node targetNode = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(target));
            eventGraphGroupElem.addTriplePattern(new Triple(uriVar, Oeev.concerns.asNode(), targetNode));
        }else{
            // append where between event uri and the multi-valued EventModel.TARGETS_FIELD property because
            // the sparql service don't fetch multi-valued property during the search call
            eventGraphGroupElem.addTriplePattern(targetTriple);
        }
    }


    /**
     * Append a REGEX filter on {@link EventModel#DESCRIPTION_FIELD}
     *
     * @param descriptionPattern the pattern to evaluate on event description
     */
    protected void appendDescriptionFilter(ElementGroup eventGraphGroupElem, String descriptionPattern) {

        if (!StringUtils.isEmpty(descriptionPattern)) {
            Expr descriptionRegexFilter = SPARQLQueryHelper.regexFilter(descriptionVar.getVarName(), descriptionPattern);
            eventGraphGroupElem.addElementFilter(new ElementFilter(descriptionRegexFilter));
        }
    }

    protected void appendTypeFilter(Map<String, WhereHandler> customHandlerByFields, URI type) throws Exception {

        if (type != null) {
            WhereHandler handler = new WhereHandler();
            handler.addWhere(new TriplePath(makeVar(EventModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(type)));
            customHandlerByFields.put(EventModel.TYPE_FIELD, handler);
        }
    }


    protected void appendTimeFilter(SelectBuilder select,
                                    ElementGroup eventGraphGroupElem,
                                    OffsetDateTime start,
                                    OffsetDateTime end
    ) throws Exception {

        // remove ?start and ?end vars and replace then by start/end timestamp vars
        select.getVars().removeIf(var ->
                var.getVarName().equals(EventModel.START_FIELD) || var.getVarName().equals(EventModel.END_FIELD)
        );
        select.addVar(startInstantTimeStampVar);
        select.addVar(endInstantTimeStampVar);

        if (start == null && end == null) {
            return;
        }

        Expr durationEventDateRange = SPARQLQueryHelper.eventsIntervalDateRange(startInstantTimeStampVar.getVarName(), start, endInstantTimeStampVar.getVarName(), end);
        eventGraphGroupElem.addElementFilter(new ElementFilter(durationEventDateRange));
    }

    public EventModel fromResult(SPARQLResult result, String lang, EventModel model) throws Exception {

        model.setUri(new URI(SPARQLDeserializers.formatURI(result.getStringValue(EventModel.URI_FIELD))));
        model.setType(new URI(result.getStringValue(EventModel.TYPE_FIELD)));

        SPARQLLabel typeLabel = new SPARQLLabel();
        typeLabel.setDefaultLang(StringUtils.isEmpty(lang) ? OpenSilex.DEFAULT_LANGUAGE : lang);
        typeLabel.setDefaultValue(result.getStringValue(EventModel.TYPE_NAME_FIELD));
        model.setTypeLabel(typeLabel);

        model.setIsInstant(Boolean.parseBoolean(result.getStringValue(EventModel.DESCRIPTION_FIELD)));
        model.setDescription(result.getStringValue(EventModel.DESCRIPTION_FIELD));

        String startStr = result.getStringValue(startInstantTimeStampVar.getVarName());
        if (!StringUtils.isEmpty(startStr)) {
            InstantModel instant = new InstantModel();
            instant.setDateTimeStamp(timeDeserializer.fromString(startStr));
            model.setStart(instant);
        }

        String endStr = result.getStringValue(endInstantTimeStampVar.getVarName());
        if (!StringUtils.isEmpty(endStr)) {
            InstantModel instant = new InstantModel();
            instant.setDateTimeStamp(timeDeserializer.fromString(endStr));
            model.setEnd(instant);
        }

        return model;
    }


    protected void updateOrderByList(List<OrderBy> orderByList) {

        if(orderByList == null){
            return;
        }
        
        // specific ordering on end/start -> ordering on timestamp
        for (int i = 0; i < orderByList.size(); i++) {
            OrderBy orderBy = orderByList.get(i);

            if (orderBy.getFieldName().equals(EventModel.START_FIELD)) {
                orderByList.set(i, new OrderBy(startInstantTimeStampVar.getVarName(), orderBy.getOrder()));
            } else if (orderBy.getFieldName().equals(EventModel.END_FIELD)) {
                orderByList.set(i, new OrderBy(endInstantTimeStampVar.getVarName(), orderBy.getOrder()));
            }
        }
    }

    public ListWithPagination<EventModel> search(URI target,
                                                 String descriptionPattern,
                                                 URI type,
                                                 OffsetDateTime start, OffsetDateTime end,
                                                 String lang,
                                                 List<OrderBy> orderByList,
                                                 Integer page, Integer pageSize) throws Exception {


        this.updateOrderByList(orderByList);

        // set the custom filter on type
        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        appendTypeFilter(customHandlerByFields, type);

        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();

        ListWithPagination<EventModel> results = sparql.searchWithPagination(
                eventGraph,
                EventModel.class,
                lang,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    // for each optional field, the filtering must be applied outside of the OPTIONAL
                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                    appendTargetEqFilter(eventGraphGroupElem, target, orderByList);
                    appendTimeFilter(select, eventGraphGroupElem, start, end);
                    initialSelect.set(select);
                }),
                customHandlerByFields,

                // custom result handler, direct convert SPARQLResult to EventModel
                (result -> fromResult(result, lang, new EventModel())),

                orderByList,
                page,
                pageSize
        );

        // manually fetch targets

        Map<String, Boolean> fieldsToFetch = new HashMap<>();

        // Check if the <?uri,oeev:concerns,?target> triple is already into select (due to filtering and/or ordering)
        // If so then no need to add it one more time
        boolean addTargetTriple = target == null && orderByList.stream().noneMatch(order -> order.getFieldName().equalsIgnoreCase(EventModel.TARGETS_FIELD));

        fieldsToFetch.put(EventModel.TARGETS_FIELD, addTargetTriple);

        SPARQLListFetcher<EventModel> dataListFetcher = new SPARQLListFetcher<>(
                sparql,
                EventModel.class,
                eventGraph,
                fieldsToFetch,
                initialSelect.get(),
                results.getList()
        );
        dataListFetcher.updateModels();

        return results;
    }

    public int count(List<URI> targets) throws Exception {

        return sparql.count(
                eventGraph,
                EventModel.class,
                null,
                select -> {
                    appendInTargetsValues(select, targets.stream(),targets.size());
                },
                null
        );
    }

}