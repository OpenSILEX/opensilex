//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.SHACL;
import org.opensilex.utils.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vincent
 */
class SPARQLClassQueryBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassQueryBuilder.class);

    private final SPARQLClassAnalyzer analyzer;

    private final SPARQLClassObjectMapperIndex mapperIndex;

    public SPARQLClassQueryBuilder(SPARQLClassObjectMapperIndex mapperIndex, SPARQLClassAnalyzer analyzer) {
        this.analyzer = analyzer;
        this.mapperIndex = mapperIndex;
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the SPARQL variable which represent the object field name
     * @see SPARQLNamedResourceModel#getName()
     */
    public static String getObjectNameVarName(String objectFieldName) {
        return "_" + objectFieldName + "_name";
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the SPARQL variable which represent the object field name
     * @see SPARQLNamedResourceModel#getName()
     */
    public static String getObjectDefaultNameVarName(String objectFieldName) {
        return "_" + objectFieldName + "_name_default";
    }

    /**
     * @param objectFieldName the field name
     * @return the name of the SPARQL variable which represent the object field timestamp
     * @see InstantModel#getDateTimeStamp()
     */
    public static String getTimeStampVarName(String objectFieldName) {
        return "_" + objectFieldName + "__timestamp";
    }


    public SelectBuilder getSelectBuilder(Node graph, String lang, ThrowingConsumer<SelectBuilder,Exception> filterHandler, Map<String,WhereHandler> customHandlerByFields) throws Exception {
        SelectBuilder selectBuilder = new SelectBuilder();
        selectBuilder.setDistinct(true);

        addSelectUriTypeVars(selectBuilder);
        analyzer.forEachDataProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
        });

        analyzer.forEachObjectProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
            if (SPARQLNamedResourceModel.class.isAssignableFrom(field.getType())) {
                selectBuilder.addVar(getObjectNameVarName(field.getName()));
                selectBuilder.addVar(getObjectDefaultNameVarName(field.getName()));
            }
        });

        analyzer.forEachLabelProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
        });

        initializeQueryBuilder(selectBuilder, graph, lang,customHandlerByFields);
        if(filterHandler != null){
            filterHandler.accept(selectBuilder);
        }
        appendBlankNodeFilter(selectBuilder);

        return selectBuilder;
    }


    public AskBuilder getAskBuilder(Node graph, String lang) throws Exception {
       return getAskBuilder(graph,lang,null,null);
    }

    public AskBuilder getAskBuilder(Node graph, String lang, ThrowingConsumer<AskBuilder,Exception> filterHandler, Map<String,WhereHandler> customHandlerByFields) throws Exception {
        AskBuilder askBuilder = new AskBuilder();

        initializeQueryBuilder(askBuilder, graph, lang, customHandlerByFields);
        if(filterHandler != null){
            filterHandler.accept(askBuilder);
        }
        appendBlankNodeFilter(askBuilder);

        return askBuilder;
    }

    private void appendBlankNodeFilter(AbstractQueryBuilder<?> builder){
        if (!analyzer.allowBlankNode()) {
            ExprFactory exprFactory = new ExprFactory();
            Expr noBlankNodeFilter = exprFactory.not(exprFactory.isBlank(makeVar(analyzer.getURIFieldName())));
            builder.getWhereHandler().addFilter(noBlankNodeFilter);
        }
    }

    public void initializeQueryBuilder(AbstractQueryBuilder<?> builder, Node graph, String lang,Map<String,WhereHandler> customHandlerByFields) {
        String uriFieldName = analyzer.getURIFieldName();
        WhereHandler rootWhereHandler = new WhereHandler();

        Map<String, WhereHandler> requiredHandlersByGraph = new HashMap<>();
        Map<String, List<WhereHandler>> optionalHandlersByGraph = new HashMap<>();
        String graphKey = null;
        if (graph != null) {
            graphKey = graph.toString();
        }
        requiredHandlersByGraph.put(graphKey, rootWhereHandler);

        addQueryBuilderModelWhereProperties(
                builder,
                rootWhereHandler,
                lang,
                (field, property) -> {
                    addSelectProperty(builder, graph, uriFieldName, property, field, requiredHandlersByGraph, optionalHandlersByGraph, customHandlerByFields, null, false);
                },
                (field, property) -> {
                    addSelectProperty(builder, graph, uriFieldName, property, field, requiredHandlersByGraph, optionalHandlersByGraph, customHandlerByFields, lang, true);
                },
                (field, property) -> {
                    addSelectProperty(builder, graph, uriFieldName, property, field, requiredHandlersByGraph, optionalHandlersByGraph, customHandlerByFields, lang, false);
                },
                customHandlerByFields
        );

        // attach a filter/where just after the field definition
        if (customHandlerByFields != null) {
            for (String fieldName : customHandlerByFields.keySet()) {
                Field field = analyzer.getFieldFromName(fieldName);

                // only handle field which are not automatically fetched from db -> dataList and objectList properties
                if (field == null || (!analyzer.isDataListField(field) && !analyzer.isObjectListField(field))) {
                    continue;
                }
                WhereHandler customHandler = customHandlerByFields.get(fieldName);

                if (analyzer.isOptional(field)) {
                    optionalHandlersByGraph.computeIfAbsent(graphKey, k -> Collections.singletonList(customHandler));
                } else {
                    WhereHandler fieldHandler = requiredHandlersByGraph.get(graphKey);
                    if (fieldHandler == null) {
                        requiredHandlersByGraph.put(graphKey, customHandler);
                    } else {
                        fieldHandler.addAll(customHandler);
                    }
                }

            }
        }

        requiredHandlersByGraph.forEach((handlerGraph, handler) -> {
            if (handlerGraph != null) {
                ElementNamedGraph elementNamedGraph = new ElementNamedGraph(SPARQLDeserializers.nodeURI(handlerGraph), handler.getElement());
                builder.getWhereHandler().getClause().addElement(elementNamedGraph);
            } else {
                builder.getHandlerBlock().addAll(handler);
            }
        });

        optionalHandlersByGraph.forEach((handlerGraph, handlerList) -> {

            if (handlerGraph != null) {
                ElementGroup graphElementGroup;

                // Try to get the handler corresponding to the graph from the required handler index (already into the builder)
                WhereHandler graphHandler = requiredHandlersByGraph.get(handlerGraph);
                if (graphHandler != null) {
                    graphElementGroup = graphHandler.getClause();
                } else {
                    // if no handler found, then create and attach it to the builder
                    graphElementGroup = new ElementGroup();
                    ElementNamedGraph elementNamedGraph = new ElementNamedGraph(NodeFactory.createURI(handlerGraph), graphElementGroup);
                    builder.getWhereHandler().getClause().addElement(new ElementOptional(elementNamedGraph));
                }

                // insert all optional element into the graph element
                handlerList.forEach(handler -> {
                    ElementOptional elementOptional = new ElementOptional(handler.getElement());
                    graphElementGroup.addElement(elementOptional);
                });

            } else {
                handlerList.forEach(handler -> {
                    WhereHandler optionalHandler = new WhereHandler();
                    optionalHandler.addOptional(handler);
                    builder.getHandlerBlock().addAll(optionalHandler);
                });

            }

        });

    }

    public SelectBuilder getCountBuilder(Node graph, String countFieldName, String lang, ThrowingConsumer<SelectBuilder,Exception> filterHandler, Map<String, WhereHandler> customHandlerByFields) throws Exception {
        String uriFieldName = analyzer.getURIFieldName();

        SelectBuilder countBuilder = new SelectBuilder();


        // COUNT Aggregator
        countBuilder.addVar(
                AggregatorFactory.createCountExpr(
                        true,
                        SPARQLQueryHelper.getExprFactory().asVar(uriFieldName)
                ).toString(),
                makeVar(countFieldName)
        );

        initializeQueryBuilder(countBuilder, graph, lang,customHandlerByFields);

        if(filterHandler != null){
            filterHandler.accept(countBuilder);
        }
        appendBlankNodeFilter(countBuilder);

        return countBuilder;
    }

    private void addSelectUriTypeVars(SelectBuilder select) {
        select.addVar(analyzer.getURIFieldName());
        select.addVar(analyzer.getTypeFieldName());
        select.addVar(analyzer.getTypeLabelFieldName());
    }

    private void addQueryBuilderModelWhereProperties(
            AbstractQueryBuilder<?> builder,
            WhereHandler rootWhereHandler,
            String lang,
            BiConsumer<Field, Property> dataPropertyHandler,
            BiConsumer<Field, Property> objectPropertyHandler,
            BiConsumer<Field, Property> labelPropertyHandler,
            Map<String,WhereHandler> customHandlerByFields
    ) {
        String uriFieldName = analyzer.getURIFieldName();
        String typeFieldName = analyzer.getTypeFieldName();
        Var typeFieldVar = makeVar(typeFieldName);

        WhereHandler whereHandler = builder.getWhereHandler();
        // WhereHandler used for adding all WHERE clause
        rootWhereHandler.addWhere(builder.makeTriplePath(makeVar(uriFieldName), RDF.type, typeFieldVar));
        whereHandler.addWhere(builder.makeTriplePath(typeFieldVar, Ontology.subClassAny, analyzer.getRDFType()));

        String typeLabelFieldName = analyzer.getTypeLabelFieldName();
        Var typeLabelFieldVar = makeVar(typeLabelFieldName);

        addOptionalLangClauseOrDefault(whereHandler, typeFieldVar, RDFS.label.asNode(), typeLabelFieldVar, lang);

        if(customHandlerByFields != null){
            if(customHandlerByFields.containsKey(SPARQLNamedResourceModel.URI_FIELD)){
                whereHandler.addAll(customHandlerByFields.get(SPARQLNamedResourceModel.URI_FIELD));
            }
            if(customHandlerByFields.containsKey(SPARQLNamedResourceModel.TYPE_FIELD)){
                whereHandler.addAll(customHandlerByFields.get(SPARQLNamedResourceModel.TYPE_FIELD));
            }
            if(customHandlerByFields.containsKey(SPARQLNamedResourceModel.TYPE_NAME_FIELD)){
                whereHandler.addAll(customHandlerByFields.get(SPARQLNamedResourceModel.TYPE_NAME_FIELD));
            }
        }

        analyzer.forEachDataProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                dataPropertyHandler.accept(field, property);
            }
        });

        analyzer.forEachObjectProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                objectPropertyHandler.accept(field, property);
            }
        });

        analyzer.forEachLabelProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                labelPropertyHandler.accept(field, property);
            }
        });

    }

    public <T extends SPARQLResourceModel> UpdateBuilder getCreateBuilder(Node graph, T instance, boolean blankNode, BiConsumer<UpdateBuilder, Node> createExtension) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        addCreateBuilder(graph, instance, create, blankNode,createExtension, null);
        return create;
    }

    /**
     * @param fieldsToExclude list of fields to exclude from the insert query (useful for update operations where some fields should not be updated ie: dc:publisher)
     */
    public <T extends SPARQLResourceModel> Node addCreateBuilder(Node graph, T instance, UpdateBuilder create, boolean blankNode, BiConsumer<UpdateBuilder, Node> createExtension, List<String> fieldsToExclude) throws Exception {
        Node uriNode = executeOnInstanceTriples(graph, instance, (Quad quad, Field field) -> {
            if (fieldsToExclude != null && fieldsToExclude.contains(field.getName())) {
                return;
            }

            if (graph == null) {
                create.addInsert(quad.asTriple());
            } else if (quad.getGraph() != null) {  // use the graph specified during quad build
                create.addInsert(quad);
            } else {  // object/field has no default graph -> use input graph
                create.addInsert(graph, quad.asTriple());
            }
        }, blankNode);

        // append INSERT clause from instance relations
        addRelationsQuads(graph,uriNode,instance,create);

        if (createExtension != null) {
            createExtension.accept(create, uriNode);
        }
        return uriNode;
    }


    public <T extends SPARQLResourceModel> UpdateBuilder getDeleteBuilder(Node graph, T instance) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        addDeleteBuilder(graph, instance, delete);

        return delete;
    }

    public <T extends SPARQLResourceModel> void addDeleteBuilder(Node graph, T instance, UpdateBuilder delete) throws Exception {
        executeOnInstanceTriples(graph, instance, (Quad quad, Field field) -> {
            if (graph == null) {
                delete.addDelete(quad.asTriple());
            } else if (quad.getGraph() != null) {  // use the graph specified during quad build
                delete.addDelete(quad);
            } else {   // object/field has no default graph -> use input graph
                delete.addDelete(graph, quad.asTriple());
            }
        }, false);

    }

    /**
     * Build a query which delete all triples related to the given urisToDelete, except dc:publisher and dc:issued.
     * Useful for update operations where dc:publisher and dc:issued should not be updated.
     * This method handle IgnoreUpdateIfNull SPARQL annotation by not deleting relations for fields with this annotation when the field value is null.
     * Generated query same as getDeleteBuilder with excludedPredicates = [dc:publisher, dc:issued]
     * Filter will be : FILTER (?p NOT IN (dc:publisher, dc:issued))
     * @see SPARQLClassQueryBuilder#getDeleteBuilder(List, URI, List, Map, Map)  to see the generated query example
     */
    public <T extends SPARQLResourceModel> UpdateBuilder getDeleteBuilderForUpdateCases(List<T> modelsToDelete, URI graph) throws IllegalAccessException {
        List<URI> excludedPredicates = List.of(
                URI.create("http://purl.org/dc/terms/publisher"),
                URI.create("http://purl.org/dc/terms/issued")
        );

        // handle IgnoreUpdateIfNull annotation by not deleting relations for fields with this annotation when the field value is null
        List<Field> ignoreUpdateIfNullFields = analyzer.getIgnorUpdateIfNullFields();
        Map<URI, List<URI>> predicatesToIgnoreByUri = new HashMap<>();
        Map<URI, List<URI>> reversePredicatesToIgnoreByUri = new HashMap<>();
        for (T model : modelsToDelete) {
            for (Field field : ignoreUpdateIfNullFields) {
                    Object fieldValue = field.get(model);
                    if (fieldValue == null) {
                        Property property = analyzer.getFieldProperty(field);
                        URI predicateUri = URI.create(property.getURI());

                        boolean isReverseRelation = analyzer.isReverseRelation(field);
                        var mapToUse = isReverseRelation ? reversePredicatesToIgnoreByUri : predicatesToIgnoreByUri;
                        mapToUse.computeIfAbsent(model.getUri(), k -> new ArrayList<>()).add(predicateUri);
                    }
            }
        }

        List<URI> urisToDelete = modelsToDelete.stream().map(SPARQLResourceModel::getUri).toList();
        return getDeleteBuilder(urisToDelete, graph, excludedPredicates, predicatesToIgnoreByUri, reversePredicatesToIgnoreByUri);
    }


    /**
     * Delete all triples related to the given urisToDelete, except those specified by excludedPredicates, predicatesToIgnoreByUri or reversePredicatesToIgnoreByUri params.
     * @param graph could be a URI or null. If null, the graphs clauses are removed and so the query search in default graph only.
     * @param excludedPredicates allow to exclude some triples from deletion by specifying their predicate. For now works only for predicates where the uri to delete is the subject. Handle short and long uris.
     * @param predicatesToIgnoreByUri for more details see {@link #buildNotExistsFilterForUriAndPredicateCouples(Var, Var, Var, Var, URI, boolean, Map)}
     * @param reversePredicatesToIgnoreByUri for more details see {@link #buildNotExistsFilterForUriAndPredicateCouples(Var, Var, Var, Var, URI, boolean, Map)}
     * @implNote  generated query example : (the filter clause appears only if excludedPredicates is not empty)
     *<pre>
     *DELETE {
     *     GRAPH <graphUri> {
     *          ?uriToDelete ?p ?o .
     *          ?s ?p ?uriToDelete .
     *     }
     * }
     * WHERE {
     *     FILTER ( ?uriToDelete IN (<uriToDelete1>, <uriToDelete2>) )
     *
     *     {
     *         GRAPH <graphUri> {
     *             ?uriToDelete ?p ?o .
     *             FILTER (?p NOT IN (<excludedPredicate1>, <excludedPredicate2>))
     *             FILTER NOT EXISTS {
     *                 GRAPH <graphUri> {
     *                     ?uriToDelete ?p ?o .
     *                     FILTER (
     *                         (?uriToDelete = <S1> && ?p = <P1>)
     *                      || (?uriToDelete = <S2> && ?p = <P2>)
     *                      || (?uriToDelete = <S3> && ?p = <P3>)
     *                     )
     *                 }
     *             }
     *         }
     *     }
     *     UNION
     *     {
     *         GRAPH <graphUri> {
     *             ?s ?p ?uriToDelete .
     *             FILTER NOT EXISTS {
     *                 GRAPH <graphUri> {
     *                     ?s ?p ?uriToDelete .
     *                     FILTER (
     *                         (?uriToDelete = <S1> && ?p = <P1>)
     *                      || (?uriToDelete = <S2> && ?p = <P2>)
     *                      || (?uriToDelete = <S3> && ?p = <P3>)
     *                     )
     *                 }
     *             }
     *         }
     *     }
     * }
     *</pre>
     */
    private UpdateBuilder getDeleteBuilder(List<URI> urisToDelete,
                                           URI graph,
                                           List<URI> excludedPredicates,
                                           Map<URI, List<URI>> predicatesToIgnoreByUri,
                                           Map<URI, List<URI>> reversePredicatesToIgnoreByUri) {
        UpdateBuilder delete = new UpdateBuilder();
        if (urisToDelete == null || urisToDelete.isEmpty()) {
            return delete;
        }

        Var uriVar = makeVar("uriToDelete");
        Var subjectVar = makeVar("s");
        Var predicateVar = makeVar("p");
        Var objectVar = makeVar("o");

        Triple relation = new  Triple(uriVar, predicateVar, objectVar);
        Triple inverseRelation = new Triple(subjectVar, predicateVar, uriVar);

        if(graph != null){
            Object graphObject = SPARQLDeserializers.nodeURI(graph);
            delete.addDelete(graphObject, relation);
            delete.addDelete(graphObject, inverseRelation);
        } else {
            delete.addDelete(relation);
            delete.addDelete(inverseRelation);
        }

        WhereBuilder globalWhere = new WhereBuilder();

        globalWhere.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, urisToDelete));

        WhereBuilder classicSubquery = buildWhereClauseForDeleteQuery(
                uriVar,
                predicateVar,
                subjectVar,
                objectVar,
                graph,
                false,
                excludedPredicates,
                predicatesToIgnoreByUri
        );

        WhereBuilder inverseSubquery = buildWhereClauseForDeleteQuery(
                uriVar,
                predicateVar,
                subjectVar,
                objectVar,
                graph,
                true,
                null,
                reversePredicatesToIgnoreByUri
        );


        WhereBuilder globalSubquery = new WhereBuilder();
        globalSubquery.addWhere(classicSubquery);
        globalSubquery.addUnion(inverseSubquery);
        globalWhere.addWhere(globalSubquery);
        delete.addWhere(globalWhere);

        return delete;
    }

    /**
     * @param graph could be a URI or null. If null, the graph clause is removed and so the query search in default graph only.
     * @param isInverseRelation should be true if the uriToDelete is the object in the triple. False otherwise.
     * @param excludedPredicates allow to exclude some triples from deletion by specifying their predicate.
     * @param predicatesToIgnoreByUri for more details see {@link #buildNotExistsFilterForUriAndPredicateCouples(Var, Var, Var, Var, URI, boolean, Map)}
     * @implNote generated SPARQL query example for isInverseRelation = false, graph = null :
     * <pre>
     * GRAPH <graphUri> {
     *     ?uriToDelete ?p ?o .
     *     FILTER (?p NOT IN (<excludedPredicate1>, <excludedPredicate2>))
     *     FILTER NOT EXISTS {
     *         GRAPH <graphUri> {
     *             ?uriToDelete ?p ?o .
     *             FILTER (
     *                  (?uriToDelete = <S1> && ?p = <P1>)
     *               || (?uriToDelete = <S2> && ?p = <P2>)
     *               || (?uriToDelete = <S3> && ?p = <P3>)
     *             )
     *         }
     *     }
     * }
     *</pre>
     */
    private WhereBuilder buildWhereClauseForDeleteQuery(Var uriToDeleteVar,
                                                        Var predicateVar,
                                                        Var subjectVar,
                                                        Var objectVar,
                                                        URI graph,
                                                        boolean isInverseRelation,
                                                        List<URI> excludedPredicates,
                                                        Map<URI, List<URI>> predicatesToIgnoreByUri){
        WhereBuilder globalWhere = new WhereBuilder();
        Triple relation = isInverseRelation ? new Triple(subjectVar, predicateVar, uriToDeleteVar) :
                new Triple(uriToDeleteVar, predicateVar, objectVar);
        WhereBuilder graphSubquery = new WhereBuilder();
        graphSubquery.addWhere(relation);
        //do not delete excluded predicates
        if(!CollectionUtils.isEmpty(excludedPredicates)) {
            Expr predicateFilter = SPARQLQueryHelper.notInUrisFilter(excludedPredicates, predicateVar);
            graphSubquery.addFilter(predicateFilter);
        }
        //do not delete excluded predicate for specific URIs
        if (predicatesToIgnoreByUri != null && !predicatesToIgnoreByUri.isEmpty()) {
            Expr NotExists = buildNotExistsFilterForUriAndPredicateCouples(
                    uriToDeleteVar,
                    predicateVar,
                    subjectVar,
                    objectVar,
                    graph,
                    isInverseRelation,
                    predicatesToIgnoreByUri
            );
            graphSubquery.addFilter(NotExists);
        }
        
        if (graph != null) {
            return globalWhere.addGraph(SPARQLDeserializers.nodeURI(graph), graphSubquery);
        }
        return globalWhere.addWhere(graphSubquery);
    }

    /**
     * Build a NOT EXISTS filter to exclude specific (S,P) or (P,O) couples from deletion
     * @param graph could be a URI or null. If null, the graph clause is removed and so the filter search in default graph only.
     * @param isInverseRelation should be true if the uriToDelete is the object in the triple. False otherwise.
     * @param predicatesToIgnoreByUri for each uriToDelete (key), list of predicates (value) to exclude from deletion.
     * @implNote generated SPARQL query example for isInverseRelation = false and putGraphClause = true :
     * <pre>
     *       FILTER NOT EXISTS {
     *         GRAPH ?g {
     *         ?uriToDelete ?p ?o .
     *
     *         # List of (S, P) couples to exclude from deletion
     *         FILTER (
     *                (?uriToDelete = <S1> && ?p = <P1>)
     *             || (?uriToDelete = <S2> && ?p = <P2>)
     *             || (?uriToDelete = <S3> && ?p = <P3>)
     *               )
     *         }
     *     }
     *</pre>
     */
    private Expr buildNotExistsFilterForUriAndPredicateCouples(Var uriToDeleteVar,
                                                               Var predicateVar,
                                                               Var subjectVar,
                                                               Var objectVar,
                                                               URI graph,
                                                               boolean isInverseRelation,
                                                               Map<URI, List<URI>> predicatesToIgnoreByUri){
        //create new map to work only with long uri in string format
        Map<String, List<String>> finalPredicatesToIgnoreByUri = new HashMap<>();
        for (URI uriToDelete : predicatesToIgnoreByUri.keySet()) {
            List<String> modifiedPredicates = new ArrayList<>();
            for (URI predicate : predicatesToIgnoreByUri.get(uriToDelete)) {
                String expandedPredicate = SPARQLDeserializers.getExpandedURI(predicate);
                modifiedPredicates.add(expandedPredicate);
            }
            String expandedUriToDelete = SPARQLDeserializers.getExpandedURI(uriToDelete);
            finalPredicatesToIgnoreByUri.put(expandedUriToDelete, modifiedPredicates);
        }

        WhereBuilder globalWhere = new WhereBuilder();
        Triple relation = isInverseRelation ? new Triple(subjectVar, predicateVar, uriToDeleteVar) :
                new Triple(uriToDeleteVar, predicateVar, objectVar);

        WhereBuilder graphSubquery = new WhereBuilder();
        graphSubquery.addWhere(relation);

        if (!finalPredicatesToIgnoreByUri.isEmpty()) {
            List<Expr> uriPredicateFilters = new ArrayList<>();
            for (String uri : finalPredicatesToIgnoreByUri.keySet()) {
                Expr uriExpr = SPARQLQueryHelper.getExprFactory().asExpr(NodeFactory.createURI(uri));
                List<String> predicates = finalPredicatesToIgnoreByUri.get(uri);
                List<Expr> predicatesExpr = predicates.stream()
                        .map(stringUri -> SPARQLQueryHelper.getExprFactory().asExpr(NodeFactory.createURI(stringUri)))
                        .toList();
                for (Expr predicate : predicatesExpr) {
                    Expr andFilter = SPARQLQueryHelper.getExprFactory().and(
                            SPARQLQueryHelper.getExprFactory().eq(
                                    SPARQLQueryHelper.getExprFactory().asVar(uriToDeleteVar.getVarName()),
                                    uriExpr
                            ),
                            SPARQLQueryHelper.getExprFactory().eq(
                                    SPARQLQueryHelper.getExprFactory().asVar(predicateVar.getVarName()),
                                    predicate
                            )
                    );
                    uriPredicateFilters.add(andFilter);
                }
            }
            if (!uriPredicateFilters.isEmpty()) {
                Expr combinedFilter = uriPredicateFilters.get(0);
                for (int i = 1; i < uriPredicateFilters.size(); i++) {
                    combinedFilter = SPARQLQueryHelper.getExprFactory().or(combinedFilter, uriPredicateFilters.get(i));
                }
                graphSubquery.addFilter(combinedFilter);
            }
        }

        if (graph != null ) {
            globalWhere.addGraph(SPARQLDeserializers.nodeURI(graph), graphSubquery);
        } else {
            globalWhere.addWhere(graphSubquery);
        }
        return SPARQLQueryHelper.getExprFactory().notexists(globalWhere);
    }


    /**
     * Add the WHERE clause into handler, depending on if the given field is optional or not, according {@link #analyzer}
     *
     * @param select the root {@link SelectBuilder}, needed in order to create the {@link TriplePath} to add to the handler
     * @param uriFieldName name of the uri SPARQL variable
     * @param property the {@link Property} to add
     * @param field the property corresponding {@link Field}
     * @see SPARQLClassAnalyzer#isOptional(Field)
     * @see SelectBuilder#makeTriplePath(Object, Object, Object)
     */
    private void addSelectProperty(AbstractQueryBuilder<?> select, Node graph, String uriFieldName, Property property, Field field,
                                   Map<String, WhereHandler> requiredHandlersByGraph,
                                   Map<String, List<WhereHandler>> optionalHandlersByGraph,
                                   Map<String,WhereHandler> customHandlerByFields,
                                   String lang,
                                   boolean isObject) {
        // If the property is not an object and a language is specified, then a filter on the language will be added.
        // In order to fetch the default language in the case where the specified language does not exist, the clause
        // will be duplicated with different language filters.
        if (lang != null && !isObject && analyzer.isOptional(field)) {
            addSelectPropertyWithLangFilter(select, graph, uriFieldName, property, field, requiredHandlersByGraph,
                    optionalHandlersByGraph, customHandlerByFields, lang, false, false);
            if (!lang.isEmpty()) {
                addSelectPropertyWithLangFilter(select, graph, uriFieldName, property, field, requiredHandlersByGraph,
                        optionalHandlersByGraph, customHandlerByFields, "", false, false);
            }
        } else {
            addSelectPropertyWithLangFilter(select, graph, uriFieldName, property, field, requiredHandlersByGraph,
                    optionalHandlersByGraph, customHandlerByFields, lang, isObject, true);
        }
    }

    /**
     * Add a WHERE clause into the handler, with the specified language filter.
     * @param filterLangWithDefault If true, the lang filter will have two options : the specified language and the
     *                              default language. If false, only the specified language will be filtered.
     */
    private void addSelectPropertyWithLangFilter(AbstractQueryBuilder<?> select, Node graph, String uriFieldName, Property property, Field field,
                                                 Map<String, WhereHandler> requiredHandlersByGraph,
                                                 Map<String, List<WhereHandler>> optionalHandlersByGraph,
                                                 Map<String,WhereHandler> customHandlerByFields,
                                                 String lang,
                                                 boolean isObject,
                                                 boolean filterLangWithDefault) {

        Var uriFieldVar = makeVar(uriFieldName);
        Var propertyFieldVar = makeVar(field.getName());

        boolean isOptional = analyzer.isOptional(field);
        boolean isReverseRelation = analyzer.isReverseRelation(field);

        TriplePath triple;
        if (isReverseRelation) {
            triple = select.makeTriplePath(propertyFieldVar, property, uriFieldVar);
            if (isObject) {
                try {
                    graph = mapperIndex.getForResource(analyzer.getFieldRDFType(field)).getDefaultGraph();
                } catch (Exception ex) {
                    LOGGER.error("Unexpected exception", ex);

                }
            }
        } else {
            triple = select.makeTriplePath(uriFieldVar, property, propertyFieldVar);
        }

        TriplePath rdtTypeTriple = null;
        if (property.equals(RDFS.subClassOf)) {
            rdtTypeTriple = select.makeTriplePath(propertyFieldVar, RDF.type, OWL2.Class);
        }

        WhereHandler handler;

        String graphKey = null;
        if (graph != null) {
            graphKey = graph.toString();
        }

        if (isOptional) {
            if (!optionalHandlersByGraph.containsKey(graphKey)) {
                optionalHandlersByGraph.put(graphKey, new ArrayList<>());
            }
            handler = new WhereHandler();
            optionalHandlersByGraph.get(graphKey).add(handler);
        } else {
            if (!requiredHandlersByGraph.containsKey(graphKey)) {
                requiredHandlersByGraph.put(graphKey, new WhereHandler());
            }
            handler = requiredHandlersByGraph.get(graphKey);
        }

        handler.addWhere(triple);

        // add custom handler for the field
        if(customHandlerByFields != null){
            WhereHandler customHandler = customHandlerByFields.get(field.getName());
            if(customHandler != null){
                handler.addAll(customHandler);
            }
        }

        if (lang != null && !isObject) {
            if (filterLangWithDefault) {
                addLangFilterWithDefault(field.getName(), lang, handler);
            } else {
                handler.addFilter(SPARQLQueryHelper.langFilter(propertyFieldVar, lang));
            }
        }

        if (rdtTypeTriple != null) {
            handler.addWhere(rdtTypeTriple);
        }

        try {
            if(isObject){
                // try to directly fetch object label in the query
                if (SPARQLNamedResourceModel.class.isAssignableFrom(field.getType())) {
                    addObjectPropertyName(field, select, propertyFieldVar, lang, graph,isOptional,handler, requiredHandlersByGraph);

                // try to directly fetch time object timestamp in the query
                }else if(InstantModel.class.isAssignableFrom(field.getType())){
                    addTimeTimeStamp(field,propertyFieldVar,select,handler);
                }
            }


        } catch (SPARQLMapperNotFoundException | SPARQLInvalidClassDefinitionException e) {
            throw new RuntimeException(e);
        }
    }

    private void addObjectPropertyName(Field field,
                                       AbstractQueryBuilder<?> select, Var propertyFieldVar, String lang,
                                       Node graph,
                                       boolean isOptional,
                                       WhereHandler handler,
                                       Map<String, WhereHandler> requiredHandlersByGraph) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {

        String objFieldName = getObjectNameVarName(field.getName());
        addOptionalLangClauseOrDefault(handler, propertyFieldVar, RDFS.label.asNode(), makeVar(objFieldName), lang);

        String objDefaultFieldName = getObjectDefaultNameVarName(field.getName());

        TriplePath objectDefaultNameTriple = select.makeTriplePath(propertyFieldVar, RDFS.label, makeVar(objDefaultFieldName));
        Node objectPropertyGraph = mapperIndex.getForClass(field.getType()).getDefaultGraph();

        // put label and label lang filtering into an optional clause
        WhereHandler objectNameDefaultOptionalHandler = new WhereHandler();
        objectNameDefaultOptionalHandler.addWhere(objectDefaultNameTriple);
        if (lang != null) {
            addLangFilterWithDefault(objDefaultFieldName, lang, objectNameDefaultOptionalHandler);
        }

        // if the object is stored in the same graph as the current model then try to get object name into this graph
        if (objectPropertyGraph != null && !objectPropertyGraph.equals(graph) && analyzer.useDefaultGraph(field)) {

            // if the object field is optional, then the object graph clause must be put into the parent clause
            // else the field from the parent clause doesn't match with the object field
            if(isOptional){
                ElementGroup objectGraphClauseIntoRootHandler = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(handler.getClause(),objectPropertyGraph);
                objectGraphClauseIntoRootHandler.addElement(objectNameDefaultOptionalHandler.getElement());
            }else{
                WhereHandler objectGraphHandler = requiredHandlersByGraph.computeIfAbsent(objectPropertyGraph.toString(), objectHandler -> new WhereHandler());
                objectGraphHandler.addOptional(objectNameDefaultOptionalHandler);
            }

        }

    }

    private void addTimeTimeStamp(Field field, Var propertyFieldVar, AbstractQueryBuilder<?> select, WhereHandler handler) {

        String timeStampVarName = getTimeStampVarName(field.getName());
        Var timeStampVar = makeVar(timeStampVarName);

        // projection of timestamp variables for direct retrieval in SELECT
        if (select instanceof SelectBuilder){
            ((SelectBuilder) select).addVar(timeStampVar);
        }

        TriplePath timeStampTriple = select.makeTriplePath(propertyFieldVar, Time.inXSDDateTimeStamp,timeStampVar);
        handler.addWhere(timeStampTriple);
    }

    /**
     * Add a lang filter which also takes the default language, if it exists. That means that this filter can return
     * at most 2 values. Do not use this method if you want only one value.
     */
    private void addLangFilterWithDefault(String fieldName, String lang,
                                          WhereHandler handler) {
        Locale locale = Locale.forLanguageTag(lang);
        handler.addFilter(SPARQLQueryHelper.langFilterWithDefault(fieldName, locale.getLanguage()));
    }

    /**
     * Add an optional where clause with a single triple and an exclusive filter on the language. The following SPARQL
     * code is generated :
     *
     * <pre>
     *     OPTIONAL {
     *         ?fieldVar property ?fieldNameVar
     *         FILTER langMatches(lang(?fieldNameVar), "lang")
     *     }
     * </pre>
     *
     * For example, with the parameters <code>rdfType</code>, <code>RDFS.label</code>, <code>rdfTypeName</code> and <code>"en"</code>,
     * the following code is generated :
     *
     * <pre>
     *     OPTIONAL {
     *         ?rdfType rdfs:label ?rdfTypeName
     *         FILTER (langMatches(lang(?rdfTypeName), "en")
     *     }
     * </pre>
     *
     * The method can also be used to filter on the default language with an empty string as the language parameter.
     */
    private void addOptionalLangClause(WhereHandler where, Var fieldVar, Node property, Var fieldNameVar, String lang) {
        WhereBuilder optionalLang = new WhereBuilder();
        optionalLang.addWhere(fieldVar, property, fieldNameVar);
        optionalLang.addFilter(SPARQLQueryHelper.langFilter(fieldNameVar, lang));
        where.addOptional(optionalLang.getWhereHandler());
    }

    /**
     * <p>
     *     Add two optional where clauses, both with the same single triple but with different filters. In the first
     *     case, the filter matches the given language. In the second case, the filter matches the default language.
     *     So, the query will return the property in the preferred language if it exists, and in the default language otherwise.
     *     It is guaranteed that at most one value will be returned.
     * </p>
     * <p>
     *     For example, it can be used to retrieve the label of an RDF type :
     * </p>
     * <pre>
     *     OPTIONAL {
     *         ?rdfType rdfs:label ?rdfTypeName
     *         FILTER (langMatches(lang(?rdfTypeName), "en")
     *     } OPTIONAL {
     *         ?rdfType rdfs:label ?rdfTypeName
     *         FILTER (langMatches(lang(?rdfTypeName), "")
     *     }
     * </pre>
     */
    private void addOptionalLangClauseOrDefault(WhereHandler where, Var fieldVar, Node property, Var fieldNameVar, String lang) {
        addOptionalLangClause(where, fieldVar, property, fieldNameVar, lang);
        addOptionalLangClause(where, fieldVar, property, fieldNameVar, "");
    }

    private <T extends SPARQLResourceModel> Node executeOnInstanceTriples(Node graph, T instance, BiConsumer<Quad, Field> tripleHandler, boolean blankNode) throws Exception {
        Quad quad;
        Triple triple;

        Node uriNode;
        if (blankNode) {
            uriNode = NodeFactory.createBlankNode();
        } else {
            URI uri = analyzer.getURI(instance);
            uriNode = SPARQLDeserializers.nodeURI(uri);
        }

        if (instance.getType() == null) {
            instance.setType(new URI(analyzer.getRDFType().getURI()));
        }

        triple = new Triple(uriNode, RDF.type.asNode(), SPARQLDeserializers.nodeURI(instance.getType()));
        quad = new Quad(graph, triple);
        tripleHandler.accept(quad, analyzer.getURIField());

        for (Field field : analyzer.getDataPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                Property property = analyzer.getDataPropertyByField(field);
                Node fieldNodeValue = SPARQLDeserializers.getForClass(fieldValue.getClass()).getNode(fieldValue);
                if (analyzer.isReverseRelation(field)) {
                    triple = new Triple(fieldNodeValue, property.asNode(), uriNode);
                } else {
                    triple = new Triple(uriNode, property.asNode(), fieldNodeValue);
                }
                quad = new Quad(graph, triple);
                tripleHandler.accept(quad, field);
            }
        }

        for (Field field : analyzer.getObjectPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                SPARQLClassObjectMapper<SPARQLResourceModel> fieldMapper = mapperIndex.getForClass(fieldValue.getClass());
                URI propertyFieldURI = fieldMapper.getURI(fieldValue);
                if (propertyFieldURI == null) {
                    // TODO change exception type
                    throw new Exception("Object URI value can't be null: " + field.getName());
                } else {
                    Property property = analyzer.getObjectPropertyByField(field);
                    if (analyzer.isReverseRelation(field)) {
                        triple = new Triple(SPARQLDeserializers.nodeURI(propertyFieldURI), property.asNode(), uriNode);
                        quad = new Quad(fieldMapper.getDefaultGraph(), triple);
                    } else {
                        triple = new Triple(uriNode, property.asNode(), SPARQLDeserializers.nodeURI(propertyFieldURI));
                        quad = new Quad(graph, triple);
                    }

                    tripleHandler.accept(quad, field);
                }
            }
        }

        for (Field field : analyzer.getLabelPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);
            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                SPARQLLabel label = (SPARQLLabel) fieldValue;
                Property property = analyzer.getLabelPropertyByField(field);
                for (Map.Entry<String, String> translation : label.getAllTranslations().entrySet()) {
                    Node translationNode = NodeFactory.createLiteral(translation.getValue(), translation.getKey());
                    if (analyzer.isReverseRelation(field)) {
                        triple = new Triple(translationNode, property.asNode(), uriNode);
                    } else {
                        triple = new Triple(uriNode, property.asNode(), translationNode);
                    }
                    quad = new Quad(graph, triple);
                    tripleHandler.accept(quad, field);
                }
            }
        }

        for (Field field : analyzer.getDataListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getFieldValue(field, instance);

            if (fieldValues != null) {
                Property property = analyzer.getDataListPropertyByField(field);

                for (Object listValue : fieldValues) {
                    Node listNodeValue = SPARQLDeserializers.getForClass(listValue.getClass()).getNode(listValue);
                    if (analyzer.isReverseRelation(field)) {
                        triple = new Triple(listNodeValue, property.asNode(), uriNode);
                    } else {
                        triple = new Triple(uriNode, property.asNode(), listNodeValue);
                    }
                    quad = new Quad(graph, triple);
                    tripleHandler.accept(quad, field);
                }
            }
        }

        for (Field field : analyzer.getObjectListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getFieldValue(field, instance);

            if (fieldValues != null) {
                for (Object listValue : fieldValues) {
                    if (listValue == null) {
                        if (!analyzer.isOptional(field)) {
                            // TODO change exception type
                            throw new Exception("Field value can't be null");
                        }
                    } else {
                        SPARQLClassObjectMapper<SPARQLResourceModel> fieldMapper = mapperIndex.getForClass(listValue.getClass());
                        URI propertyFieldURI = fieldMapper.getURI(listValue);
                        if (propertyFieldURI == null) {
                            // TODO change exception type
                            throw new Exception("Object URI value can't be null");
                        } else {
                            Property property = analyzer.getObjectListPropertyByField(field);
                            Node propertyFieldNode = SPARQLDeserializers.nodeURI(propertyFieldURI);

                            if (analyzer.isReverseRelation(field)) {
                                Node fieldGraph = fieldMapper.getDefaultGraph();
                                if(fieldGraph == null && graph != null){
                                    fieldGraph = graph;
                                }
                                quad = new Quad(fieldGraph, propertyFieldNode, property.asNode(), uriNode);
                            } else {
                                quad = new Quad(graph, uriNode, property.asNode(), propertyFieldNode);
                            }

                            tripleHandler.accept(quad, field);
                        }

                    }
                }
            }
        }

        return uriNode;
    }

    private <T extends SPARQLResourceModel> void addRelationsQuads(Node graph, Node uriNode, T instance, UpdateBuilder builder) throws Exception {

        if(instance.getRelations() == null){
            return;
        }

        for (SPARQLModelRelation relation : instance.getRelations()) {
            Class<?> valueType = relation.getType();
            String vString = relation.getValue();

            if (! StringUtils.isEmpty(vString)) {
                Node valueNode = SPARQLDeserializers.getForClass(valueType).getNodeFromString(relation.getValue());
                Triple relationTriple = new Triple(uriNode, SPARQLDeserializers.nodeURI(relation.getProperty()), valueNode);

                Node relationGraph = relation.getGraph() != null ? SPARQLDeserializers.nodeURI(relation.getGraph()) : graph;

                if(relationGraph != null){
                    builder.addInsert(new Quad(relationGraph,relationTriple));
                }else {
                    builder.addInsert(relationTriple);
                }

            }
        }

    }

    public String generateSHACL() {
        Model model = ModelFactory.createDefaultModel();

        Resource shape = model.createResource(analyzer.getRDFType() + "_ShapeSHACL");

        shape.addProperty(RDF.type, SHACL.NodeShape);
        shape.addProperty(SHACL.targetClass, analyzer.getRDFType());

        analyzer.forEachDataProperty((field, property) -> {

            if (!analyzer.isReverseRelation(field)) {
                Seq seq = model.createSeq();
                seq.addProperty(SHACL.path, property);

                XSDDatatype dataType = analyzer.getFieldDatatype(field);
                if (dataType.equals(XSDDatatype.XSDanyURI)) {
                    seq.addProperty(SHACL.nodeKind, SHACL.IRI);
                } else {
                    seq.addProperty(SHACL.datatype, model.createResource(dataType.getURI()));
                }
                seq.addProperty(SHACL.maxCount, "1", XSDDatatype.XSDinteger);

                if (analyzer.isOptional(field)) {
                    seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
                } else {
                    seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
                }

                shape.addProperty(SHACL.property, seq);
            }
        });

        analyzer.forEachObjectProperty(
                (field, property) -> {
                    if (!analyzer.isReverseRelation(field)) {
                        Seq seq = model.createSeq();
                        seq.addProperty(SHACL.path, property);
                        seq.addProperty(SHACL.classProperty, analyzer.getFieldRDFType(field));
                        seq.addProperty(SHACL.maxCount, "1", XSDDatatype.XSDinteger);

                        if (analyzer.isOptional(field)) {
                            seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
                        } else {
                            seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
                        }

                        shape.addProperty(SHACL.property, seq);
                    }
                }
        );

        analyzer.forEachLabelProperty(
                (field, property) -> {
                    Seq seq = model.createSeq();

                    if (!analyzer.isReverseRelation(field)) {
                        seq.addProperty(SHACL.path, property);
                        seq.addProperty(SHACL.uniqueLang, "true", XSDDatatype.XSDboolean);

                        if (analyzer.isOptional(field)) {
                            seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
                        } else {
                            seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
                        }

                        shape.addProperty(SHACL.property, seq);
                    }
                }
        );

        analyzer.forEachDataPropertyList(
                (field, property) -> {
                    Seq seq = model.createSeq();

                    if (!analyzer.isReverseRelation(field)) {
                        seq.addProperty(SHACL.path, property);

                        XSDDatatype dataType = analyzer.getFieldListDatatype(field);
                        if (dataType.equals(XSDDatatype.XSDanyURI)) {
                            seq.addProperty(SHACL.nodeKind, SHACL.IRI);
                        } else {
                            seq.addProperty(SHACL.datatype, model.createResource(dataType.getURI()));
                        }

                        if (analyzer.isOptional(field)) {
                            seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
                        } else {
                            seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
                        }

                        shape.addProperty(SHACL.property, seq);
                    }
                }
        );

        analyzer.forEachObjectPropertyList(
                (field, property) -> {
                    Seq seq = model.createSeq();

                    if (!analyzer.isReverseRelation(field)) {
                        seq.addProperty(SHACL.path, property);

                        seq.addProperty(SHACL.classProperty, analyzer.getFieldListRDFType(field));

                        if (analyzer.isOptional(field)) {
                            seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
                        } else {
                            seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
                        }

                        shape.addProperty(SHACL.property, seq);
                    }
                }
        );

        StringWriter str = new StringWriter();

        model.write(str,
                "TURTLE");

        return str.toString();
    }

}
