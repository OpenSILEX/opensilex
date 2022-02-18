//******************************************************************************
//                          EventDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
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
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
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

    // triple and vars associated  to some filters
    protected static final String TARGET_PARTIAL_MATCH_VAR_NAME = "target_partial_match";
    protected static final Var partialTargetMatchVar;
    protected static final Triple partialTargetMatchTriple;

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

        partialTargetMatchVar = SPARQLQueryHelper.makeVar(TARGET_PARTIAL_MATCH_VAR_NAME);
        partialTargetMatchTriple = new Triple(uriVar, Oeev.concerns.asNode(), partialTargetMatchVar);
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

    public URI getGraph() throws URISyntaxException {
        return new URI(eventGraph.toString());
    }

    public T update(T model) throws Exception {
        check(Collections.singletonList(model), false);
        sparql.update(model);
        return model;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(EventModel.class, uri);
    }

    public EventModel get(URI uri, UserModel user) throws Exception {
        return sparql.loadByURI(eventGraph, EventModel.class, uri, user.getLanguage());
    }

    protected void appendInTargetsValues(SelectBuilder select, Stream<URI> targets, int size) {

        ElementGroup rootElementGroup = select.getWhereHandler().getClause();
        ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);
        eventGraphGroupElem.addTriplePattern(targetTriple);

        SPARQLQueryHelper.addWhereUriValues(select, EventModel.TARGETS_FIELD, targets, size);
    }

    /**
     * Handle filtering on target
     *
     * @param eventGraphGroupElem {@link ElementGroup} in which handle target filtering
     * @param target              string representation of a URI (exact target matching) or a part of a URI(partial target matching)
     * @param orderByList         List of {@link OrderBy} used by the query. Used to check if sort on target field is asked
     * @return true if the triple <?uri,oeev:concerns,?targets> is added to the {@link ElementGroup}
     */
    protected boolean appendTargetEqFilter(ElementGroup eventGraphGroupElem, String target, List<OrderBy> orderByList) throws Exception {

        boolean targetFiltering = !StringUtils.isEmpty(target);

        if (!targetFiltering) {
            boolean targetInOrders = orderByList != null && orderByList.stream().anyMatch(order -> order.getFieldName().equalsIgnoreCase(EventModel.TARGETS_FIELD));
            if (!targetInOrders) {
                return false;
            }else{
                // append where between event uri and the multi-valued EventModel.TARGETS_FIELD property because
                // the sparql service don't fetch multi-valued property during the search call, so the ORDER will no works else
                eventGraphGroupElem.addTriplePattern(targetTriple);
                return true;
            }
        }

        boolean exactTargetMatching = false;

        try {
            // full URI/exact matching
            URI targetUri = new URI(target);
            exactTargetMatching = targetUri.isAbsolute();
        } catch (URISyntaxException ignored) {
            // partial matching
        }

        if (exactTargetMatching) {
            String expandedTarget = SPARQLDeserializers.getExpandedURI(new URI(target));
            Node targetNode = NodeFactory.createURI(expandedTarget);
            eventGraphGroupElem.addTriplePattern(new Triple(uriVar, Oeev.concerns.asNode(), targetNode));
        } else {
            /* create EXISTS clause and filter on event with a target matching with the partial string filter.
            It allows the retrieval, for each event matching with target filter, of the full target list(not only URIs which match) */

            WhereBuilder existsWhere = new WhereBuilder();
            existsWhere.addWhere(partialTargetMatchTriple);
            Expr targetStrFilterExpr = SPARQLQueryHelper.regexStrFilter(partialTargetMatchVar.getVarName(), target);
            existsWhere.addFilter(targetStrFilterExpr);

            Expr partialTargetMatchExpr = SPARQLQueryHelper.getExprFactory().exists(existsWhere);
            eventGraphGroupElem.addElementFilter(new ElementFilter(partialTargetMatchExpr));
        }

        // return false, indeed if a partial/exact match is specified, the triple  <?uri,oeev:concerns,?targets> is not needed here
        return false;
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
            handler.addWhere(new TriplePath(makeVar(SPARQLResourceModel.TYPE_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(type)));
            customHandlerByFields.put(SPARQLResourceModel.TYPE_FIELD, handler);
        }
    }


    protected void appendTimeFilter(ElementGroup eventGraphGroupElem,
                                    OffsetDateTime start,
                                    OffsetDateTime end
    ) throws Exception {

        if (start == null && end == null) {
            return;
        }

        Expr durationEventDateRange = SPARQLQueryHelper.eventsIntervalDateRange(startInstantTimeStampVar.getVarName(), start, endInstantTimeStampVar.getVarName(), end);
        eventGraphGroupElem.addElementFilter(new ElementFilter(durationEventDateRange));
    }

    public EventModel fromResult(SPARQLResult result, String lang, EventModel model) throws Exception {

        model.setUri(new URI(SPARQLDeserializers.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD))));
        model.setType(new URI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));

        SPARQLLabel typeLabel = new SPARQLLabel();
        typeLabel.setDefaultLang(StringUtils.isEmpty(lang) ? OpenSilex.DEFAULT_LANGUAGE : lang);
        typeLabel.setDefaultValue(result.getStringValue(SPARQLResourceModel.TYPE_NAME_FIELD));
        model.setTypeLabel(typeLabel);

        model.setIsInstant(Boolean.parseBoolean(result.getStringValue(EventModel.IS_INSTANT_FIELD)));
        model.setDescription(result.getStringValue(EventModel.DESCRIPTION_FIELD));

        String startTimeStamp = result.getStringValue(startInstantTimeStampVar.getVarName());
        if (!StringUtils.isEmpty(startTimeStamp)) {

            InstantModel instant = new InstantModel();
            instant.setUri(URI.create(result.getStringValue(startInstantVar.getVarName())));
            instant.setDateTimeStamp(timeDeserializer.fromString(startTimeStamp));
            model.setStart(instant);
        }

        String endTimeStamp = result.getStringValue(endInstantTimeStampVar.getVarName());
        if (!StringUtils.isEmpty(endTimeStamp)) {

            InstantModel instant = new InstantModel();
            instant.setUri(URI.create(result.getStringValue(endInstantVar.getVarName())));
            instant.setDateTimeStamp(timeDeserializer.fromString(endTimeStamp));
            model.setEnd(instant);
        }

        return model;
    }


    protected void updateOrderByList(List<OrderBy> orderByList) {

        if (CollectionUtils.isEmpty(orderByList)) {
            // Use default ORDER-BY since we use SPARQLListFetcher
            orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);
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

    /**
     *
     * @param target partial or exact match on target
     * @param targets exact match on a URI list
     * @param descriptionPattern
     * @param type
     * @param start
     * @param end
     * @param lang
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     *
     * @implNote <b>targets</b> filter is applied in priority if non-empty or null, else <b>target</b> filter is applied. (Not both of them)
     */
    public ListWithPagination<EventModel> search(String target,
                                                 List<URI> targets,
                                                 String descriptionPattern,
                                                 URI type,
                                                 OffsetDateTime start,
                                                 OffsetDateTime end,
                                                 String lang,
                                                 List<OrderBy> orderByList,
                                                 Integer page,
                                                 Integer pageSize) throws Exception {


        this.updateOrderByList(orderByList);

        // set the custom filter on type
        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        appendTypeFilter(customHandlerByFields, type);

        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();
        AtomicReference<Boolean> targetTripleAdded = new AtomicReference<>(false);

        ListWithPagination<EventModel> results = sparql.searchWithPagination(
                eventGraph,
                EventModel.class,
                lang,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    // match on list of URIs
                    if(! CollectionUtils.isEmpty(targets)){
                        appendInTargetsValues(select, targets.stream(), targets.size());
                    }else{
                        // partial/exact match on one pattern/URI

                        // append target filtering + check if the corresponding triple has been added or not to the query.
                        // Used in order to indicate further to the SPARQLListFetcher if this triple must be added or not
                        targetTripleAdded.set(appendTargetEqFilter(eventGraphGroupElem, target, orderByList));
                    }

                    // for each optional field, the filtering must be applied outside the OPTIONAL
                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                    appendTimeFilter(eventGraphGroupElem, start, end);

                    initialSelect.set(select);
                }),
                customHandlerByFields,
                (result -> fromResult(result, lang, new EventModel())),  // custom result handler, direct convert SPARQLResult to EventModel
                orderByList,
                page,
                pageSize
        );

        Map<String, Boolean> fieldsToFetch = new HashMap<>();

        // Append he <?uri,oeev:concerns,?target> triple only if not already into select.
        fieldsToFetch.put(EventModel.TARGETS_FIELD, !targetTripleAdded.get());

        // manually fetch targets
        SPARQLListFetcher<EventModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                EventModel.class,
                eventGraph,
                fieldsToFetch,
                initialSelect.get(),
                results.getList()
        );
        listFetcher.updateModels();

        return results;
    }

    public int count(List<URI> targets) throws Exception {

        return sparql.count(
                eventGraph,
                EventModel.class,
                null,
                select -> appendInTargetsValues(select, targets.stream(), targets.size()),
                null
        );
    }

}