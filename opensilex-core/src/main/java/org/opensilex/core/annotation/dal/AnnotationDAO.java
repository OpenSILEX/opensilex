//******************************************************************************
//                          AnnotationDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.OA;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Renaud COLIN
 */
public class AnnotationDAO {

    protected final SPARQLService sparql;
    protected final Node annotationGraph;
    protected final URI annotationGraphURI;
    protected final Triple targetTriple;

    protected final static Var motivationNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(AnnotationModel.MOTIVATION_FIELD));
    protected final static Var motivationDefaultNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectDefaultNameVarName(AnnotationModel.MOTIVATION_FIELD));

    public AnnotationDAO(SPARQLService sparql) throws SPARQLException, URISyntaxException {
        this.sparql = sparql;
        annotationGraph = sparql.getDefaultGraph(AnnotationModel.class);
        annotationGraphURI = new URI(annotationGraph.toString());

        Var uriVar = SPARQLQueryHelper.makeVar(SPARQLResourceModel.URI_FIELD);
        Var targetVar = SPARQLQueryHelper.makeVar(AnnotationModel.TARGET_FIELD);
        targetTriple = new Triple(uriVar, OA.hasTarget.asNode(), targetVar);

    }

    public AnnotationModel create(AnnotationModel model) throws Exception {
        sparql.create(model);
        return model;
    }

    public List<AnnotationModel> create(List<AnnotationModel> models) throws Exception {
        sparql.create(AnnotationModel.class, models);
        return models;
    }

    public AnnotationModel update(AnnotationModel model) throws Exception {
        sparql.update(model);
        return model;
    }

    public List<AnnotationModel> update(List<AnnotationModel> models) throws Exception {
        sparql.update(models);
        return models;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(AnnotationModel.class, uri);
    }

    public AnnotationModel get(URI uri, UserModel user) throws Exception {
        return sparql.getByURI(AnnotationModel.class, uri, user.getLanguage());
    }

    private void appendBodyValueFilter(ElementGroup annotationGraphGroupElem, String descriptionPattern) {

        if (!StringUtils.isEmpty(descriptionPattern)) {
            Expr bodyValueFilterExpr = SPARQLQueryHelper.regexFilter(AnnotationModel.URI_FIELD, descriptionPattern);
            annotationGraphGroupElem.addElementFilter(new ElementFilter(bodyValueFilterExpr));
        }
    }

    private void appendMotivationFilter(ElementGroup annotationGraphGroupElem, URI motivation) throws Exception {

        if (motivation != null) {
            Expr motivationEqFilter = SPARQLQueryHelper.eq(AnnotationModel.MOTIVATION_FIELD, new URI(SPARQLDeserializers.getExpandedURI(motivation.toString())));
            annotationGraphGroupElem.addElementFilter(new ElementFilter(motivationEqFilter));
        }
    }

    private void appendTargetFilter(ElementGroup annotationGraphGroupElem, URI target) throws Exception {

        if (target != null) {
            Expr targetEqFilter = SPARQLQueryHelper.eq(AnnotationModel.TARGET_FIELD, target);
            annotationGraphGroupElem.addTriplePattern(targetTriple);
            annotationGraphGroupElem.addElementFilter(new ElementFilter(targetEqFilter));
        }
    }

    private void appendCreatorFilter(ElementGroup annotationGraphGroupElem, URI creator) throws Exception {

        if (creator != null) {
            Expr targetEqFilter = SPARQLQueryHelper.eq(AnnotationModel.CREATOR_FIELD, creator);
            annotationGraphGroupElem.addElementFilter(new ElementFilter(targetEqFilter));
        }

    }

    public ListWithPagination<AnnotationModel> search(String bodyValuePattern,
                                                      URI target,
                                                      URI motivation,
                                                      URI creator,
                                                      String lang,
                                                      List<OrderBy> orderByList,
                                                      Integer page,
                                                      Integer pageSize) throws Exception {

        // use a specific ordering for motivation : use the motivation name instead of the motivation URI which is used by default
        List<OrderBy> defaultOrderByList = new LinkedList<>();
        Map<Expr, Order> specificOrderMap = new HashMap<>();

        Map<String, Function<String, Stream<Expr>>> specificExprMapping = new HashMap<>();

        specificExprMapping.put(AnnotationModel.MOTIVATION_FIELD, (motivationField -> {
            Expr motivationNameOrdering = new E_StrLowerCase(new ExprVar(motivationNameVar));
            Expr motivationDefaultNameOrdering = new E_StrLowerCase(new ExprVar(motivationDefaultNameVar));

            return Stream.of(motivationNameOrdering, motivationDefaultNameOrdering);
        }));

        SPARQLQueryHelper.computeCustomOrderByList(orderByList,defaultOrderByList,specificOrderMap,specificExprMapping);

        return sparql.searchWithPagination(
                annotationGraph,
                AnnotationModel.class,
                lang,
                selectBuilder -> {

                    // Get element group specific to the annotation graph
                    // this is needed in order to handle filtering on multi-valued attribute 'targets', because all filtering must be applied into the same clause
                    // Initially this attribute is not present into the SelectBuilder (the sparql service don't fetch multi-valued property during the search call)

                    ElementGroup rootElementGroup = selectBuilder.getWhereHandler().getClause();
                    ElementGroup annotationGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup,annotationGraph);

                    appendTargetFilter(annotationGraphGroupElem, target);
                    appendBodyValueFilter(annotationGraphGroupElem, bodyValuePattern);
                    appendMotivationFilter(annotationGraphGroupElem, motivation);
                    appendCreatorFilter(annotationGraphGroupElem, creator);

                    // add specific ORDER BY directly to the select builder
                    specificOrderMap.forEach(selectBuilder::addOrderBy);
                },
                null,
                null,
                defaultOrderByList,
                page,
                pageSize
        );
    }

    /**
     *
     * @param target the URI on which find associated annotations
     * @return the number of annotations associated to a target
     */
    public int countAnnotations(URI target) throws Exception {

        return sparql.count(annotationGraph,AnnotationModel.class,null,countBuilder -> {

            ElementGroup rootElementGroup = countBuilder.getWhereHandler().getClause();
            ElementGroup annotationGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup,annotationGraph);

            appendTargetFilter(annotationGraphGroupElem, target);
        },null);
    }


    public ListWithPagination<MotivationModel> searchMotivations(String stringPattern,
                                                                 String lang,
                                                                 List<OrderBy> orderByList,
                                                                 Integer page,
                                                                 Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                MotivationModel.class,
                lang,
                selectBuilder -> {
                    addMotivationNameRegexFilter(selectBuilder, stringPattern);
                    addMotivationNameLangFilter(selectBuilder, lang);
                },
                orderByList,
                page,
                pageSize
        );

    }

    private void addMotivationNameRegexFilter(SelectBuilder selectBuilder, String stringPattern) {
        Expr regexFilter = SPARQLQueryHelper.regexFilter(MotivationModel.NAME_FIELD, stringPattern);
        if (regexFilter != null) {
            selectBuilder.addFilter(regexFilter);
        }
    }

    private void addMotivationNameLangFilter(SelectBuilder selectBuilder, String lang) {
        if (!StringUtils.isEmpty(lang)) {
            Expr langFilter = SPARQLQueryHelper.langFilterWithDefault(SPARQLNamedResourceModel.NAME_FIELD, Locale.forLanguageTag(lang).getLanguage());
            selectBuilder.addFilter(langFilter);
        }
    }
}
