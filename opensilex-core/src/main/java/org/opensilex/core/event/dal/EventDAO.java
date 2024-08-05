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
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.DateTimeDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
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
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Renaud COLIN
 */
public class EventDAO<T extends EventModel, F extends EventSearchFilter> {

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;
    protected final OntologyDAO ontologyDAO;
    protected final Class<T> clazz;

    protected final Node graph;

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

        descriptionTriple = Triple.create(uriVar, RDFS.comment.asNode(), descriptionVar);
        targetTriple = Triple.create(uriVar, Oeev.concerns.asNode(), targetVar);

        startInstantVar = SPARQLQueryHelper.makeVar(EventModel.START_FIELD);
        endInstantVar = SPARQLQueryHelper.makeVar(EventModel.END_FIELD);
        startInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.START_FIELD));
        endInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.END_FIELD));

        isInstantTriple = Triple.create(uriVar, Oeev.isInstant.asNode(), isInstantVar);
        beginTriple = Triple.create(uriVar, Time.hasBeginning.asNode(), startInstantVar);
        beginInstantTimeStampTriple = Triple.create(startInstantVar, Time.inXSDDateTimeStamp.asNode(), startInstantTimeStampVar);
        endTriple = Triple.create(uriVar, Time.hasEnd.asNode(), endInstantVar);
        endInstantTimeStampTriple = Triple.create(endInstantVar, Time.inXSDDateTimeStamp.asNode(), endInstantTimeStampVar);

        partialTargetMatchVar = SPARQLQueryHelper.makeVar(TARGET_PARTIAL_MATCH_VAR_NAME);
        partialTargetMatchTriple = Triple.create(uriVar, Oeev.concerns.asNode(), partialTargetMatchVar);
    }

    public EventDAO(SPARQLService sparql, MongoDBService mongodb, Class<T> clazz) throws SPARQLException, SPARQLDeserializerNotFoundException {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.clazz = clazz;

        ontologyDAO = new OntologyDAO(sparql);
        graph = sparql.getDefaultGraph(clazz);
        timeDeserializer = (DateTimeDeserializer) SPARQLDeserializers.getForClass(OffsetDateTime.class);
    }

    //#region PUBLIC METHODS

    public T create(T model) throws Exception {
        sparql.create(graph, model, false, true);
        return model;
    }

    public List<T> create(List<T> models) throws Exception {
        sparql.create(graph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false, true);
        return models;
    }

    public Node getGraphAsNode(){
        return graph;
    }

    public URI getGraphUri() throws URISyntaxException {
        return new URI(graph.toString());
    }

    public T update(T model) throws Exception {
        sparql.update(model);
        return model;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(EventModel.class, uri);
    }

    public T get(URI uri, AccountModel user) throws Exception {
        return sparql.loadByURI(graph, clazz, uri, user.getLanguage());
    }

    public List<T> getList(List<URI> uris, AccountModel user) throws Exception {
        return sparql.getListByURIs(clazz, uris, user.getLanguage());
    }

    /**
     *
     * @param searchFilter The filter to be used for search
     * @return a paginated list of T
     *
     * @implNote <b>targets</b> filter is applied in priority if non-empty or null, else <b>target</b> filter is applied. (Not both of them)
     */
    public ListWithPagination<T> search(F searchFilter) throws Exception {

        this.updateOrderByList(searchFilter.getOrderByList());

        ListWithPagination<T> results = sparql.searchWithPagination(
                graph,
                clazz,
                searchFilter.getLang(),
                (select -> appendAllFilters(select, searchFilter)),
                getCustomHandlerForFields(searchFilter),
                (result -> fromResult(result, searchFilter.getLang(), clazz.getDeclaredConstructor().newInstance())),
                searchFilter.getOrderByList(),
                searchFilter.getPage(),
                searchFilter.getPageSize()
        );

        // manually fetch targets
        SPARQLListFetcher<T> listFetcher = new SPARQLListFetcher<>(
                sparql,
                clazz,
                graph,
                Collections.singleton(EventModel.TARGETS_FIELD),
                results.getList()
        );
        listFetcher.updateModels();

        return results;
    }

    public int countForTargets(List<URI> targets) throws Exception {

        return sparql.count(
                graph,
                clazz,
                null,
                select -> appendInTargetsValues(select, targets.stream(), targets.size()),
                null
        );
    }

    public int countForTarget(URI target) throws Exception {
        return countForTargets(Collections.singletonList(target));
    }

    //#endregion

    //#region PRIVATE/PROTECTED METHODS

    /**
     * Appends all sparql filters, override this in subclasses to handle specific filters
     *
     * @param select to append to
     * @param filter
     */
    protected void appendAllFilters(SelectBuilder select, F filter) throws Exception {
        ElementGroup rootElementGroup = select.getWhereHandler().getClause();
        ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, graph);

        //append filter for baseType
        if (filter.getBaseType() != null) {
            Var baseTypeVar = makeVar("baseType");
            Var targetTypeVar = makeVar("target");

            eventGraphGroupElem.addTriplePattern(Triple.create(uriVar, Oeev.concerns.asNode(), targetTypeVar));
            select.addWhere(baseTypeVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(filter.getBaseType()));
            select.addWhere(targetTypeVar, RDF.type, baseTypeVar);
        }

        // match on list of URIs
        if(! CollectionUtils.isEmpty(filter.getTargets())){
            appendInTargetsValues(select, filter.getTargets().stream(), filter.getTargets().size());
        }

        // for each optional field, the filtering must be applied outside the OPTIONAL
        appendDescriptionFilter(eventGraphGroupElem, filter.getDescriptionPattern());
        appendTimeFilter(eventGraphGroupElem, filter.getStart(), filter.getEnd());
    }

    /**
     *
     * @param filter
     * @return custom handler by fields for search function, override in subclasses to custom handle specific fields
     */
    protected Map<String, WhereHandler> getCustomHandlerForFields(F filter) throws Exception {
        // set the custom filter on type
        Map<String, WhereHandler> customHandlerByFields = new HashMap<>();
        appendTypeFilter(customHandlerByFields, filter.getType());
        return customHandlerByFields;
    }

    /**
     *
     * @param select The SelectBuilder to modify
     * @param targets list of targets to filter by
     * @param size of the targets stream
     */
    protected void appendInTargetsValues(SelectBuilder select, Stream<URI> targets, int size) {

        ElementGroup rootElementGroup = select.getWhereHandler().getClause();
        ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, graph);
        eventGraphGroupElem.addTriplePattern(targetTriple);

        SPARQLQueryHelper.addWhereUriValues(select, EventModel.TARGETS_FIELD, targets, size);
    }

    /**
     * Append a REGEX filter on {@link EventModel#DESCRIPTION_FIELD}
     *
     * @param descriptionPattern the pattern to evaluate on event description, if null or empty then doesn't do anything
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

    protected T fromResult( SPARQLResult result, String lang, T model) throws Exception {

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
    //#endregion

}