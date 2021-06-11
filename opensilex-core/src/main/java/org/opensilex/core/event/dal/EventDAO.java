//******************************************************************************
//                          EventDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriListException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;

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
    public static final Var startInstantTimeStampVar;
    public static final Var endInstantTimeStampVar;
    protected static final Var fromVar;
    protected static final Var toVar;
    protected static final Var targetVar;

    // SPARQL triple
    protected static final Triple beginTriple;
    protected static final Triple beginInstantTimeStampTriple;
    protected static final Triple endTriple;
    protected static final Triple endInstantTimeStampTriple;
    protected static final Triple targetTriple;

    static {

        uriVar = SPARQLQueryHelper.makeVar(SPARQLResourceModel.URI_FIELD);
        descriptionVar = SPARQLQueryHelper.makeVar(EventModel.DESCRIPTION_FIELD);
        targetVar = SPARQLQueryHelper.makeVar(EventModel.TARGETS_FIELD);
        fromVar = SPARQLQueryHelper.makeVar(MoveModel.FROM_FIELD);
        toVar = SPARQLQueryHelper.makeVar(MoveModel.TO_FIELD);

        Var beginInstantVar = SPARQLQueryHelper.makeVar(EventModel.START_FIELD);
        Var endInstantVar = SPARQLQueryHelper.makeVar(EventModel.END_FIELD);
        startInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.START_FIELD));
        endInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.END_FIELD));

        beginTriple = new Triple(uriVar, Time.hasBeginning.asNode(), beginInstantVar);
        beginInstantTimeStampTriple = new Triple(beginInstantVar, Time.inXSDDateTimeStamp.asNode(), startInstantTimeStampVar);
        endTriple = new Triple(uriVar, Time.hasEnd.asNode(), endInstantVar);
        endInstantTimeStampTriple = new Triple(endInstantVar, Time.inXSDDateTimeStamp.asNode(), endInstantTimeStampVar);

        targetTriple = new Triple(uriVar, Oeev.concerns.asNode(), targetVar);
    }

    public EventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException {
        this.sparql = sparql;
        this.mongodb = mongodb;

        ontologyDAO = new OntologyDAO(sparql);
        eventGraph = sparql.getDefaultGraph(EventModel.class);
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

    protected void appendTargetFilter(ElementGroup eventGraphGroupElem, URI target) {
        if (target != null) {
            Node targetNode = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(target));
            eventGraphGroupElem.addTriplePattern(new Triple(uriVar, Oeev.concerns.asNode(), targetNode));
        }
    }

    protected void appendTargetRegexFilter(ElementGroup eventGraphGroupElem, String targetPattern, List<OrderBy> orderByList) throws URISyntaxException {

        boolean targetFiltering = !StringUtils.isEmpty(targetPattern);
        boolean addTargetTriple;

        if (targetFiltering) {
            addTargetTriple = true;
        } else {
            // append triple if a sort on this field is required
            addTargetTriple = orderByList.stream().anyMatch(order -> order.getFieldName().equalsIgnoreCase(EventModel.TARGETS_FIELD));
        }

        if (!addTargetTriple) {
            return;
        }

        // append where between event uri and the multi-valued EventModel.TARGETS_FIELD property because
        // the sparql service don't fetch multi-valued property during the search call
        eventGraphGroupElem.addTriplePattern(targetTriple);

        if (targetFiltering) {

            String targetStr;
            try {
                URI targetUri = new URI(targetPattern);

                // if the patten include an URI with a prefix, then we must expand URI in order to match with the target str
                targetStr = targetUri.getScheme() != null ? SPARQLDeserializers.getExpandedURI(targetPattern) : targetPattern;

            } catch (URISyntaxException e) {
                targetStr = targetPattern;
            }

            Expr targetRegexFilter = SPARQLQueryHelper.regexStrFilter(targetVar.getVarName(), targetStr);
            eventGraphGroupElem.addElementFilter(new ElementFilter(targetRegexFilter));
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

    protected void appendTypeFilter(ElementGroup eventGraphGroupElem, URI type) throws Exception {

        if (type != null) {
            Expr typeEqFilter = SPARQLQueryHelper.eq(EventModel.TYPE_FIELD, type);
            eventGraphGroupElem.addElementFilter(new ElementFilter(typeEqFilter));
        }
    }

    protected void appendTimeFilter(
            ElementGroup eventGraphGroupElem,
            OffsetDateTime start,
            OffsetDateTime end
    ) throws Exception {

        if (start == null && end == null) {
            return;
        }

        Expr durationEventDateRange = SPARQLQueryHelper.dateTimeRange(startInstantTimeStampVar.getVarName(), start, endInstantTimeStampVar.getVarName(), end);
        eventGraphGroupElem.addElementFilter(new ElementFilter(durationEventDateRange));
    }


    public ListWithPagination<EventModel> search(String targetPattern,
                                                 String descriptionPattern,
                                                 URI type,
                                                 OffsetDateTime start, OffsetDateTime end,
                                                 String lang,
                                                 List<OrderBy> orderByList,
                                                 Integer page, Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                EventModel.class,
                lang,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                    appendTypeFilter(eventGraphGroupElem, type);
                    appendTargetRegexFilter(eventGraphGroupElem, targetPattern, orderByList);
                    appendTimeFilter(eventGraphGroupElem, start, end);
                }),
                orderByList,
                page,
                pageSize
        );

    }

}