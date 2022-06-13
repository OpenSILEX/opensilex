//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.mem.TupleSlot;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.opensilex.sparql.deserializer.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;

import org.opensilex.utils.OrderBy;

/**
 * @author Vincent MIGOT
 */
public class SPARQLQueryHelper {

    private SPARQLQueryHelper() {
    }

    protected static final ExprFactory exprFactory = new ExprFactory();

    public static ExprFactory getExprFactory() {
        return exprFactory;
    }

    public static Expr regexFilter(String varName, String regexPattern) {
        return regexFilter(varName, regexPattern, null);
    }

    public static Expr regexStrFilter(String varName, String regexPattern){
        return regexFilter(exprFactory.str(exprFactory.asVar(varName)),regexPattern,null);
    }

    public static Expr regexFilter(Expr expr, String regexPattern, String regexFlag) {
        if (StringUtils.isEmpty(regexPattern)) {
            return null;
        }

        if (regexFlag == null) {
            regexFlag = "i";
        }
        return new E_Regex(expr, regexPattern, regexFlag);
    }

    public static Expr regexFilter(String varName, String regexPattern, String regexFlag) {
        ExprVar name = new ExprVar(varName);
        return regexFilter(name, regexPattern, regexFlag);
    }

    public static Expr regexFilterOnURI(String varName, String regexPattern, String regexFlag) {
        ExprVar name = new ExprVar(varName);
        return regexFilter(new E_Str(name), regexPattern, regexFlag);
    }

    public static Expr or(Expr... expressions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalOr(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }

    public static Expr or(Collection<Expr> expressions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalOr(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }

    public static Expr or(Expr[] expressions, Expr... extensions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalOr(parentExpr, expr);
                }
            }
        }

        for (Expr expr : extensions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalOr(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }

    public static Expr and(Expr... expressions) {
        Expr parentExpr = null;

        for (Expr expr : expressions) {
            if (expr != null) {
                if (parentExpr == null) {
                    parentExpr = expr;
                } else {
                    parentExpr = new E_LogicalAnd(parentExpr, expr);
                }
            }
        }

        return parentExpr;
    }

    /**
     * @param varName the variable name
     * @param object the object to compare with the given variable
     * @return an E_Equals expression between the given variable and the given object
     * @throws SPARQLDeserializerNotFoundException if no {@link SPARQLDeserializer} is found for object
     * @see ExprFactory#eq(Object, Object)
     * @see SPARQLDeserializers#getForClass(Class)
     */
    public static Expr eq(String varName, Object object) throws Exception {
        if (object instanceof Node) {
            return eq(varName, (Node) object);
        }
        Node node = SPARQLDeserializers.getForClass(object.getClass()).getNode(object);
        return eq(varName, node);
    }

    public static Expr eq(Var var, Object object) throws Exception {
        return eq(var.getVarName(), object);
    }

    public static Expr eq(Var var, Node node) throws Exception {
        return eq(var.getVarName(), node);
    }

    /**
     * @param varName the variable name
     * @param node the Jena node to compare with the given variable
     * @return an E_Equals expression between the given variable and the given object
     * @see ExprFactory#eq(Object, Object)
     */
    public static Expr eq(String varName, Node node) {
        return exprFactory.eq(NodeFactory.createVariable(varName), node);
    }

    public static Expr inURIFilter(String uriField, Collection<URI> uris) {
        return inURIFilter(makeVar(uriField), uris);
    }

    public static Expr inURIFilter(Var var, Collection<URI> uris) {
        if (uris != null && !uris.isEmpty()) {
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            // get ressource with relation specified in the given list
            return exprFactory.in(var, uris.stream()
                    .map(uri -> {
                        return NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString()));
                    })
                    .toArray());
        }

        return null;
    }

    public static void inURI(AbstractQueryBuilder<?> select, String uriField, Collection<URI> uris) {
        Expr filter = inURIFilter(uriField, uris);
        if (filter != null) {
            select.getWhereHandler().addFilter(filter);
        }
    }

    /**
     * Constructs a lang filter on the variable. Also takes the default language if it exists (the filters keeps at
     * most 2 values). Do not use this method if you want only one language.
     *
     * @see SPARQLQueryHelper#langFilter(Var, String)
     *
     * @param varName
     * @param lang
     * @return
     */
    public static Expr langFilterWithDefault(String varName, String lang) {
        return exprFactory.or(langFilter(makeVar(varName), lang), langFilter(makeVar(varName), ""));
    }

    /**
     * Constructs an exclusive lang filter on the variable. To filter the default language, use an empty String as the
     * language parameter.
     *
     * @param var
     * @param lang
     * @return
     */
    public static Expr langFilter(Var var, String lang) {
        return exprFactory.langMatches(exprFactory.lang(var), lang);
    }

    /**
     * Append a VALUES clause to the given select if values are not empty,
     *
     * @param where the WhereClause to update
     * @param varName the variable name
     * @param values the list of values to put in the VALUES set
     * @throws SPARQLDeserializerNotFoundException if no {@link SPARQLDeserializer} is found for an element of values
     * @see
     * <a href=www.w3.org/TR/2013/REC-sparql11-query-20130321/#inline-data>W3C SPARQL VALUES specifications</a>
     * @see SelectBuilder#addWhereValueVar(Object, Object...)
     * @see SPARQLDeserializers#getForClass(Class)
     */
    public static void addWhereValues(WhereClause<?> where, String varName, Collection<?> values) throws Exception {

        if (values.isEmpty()) {
            return;
        }

        // convert list to JENA node array and return the new SelectBuilder
        Object[] nodes = new Node[values.size()];
        int i = 0;
        for (Object object : values) {
            nodes[i++] = SPARQLDeserializers.getForClass(object.getClass()).getNodeFromString(object.toString());
        }
        where.addWhereValueVar(varName, nodes);
    }

    /**
     * Append a VALUES clause to the given select if values are not empty,
     *
     * @param where the WhereClause to update
     * @param varName the variable name
     * @param values the list of URI to put in the VALUES set
     *
     * @see
     * <a href=www.w3.org/TR/2013/REC-sparql11-query-20130321/#inline-data>W3C SPARQL VALUES specifications</a>
     * @see SelectBuilder#addWhereValueVar(Object, Object...)
     */
    public static void addWhereUriValues(WhereClause<?> where, String varName, Collection<URI> values) {
        addWhereUriValues(where,varName,values.stream(),values.size());
    }

    public static void addWhereUriStringValues(WhereClause<?> where, String varName, Stream<String> values, boolean expandUri, int size) {

        if (size == 0){
            return;
        }

        // convert list to JENA node array and return the new SelectBuilder
        Object[] nodes = new Node[size];
        AtomicInteger i = new AtomicInteger();
        values.forEach(uri -> {
            String expandedUri = expandUri ? URIDeserializer.getExpandedURI(uri) : uri;
            nodes[i.getAndIncrement()] = NodeFactory.createURI(expandedUri);
        });

        where.addWhereValueVar(varName, nodes);
    }

    public static void addWhereUriValues(WhereClause<?> where, String varName, Stream<URI> values, int size) {
        addWhereUriStringValues(where,varName,values.map(URI::toString),true,size);
    }

    /**
     * <pre>
     * Update the given {@link SelectBuilder} by adding a list of FILTER clause
     * or a VALUES ( ?var1 ?var2 ).
     *
     * If each {@link List} from varValuesMap has the same {@link List#size()},
     * then a VALUES clause is build.
     *
     * e.g. given the following map : ( var1 : (v1,v2) , var2 : (v3,v4)), the
     * following VALUES clause will be built
     *
     * VALUES (?var1 ?var2) { (v1 v3) (v2 v4)}
     *
     * Else we use a FILTER clause e.g. given the following map
     * (var1 : (v1,v2) , var2 : (v3,v4,v5)), the following list of FILTER clause will
     * be built
     * </pre>
     *
     * <pre>
     * FILTER(?v1 = v1 || ?v1 = v2)
     * FILTER(?v2 = v3 || ?v2 = v4 || ?v2 = v5)
     * </pre>
     *
     * @param select the SelectBuilder to update
     * @param varValuesMap a map between variable name and the list of values for this variable
     *
     * @see <a href=www.w3.org/TR/2013/REC-sparql11-query-20130321/#inline-data>
     * SPARQL VALUES</a>
     * @see <a href=https://www.w3.org/TR/sparql11-query/#func-logical-or>
     * SPARQL LOGICAL OR</a>
     * @see <a href=https://www.w3.org/TR/sparql11-query/#expressions> SPARQL FILTER </a>
     */
    public static void addWhereValues(SelectBuilder select, Map<String, List<?>> varValuesMap) throws Exception {

        if (varValuesMap.isEmpty()) {
            return;
        }

        // we use the VALUES clause only if all values list have the same size
        boolean useValues = true;
        Iterator<Map.Entry<String, List<?>>> mapIt = varValuesMap.entrySet().iterator();

        int firstElemSize = mapIt.next().getValue().size();
        while (useValues && mapIt.hasNext()) {
            useValues = mapIt.next().getValue().size() == firstElemSize;
        }

        if (useValues) {
            for (Map.Entry<String, List<?>> entry : varValuesMap.entrySet()) {
                addWhereValues(select, entry.getKey(), entry.getValue());
            }
            return;
        }

        // else we use filter
        for (Map.Entry<String, List<?>> entry : varValuesMap.entrySet()) {

            List<?> values = entry.getValue();
            Expr[] eqExprList = new Expr[values.size()];
            int i = 0;

            for (Object object : values) {
                Node objNode = SPARQLDeserializers.getForClass(object.getClass()).getNode(object);
                eqExprList[i++] = exprFactory.eq(makeVar(entry.getKey()), objNode);
            }
            select.addFilter(or(eqExprList));
        }

    }

    private static Expr dateRange(String startDateVarName, Object startDate, String endDateVarName, Object endDate, SPARQLDeserializer<?> deserializer) throws Exception {

        boolean startDateNull = startDate == null;
        boolean endDateNull = endDate == null;
        if (startDateNull && endDateNull) {
            return null;
        }

        Node endVar;
        Expr endDateExpr = null;

        if (!endDateNull) {
            endVar = NodeFactory.createVariable(endDateVarName);
            endDateExpr = exprFactory.le(endVar, deserializer.getNode(endDate));
        }

        if (!startDateNull) {
            Node startVar = NodeFactory.createVariable(startDateVarName);
            Expr startDateExpr = exprFactory.ge(startVar, deserializer.getNode(startDate));

            if (!endDateNull) {
                return exprFactory.and(startDateExpr, endDateExpr);
            }
            return startDateExpr;
        }
        return endDateExpr;
    }

    /**
     * @param startDateVarName the name of the startDate variable , should not
     * be null if startDate is not null
     * @param startDate the start date
     * @param endDateVarName the name of the endDate variable , should not be
     * null if endDate is not null
     * @param endDate the end date
     * @return an Expr according the two given LocalDate and variable names      <pre>
     *     null if startDate and endDate are both null
     *     an {@link E_LogicalAnd} if startDate and endDate are both non null
     *     an {@link E_GreaterThanOrEqual} if only startDate is not null
     *     an {@link E_LessThanOrEqual} if only endDate is not null
     * </pre>
     *
     * @see ExprFactory#and(Object, Object)
     * @see ExprFactory#le(Object, Object)
     * @see ExprFactory#ge(Object, Object)
     */
    public static Expr dateRange(String startDateVarName, LocalDate startDate, String endDateVarName, LocalDate endDate) throws Exception {
        return dateRange(startDateVarName,startDate,endDateVarName,endDate,new DateDeserializer());
    }

    /**
     * @param startDateVarName the name of the startDate variable , should not
     * be null if startDate is not null
     * @param startDate the start date
     * @param endDateVarName the name of the endDate variable , should not be
     * null if endDate is not null
     * @param endDate the end date
     * @return an Expr according the two given LocalDate and variable names      <pre>
     *     null if startDate and endDate are both null
     *     an {@link E_LogicalAnd} if startDate and endDate are both non null
     *     an {@link E_GreaterThanOrEqual} if only startDate is not null
     *     an {@link E_LessThanOrEqual} if only endDate is not null
     * </pre>
     *
     * @see ExprFactory#and(Object, Object)
     * @see ExprFactory#le(Object, Object)
     * @see ExprFactory#ge(Object, Object)
     */
    public static Expr dateTimeRange(String startDateVarName, OffsetDateTime startDate, String endDateVarName, OffsetDateTime endDate) throws Exception {
        return dateRange(startDateVarName,startDate,endDateVarName,endDate,SPARQLDeserializers.getForClass(OffsetDateTime.class));
    }

    /**
     * <pre>
     * INTERSECTION of interval( delimited by startDate / EndDate) AND the entity lifetime is not null
     * 3 cases :
     *  . entity's endDate is in interval
     *  . entity's endDate SUP  upper bound && entity's startDate INF lower bound
     *  . entity's endDate is null && entity's startDate INF upper bound
     *
     * </pre>
     *
     * @param startDateVarName the name of the startDate variable
     * @param startDate the start date
     * @param endDateVarName the name of the endDate variable
     * @param endDate the end date
     * @return an Expr according the two given LocalDate and variable names null if startDate and endDate are both null an {@link E_LogicalOr} Ex:
     * FILTER ( ( ( ( ?endDate <= "2020-12-31"^^xsd:date ) && ( ?endDate >= "2020-01-01"^^xsd:date ) ) || ( ( ?endDate >= "2020-12-31"^^xsd:date ) &&
     * ( ?startDate <= "2020-12-31"^^xsd:date ) ) ) || ( ( ?startDate <= "2020-12-31"^^xsd:date ) && ( ! bound(?endDate) ) ) )
     *
     */
    public static Expr intervalDateRange(String startDateVarName, LocalDate startDate, String endDateVarName, LocalDate endDate) throws Exception {

        if (startDate == null || endDate == null) {
            return null;
        }

        DateDeserializer dateDeserializer = new DateDeserializer();
        Node startVar = NodeFactory.createVariable(startDateVarName);
        Node endVar = NodeFactory.createVariable(endDateVarName);
        Expr firstExpr = exprFactory.and(exprFactory.le(endVar, dateDeserializer.getNode(endDate)), exprFactory.ge(endVar, dateDeserializer.getNode(startDate)));

        Expr secondExpr = exprFactory.and(exprFactory.ge(endVar, dateDeserializer.getNode(endDate)), exprFactory.le(startVar, dateDeserializer.getNode(endDate)));

        Expr endDateExpr = exprFactory.or(firstExpr, secondExpr);
        Expr noEndDateExpr = exprFactory.not(exprFactory.bound(endVar));

        Expr thirdExpr = exprFactory.and(exprFactory.le(startVar, dateDeserializer.getNode(endDate)), noEndDateExpr);

        return exprFactory.or(endDateExpr, thirdExpr);

    }





    /**
     * <pre>
     * INTERSECTION of interval( delimited by startDate / EndDate) AND the EVENT lifetime is not null
     * 2 cases :
     *  . Instant Event -> NO Event begin attribute
     *  . Event begin exist
     *
     * </pre>
     *
     * @param startDateVarName the name of the startDate variable corresponds to the begin Event attribute
     * @param startDate the start  date filter
     * @param endDateVarName the name of the endDate variable corresponds to the end Event attribute
     * @param endDate the end date filter
     * @return an Expr according the two given LocalDate and variable names null if startDate and endDate are both null
     * Ex:
     *  FILTER ( ( ( ! bound(?_start__timestamp) ) && ( ?_end__timestamp >= "2021-01-22T23:00:00Z"^^xsd:dateTime ) ) || ( bound(?_start__timestamp) && ( ?_end__timestamp >= "2021-01-22T23:00:00Z"^^xsd:dateTime ) ) )
     *
     */
    public static Expr eventsIntervalDateRange(String startDateVarName, OffsetDateTime startDate, String endDateVarName, OffsetDateTime endDate) throws SPARQLDeserializerNotFoundException, Exception {
        if (startDate == null && endDate == null) {
            return null;
        }

        SPARQLDeserializer<?> dateDeserializer = SPARQLDeserializers.getForClass(OffsetDateTime.class);
        Node startVar = NodeFactory.createVariable(startDateVarName);
        Node endVar = NodeFactory.createVariable(endDateVarName);
        // NO event begin (instant Event) AND (endVar < endDate )    OR   Event begin AND ( startVar < endDate )
        if(startDate == null){
            Expr instantDateExpr = exprFactory.not(exprFactory.bound(startVar));
            Expr instantRangeExpr = exprFactory.le(endVar, dateDeserializer.getNode(endDate));
            Expr completeInstantExpr = exprFactory.and(instantDateExpr, instantRangeExpr);

            Expr noInstantDateExpr = exprFactory.bound(startVar);
            Expr noInstantRangeExpr = exprFactory.le(startVar, dateDeserializer.getNode(endDate));
            Expr completeNoInstantExpr = exprFactory.and(noInstantDateExpr, noInstantRangeExpr);

            return exprFactory.or(completeInstantExpr, completeNoInstantExpr);
            // No Event begin (instant Event) AND (endVar > startDate )    OR   Event begin AND ( startDate <endVar )
        } else if(endDate == null){
            Expr instantDateExpr = exprFactory.not(exprFactory.bound(startVar));
            Expr instantRangeExpr = exprFactory.ge(endVar, dateDeserializer.getNode(startDate));
            Expr completeInstantExpr = exprFactory.and(instantDateExpr, instantRangeExpr);

            Expr noInstantDateExpr = exprFactory.bound(startVar);
            Expr noInstantRangeExpr = exprFactory.ge(endVar, dateDeserializer.getNode(startDate));
            Expr completeNoInstantExpr = exprFactory.and(noInstantDateExpr, noInstantRangeExpr);

            return exprFactory.or(completeInstantExpr, completeNoInstantExpr);
            // No Event begin (instant Event) AND ( startDate < endVar < endDate )    OR  Event begin AND ( endDate > startVar AND startDate < endVar   )
        } else {

            Expr instantDateExpr = exprFactory.not(exprFactory.bound(startVar));
            Expr instantRangeExpr = exprFactory.and(exprFactory.le(endVar, dateDeserializer.getNode(endDate)), exprFactory.ge(endVar, dateDeserializer.getNode(startDate)));
            Expr completeInstantExpr = exprFactory.and(instantDateExpr, instantRangeExpr);

            Expr noInstantDateExpr = exprFactory.bound(startVar);
            Expr noInstantRangeExpr = exprFactory.and(exprFactory.le(startVar, dateDeserializer.getNode(endDate)), exprFactory.ge(endVar, dateDeserializer.getNode(startDate)));

            Expr completeNoInstantExpr = exprFactory.and(noInstantDateExpr, noInstantRangeExpr);

            return exprFactory.or(completeInstantExpr, completeNoInstantExpr);

        }

    }

    /**
     * <pre>
     *    Append a Subject Property Object clause to the given select
     * </pre>
     *
     * @param select the SelectBuilder to update
     * @param graph the graph to find
     * @param subject the subject of the relation
     * @param property the property of the relation
     * @param value the object of the relation
     *
     */

    public static void appendRelationFilter(SelectBuilder select, String graph, Node subject, Property property, Object value) throws Exception {

        Objects.requireNonNull(select);
        Objects.requireNonNull(subject);

        // Get the ElementGroup in which we must append the triple
        ElementGroup elementGroup;
        if (graph != null) {
            elementGroup = getSelectOrCreateGraphElementGroup(select.getWhereHandler().getClause(), NodeFactory.createURI(graph));
        } else {
            elementGroup = select.getWhereHandler().getClause();
        }

        // get deserializer associated to the given value and create triple
        SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForClass(value.getClass());
        Triple triple = new Triple(subject, property.asNode(), deserializer.getNode(value));
        elementGroup.addTriplePattern(triple);
    }


    public static Var makeVar(Object o) {
        return Converters.makeVar(o);
    }

    /**
     * Get or create a {@link ElementGroup} which is affected to the given graph into the selectBuilder
     *
     * @param rootElementGroup the {@link ElementGroup} on which get or create the {@link ElementGroup} associated to the given graph
     * @param graph the graph to find or create
     * @return the founded {@link ElementGroup} if it exists, else create and return a new {@link ElementGroup} into the given graph
     */
    public static ElementGroup getSelectOrCreateGraphElementGroup(ElementGroup rootElementGroup, Node graph) {

        Objects.requireNonNull(rootElementGroup);
        Objects.requireNonNull(graph);

        for (Element element : rootElementGroup.getElements()) {

            if (!(element instanceof ElementNamedGraph)) {
                continue;
            }
            ElementNamedGraph elementNamedGraph = (ElementNamedGraph) element;
            if (!elementNamedGraph.getGraphNameNode().equals(graph)) {
                continue;
            }

            Element elementGroup = elementNamedGraph.getElement();
            if (elementGroup instanceof ElementGroup) {
                return (ElementGroup) elementGroup;
            }
        }

        ElementGroup elementGroup = new ElementGroup();
        rootElementGroup.addElement(new ElementNamedGraph(graph, elementGroup));

        return elementGroup;
    }

    /**
     * Utility function used to compute specific ordering on some field.
     *
     * @param initialOrderByList the initial list
     * @param orderByListWithoutCustomOrders the new list which contains all elements from initialOrderByList without {@link OrderBy} which have
     * {@link OrderBy#getFieldName()} contained into the specificExprMapping
     * @param specificOrderMap the new list which contains custom couple ({@link Expr},{@link Order}) which could be applied to
     * {@link SelectBuilder#addOrderBy(Expr)} method
     * @param specificExprMapping the association between {@link OrderBy#getFieldName()} and the {@link Function} which return a Stream of
     * {@link Expr}
     *
     */
    public static void computeCustomOrderByList(List<OrderBy> initialOrderByList,
                                                List<OrderBy> orderByListWithoutCustomOrders,
                                                Map<Expr, Order> specificOrderMap,
                                                Map<String, Function<String, Stream<Expr>>> specificExprMapping) {

        Objects.requireNonNull(initialOrderByList);
        Objects.requireNonNull(orderByListWithoutCustomOrders);
        Objects.requireNonNull(specificOrderMap);
        Objects.requireNonNull(specificExprMapping);

        for (OrderBy orderBy : initialOrderByList) {
            String fieldName = orderBy.getFieldName();

            if (!specificExprMapping.containsKey(fieldName)) {
                orderByListWithoutCustomOrders.add(orderBy);
            } else {
                Stream<Expr> orderByExprs = specificExprMapping.get(fieldName).apply(fieldName);
                orderByExprs.forEach(orderByExpr
                        -> specificOrderMap.put(orderByExpr, orderBy.getOrder())
                );
            }
        }
    }

    /**
     * Append a VALUES clause into builder, between the var and objects from valueStream
     * @param builder the builder to update with VALUES clause
     * @param sparqlVar SPARQL variable on which apply VALUES clause
     * @param valuesStream Stream of values of apply to sparqlVar
     *
     * @apiNote This implementation allow to create the {@code Collection} of {@code Node} to pass to the builder {@link org.apache.jena.arq.querybuilder.handlers.ValuesHandler#addValueVar(Var, Collection)}
     * without creating an intermediate collection/array of object.
     * Unlike the default implementation {@link AbstractQueryBuilder#addValueVar(Object, Object...)} which need an array.
     */
    public static void appendValueStream(AbstractQueryBuilder<?> builder, Var sparqlVar, Stream<?> valuesStream){
        Collection<Node> valueNodes = builder.makeValueNodes(valuesStream.iterator());
        builder.getValuesHandler().addValueVar(sparqlVar,valueNodes);
    }

    /**
     * @param var variable
     * @return an {@link E_Bound} expression for the given variable
     *
     * @apiNote Example : <br>
     * <code><b>bound(var)</b></code> returns <br>
     * <code><b>(BOUND(?var))</b></code>
     */
    public static Expr bound(Var var) {
        return exprFactory.bound(var);
    }

    /**
     * @param sparqlVar variable
     * @param distinct indicate if we use or not DISTINCT on variable into COUNT
     * @param value value
     * @return a {@link E_Equals} expression between a COUNT aggregator on sparqlVar and value
     *
     * @throws SPARQLDeserializerNotFoundException if no {@link SPARQLDeserializer} corresponding to value {@link Class} is found
     * @apiNote Example : <br>
     * <code><b>countEqExpr(myVar,true,0)</b></code> returns <br>
     * <code><b>(COUNT(DISTINCT(?myVar)) = 0)</b></code>
     */
    public static E_Equals countEqExpr(Var sparqlVar,  boolean distinct, Object value) throws SPARQLDeserializerNotFoundException {

        // COUNT Aggregator
        Aggregator countAgg = AggregatorFactory.createCountExpr(
                distinct,
                exprFactory.asExpr(sparqlVar)
        );

        XSDDatatype datatype = SPARQLDeserializers.getForClass(value.getClass()).getDataType();

        // use ExprAggregator in order to transform Aggregator to Expr
        return exprFactory.eq(
                new ExprAggregator(sparqlVar,countAgg) ,
                NodeFactory.createLiteral(value.toString(), datatype)
        );
    }

    /**
     * Build a triple with the given URI inserted in the specified tupleSlot.
     *
     * @param s Subject variable
     * @param p Predicate variable
     * @param o Object variable
     * @param uri URI to insert
     * @param tupleSlot Tuple slot where to insert the URI
     * @return
     * @throws IllegalArgumentException If GRAPH is given as a tuple slot
     */
    public static Triple buildUriTriple(Var s, Var p, Var o, URI uri, TupleSlot tupleSlot) throws IllegalArgumentException {
        Node uriNode = Objects.requireNonNull(SPARQLDeserializers.nodeURI(uri));

        Triple uriTriple;

        switch (tupleSlot) {
            case SUBJECT:
                uriTriple = new Triple(uriNode, p, o);
                break;
            case PREDICATE:
                uriTriple = new Triple(s, uriNode, o);
                break;
            case OBJECT:
                uriTriple = new Triple(s, p, uriNode);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return uriTriple;
    }

    /**
     * Adds the given triple in the where clause. If `namedGraph` is specified, then the triple will be matched in the
     * specified named graph. Otherwise, the triple will be matched only in the default graph.
     *
     * @param where Where clause
     * @param triple Triple to match
     * @param namedGraph The named graph to look for the triple. If null, the default graph will be used.
     */
    public static void addTripleWhereClause(WhereClause<?> where, Triple triple, Object namedGraph) {
        if (Objects.nonNull(namedGraph)) {
            where.addGraph(namedGraph, triple);
        } else {
            Var graphVar = makeVar("_g");
            where.addWhere(triple);
            WhereBuilder notExistsWhere = new WhereBuilder();
            notExistsWhere.addGraph(graphVar, triple);
            where.addFilter(SPARQLQueryHelper.exprFactory.notexists(notExistsWhere));
        }
    }

    public static WhereBuilder buildURIExistsClause(URI uri, TupleSlot tupleSlot, boolean inNamedGraph)
            throws IllegalArgumentException {
        WhereBuilder where = new WhereBuilder();
        Triple triple = buildUriTriple(makeVar("_s"), makeVar("_p"), makeVar("_o"), uri, tupleSlot);
        addTripleWhereClause(where, triple, inNamedGraph ? makeVar("_g") : null);
        return where;
    }

    /**
     * Reserved keyword used for GROUP_CONCAT var naming
     */
    private static final String CONCAT_VAR_SUFFIX = "__opensilex__concat";

    public static final String GROUP_CONCAT_SEPARATOR = ",";


    public static String getConcatVarName(String varName){
        return varName+CONCAT_VAR_SUFFIX;
    }

    public static void appendGroupConcatAggregator(SelectBuilder select, Var var, boolean distinct) throws ParseException {

        Aggregator groupConcat = distinct ?
                new AggGroupConcatDistinct(exprFactory.asExpr(var), GROUP_CONCAT_SEPARATOR) :
                new AggGroupConcat(exprFactory.asExpr(var), GROUP_CONCAT_SEPARATOR);

        Var concatVar = makeVar(getConcatVarName(var.getVarName()));
        select.addVar(groupConcat.toString(), concatVar);
    }
}
