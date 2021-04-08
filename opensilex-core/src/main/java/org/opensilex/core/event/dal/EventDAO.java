//******************************************************************************
//                          EventDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Renaud COLIN
 */
public class EventDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;

    protected final Node eventGraph;


    // SPARQL vars
    protected static final Var uriVar;
    protected static final Var descriptionVar;
    protected static final Var beginInstantTimeStampVar;
    protected static final Var endInstantTimeStampVar;
    protected static final Var fromVar;
    protected static final Var toVar;
//    protected static final Var effectiveEventEndTimeVar;

    // SPARQL triple
    protected static final Triple beginTriple;
    protected static final Triple beginInstantTimeStampTriple;
    protected static final Triple endTriple;
    protected static final Triple endInstantTimeStampTriple;

    // SPARQL EXPR
//    protected static final Expr bindDateExpr;

    static {

        uriVar = SPARQLQueryHelper.makeVar(SPARQLResourceModel.URI_FIELD);
        descriptionVar = SPARQLQueryHelper.makeVar(EventModel.DESCRIPTION_FIELD);

        fromVar = SPARQLQueryHelper.makeVar(MoveModel.FROM_FIELD);
        toVar = SPARQLQueryHelper.makeVar(MoveModel.TO_FIELD);

        Var beginInstantVar = SPARQLQueryHelper.makeVar(EventModel.START_FIELD);
        Var endInstantVar = SPARQLQueryHelper.makeVar(EventModel.END_FIELD);
        beginInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.START_FIELD));
        endInstantTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(EventModel.END_FIELD));

        beginTriple = new Triple(uriVar, Time.hasBeginning.asNode(), beginInstantVar);
        beginInstantTimeStampTriple = new Triple(beginInstantVar, Time.inXSDDateTimeStamp.asNode(), beginInstantTimeStampVar);
        endTriple = new Triple(uriVar, Time.hasEnd.asNode(), endInstantVar);
        endInstantTimeStampTriple = new Triple(endInstantVar, Time.inXSDDateTimeStamp.asNode(), endInstantTimeStampVar);
    }

    public EventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException {
        this.sparql = sparql;
        this.mongodb = mongodb;

        eventGraph = sparql.getDefaultGraph(EventModel.class);
    }

    public EventModel create(EventModel model) throws Exception {
        sparql.create(eventGraph, model, false);
        return model;
    }

    public List<EventModel> create(List<EventModel> models) throws Exception {
        sparql.create(eventGraph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false);
        return models;
    }

    public URI getGraph() throws URISyntaxException {
        return new URI(eventGraph.toString());
    }

    public EventModel update(EventModel model) throws Exception {
        sparql.update(model);
        return model;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(EventModel.class, uri);
    }

    public EventModel get(URI uri, UserModel user) throws Exception {
        return sparql.loadByURI(eventGraph, EventModel.class, uri, user.getLanguage());
    }


    protected void appendConcernedItemFilter(ElementGroup eventGraphGroupElem, URI concernedItem) {


        // append where between event uri and the multi-valued "concernedItem" property because
        // the sparql service don't fetch multi-valued property during the search call

        if(concernedItem != null){
            Node concernedItemNode = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(concernedItem));
            eventGraphGroupElem.addTriplePattern(new Triple(uriVar, Oeev.concerns.asNode(), concernedItemNode));
        }

    }

    /**
     * Append a REGEX filter on {@link EventModel#DESCRIPTION_FIELD}
     *
     * @param descriptionPattern the pattern to evaluate on event description
     */
    protected void appendDescriptionFilter(ElementGroup eventGraphGroupElem, String descriptionPattern) {

        if(!StringUtils.isEmpty(descriptionPattern)){
            Expr descriptionRegexFilter = SPARQLQueryHelper.regexFilter(descriptionVar.getVarName(), descriptionPattern);
            eventGraphGroupElem.addElementFilter(new ElementFilter(descriptionRegexFilter));
        }
    }

    protected void appendTypeFilter(ElementGroup eventGraphGroupElem, URI type) throws Exception {

        if(type != null){
            Expr typeEqFilter = SPARQLQueryHelper.eq(EventModel.TYPE_FIELD,type);
            eventGraphGroupElem.addElementFilter(new ElementFilter(typeEqFilter));
        }
    }

    protected void appendTimeFilterAndOrder(
            ElementGroup eventGraphGroupElem,
            OffsetDateTime start,
            OffsetDateTime end,
            Map<Expr, Order> specificOrderExprs,
            Map<String, OrderBy> defaultOrderByMap
    ) throws Exception {

        boolean timeNotNulls = (start != null || end != null);
        boolean timeOrdering = defaultOrderByMap.containsKey(EventModel.START_FIELD) || defaultOrderByMap.containsKey(EventModel.END_FIELD);

        if(! timeNotNulls && ! timeOrdering){
            return;
        }

        // add specific ORDER BY for ?begin or ?end, and remove them from default orders
        if (timeOrdering) {

            if (defaultOrderByMap.containsKey(EventModel.START_FIELD)) {

                // put order with a specific Expr based on the begin timestamp
                Order order = defaultOrderByMap.get(EventModel.START_FIELD).getOrder();
                Expr expr = SPARQLQueryHelper.getExprFactory().asVar(beginInstantTimeStampVar);
                specificOrderExprs.put(expr, order);

                // remove order from default orders
                defaultOrderByMap.remove(EventModel.START_FIELD);
            }

            if (defaultOrderByMap.containsKey(EventModel.END_FIELD)) {

                // put order with a specific Expr based on the end timestamp
                Order order = defaultOrderByMap.get(EventModel.END_FIELD).getOrder();
                Expr expr = SPARQLQueryHelper.getExprFactory().asVar(endInstantTimeStampVar);
                specificOrderExprs.put(expr,order);

                // remove order from default orders
                defaultOrderByMap.remove(EventModel.END_FIELD);
            }
        }

        // append Expr on start and end
        if (timeNotNulls) {
            Expr dateRangeExpr = SPARQLQueryHelper.dateTimeRange(beginInstantTimeStampVar.getVarName(), start, endInstantTimeStampVar.getVarName(), end);
            eventGraphGroupElem.addElementFilter(new ElementFilter(dateRangeExpr));
        }

    }

    public ListWithPagination<EventModel> search(URI concernedItem,
                                                 String descriptionPattern,
                                                 URI type,
                                                 OffsetDateTime start, OffsetDateTime end,
                                                 String lang,
                                                 List<OrderBy> orderByList,
                                                 Integer page, Integer pageSize) throws Exception {


        // build the index of couple (Expr,order) for any specific order by
        Map<Expr, Order> exprOrderIndex = new HashMap<>();

        // build the index of default order by indexed by field name
        Map<String,OrderBy> defaultOrderIndex =  orderByList.stream()
                .collect(Collectors.toMap(OrderBy::getFieldName, order -> order));

        return sparql.searchWithPagination(
                        EventModel.class,
                        lang,
                        (select -> {

                            ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                            ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                            appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                            appendTypeFilter(eventGraphGroupElem, type);
                            appendConcernedItemFilter(eventGraphGroupElem, concernedItem);
                            appendTimeFilterAndOrder(eventGraphGroupElem, start, end, exprOrderIndex, defaultOrderIndex);

                            exprOrderIndex.forEach(select::addOrderBy);
                        }),
                        defaultOrderIndex.values(),
                        page,
                        pageSize
        );

    }

}