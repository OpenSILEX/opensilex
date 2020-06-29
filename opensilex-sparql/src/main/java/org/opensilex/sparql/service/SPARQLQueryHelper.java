//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_Regex;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.opensilex.sparql.deserializer.DateDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.time.LocalDate;
import java.util.*;
import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;

import org.apache.jena.arq.querybuilder.Converters;

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
        if (uris != null && !uris.isEmpty()) {
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            // get ressource with relation specified in the given list
            return exprFactory.in(makeVar(uriField), uris.stream()
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

    /**
     * @param startDateVarName the name of the startDate variable , should not be null if startDate is not null
     * @param startDate the start date
     * @param endDateVarName the name of the endDate variable , should not be null if endDate is not null
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

        boolean startDateNull = startDate == null;
        boolean endDateNull = endDate == null;
        if (startDateNull && endDateNull) {
            return null;
        }

        DateDeserializer dateDeserializer = new DateDeserializer();
        Node endVar;
        Expr endDateExpr = null;

        if (!endDateNull) {
            endVar = NodeFactory.createVariable(endDateVarName);
            endDateExpr = exprFactory.le(endVar, dateDeserializer.getNode(endDate));
        }

        if (!startDateNull) {
            Node startVar = NodeFactory.createVariable(startDateVarName);
            Expr startDateExpr = exprFactory.ge(startVar, dateDeserializer.getNode(startDate));

            if (!endDateNull) {
                return exprFactory.and(startDateExpr, endDateExpr);
            }
            return startDateExpr;
        }
        return endDateExpr;
    }

    public static Expr intervalDateRange(String startDateVarName, LocalDate startDate, String endDateVarName, LocalDate endDate) throws Exception {

        if (startDate == null || endDate == null) {
            return null;
        }

        DateDeserializer dateDeserializer = new DateDeserializer();
        Node startVar = NodeFactory.createVariable(startDateVarName);
        Node endVar = NodeFactory.createVariable(endDateVarName);
        Expr startDateExpr = exprFactory.ge(endVar, dateDeserializer.getNode(startDate));
        Expr endDateExpr = exprFactory.le(startVar, dateDeserializer.getNode(endDate));
        Expr withEndDateExpr = exprFactory.and(startDateExpr, endDateExpr);
        Expr noEndDateExpr = exprFactory.not(exprFactory.bound(endVar));
        Expr withoutEndDateExpr = exprFactory.and(endDateExpr, noEndDateExpr);
        return exprFactory.or(withEndDateExpr, withoutEndDateExpr);
    }

    public static Var makeVar(Object o) {
        return Converters.makeVar(o);
    }
}
