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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.opensilex.sparql.deserializer.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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

    public static Expr langFilter(String varName, String lang) {
        return exprFactory.or(exprFactory.langMatches(exprFactory.lang(NodeFactory.createVariable(varName)), lang),
                exprFactory.langMatches(exprFactory.lang(NodeFactory.createVariable(varName)), ""));
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

    public static void addWhereUriValues(WhereClause<?> where, String varName, Stream<URI> values, int size) {

        if (size == 0){
            return;
        }

        // convert list to JENA node array and return the new SelectBuilder
        Object[] nodes = new Node[size];
        AtomicInteger i = new AtomicInteger();
        values.forEach(uri -> {
            nodes[i.getAndIncrement()] = SPARQLDeserializers.nodeURI(uri);
        });

        where.addWhereValueVar(varName, nodes);
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

    public static Expr notExistFilter(WhereClause<?> whereClause) {
        return exprFactory.notexists(whereClause);
    }

    public static Expr bound(Var var) {
        return exprFactory.bound(var);
    }
}
