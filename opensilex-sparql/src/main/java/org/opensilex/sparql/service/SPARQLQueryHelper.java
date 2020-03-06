//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.sparql.service;

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
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.time.LocalDate;
import java.util.*;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;

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

    public static final Var typeDefVar = makeVar("__type");

    public final static <T> Var getUriFieldVar(Class<T> objectClass) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        SPARQLClassObjectMapper<SPARQLResourceModel> mapper = SPARQLClassObjectMapper.getForClass(objectClass);
        return makeVar(mapper.getURIFieldName());
    }

    public static Expr regexFilter(String varName, String regexPattern) {
        return regexFilter(varName, regexPattern, null);
    }

    public static Expr regexFilter(String varName, String regexPattern, String regexFlag) {
        if (regexPattern == null || regexPattern.equals("")) {
            return null;
        }

        if (regexFlag == null) {
            regexFlag = "i";
        }

        ExprVar name = new ExprVar(varName);
        return new E_Regex(name, regexPattern, regexFlag);
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
     * @param object  the object to compare with the given variable
     * @return an E_Equals expression between the given variable and the given object
     * @throws SPARQLDeserializerNotFoundException if no {@link SPARQLDeserializer} is found for object
     * @see ExprFactory#eq(Object, Object)
     * @see SPARQLDeserializers#getForClass(Class)
     */
    public static Expr eq(String varName, Object object) throws Exception {
        Node node = SPARQLDeserializers.getForClass(object.getClass()).getNode(object);
        return eq(varName, node);
    }

    /**
     * @param varName the variable name
     * @param node    the Jena node to compare with the given variable
     * @return an E_Equals expression between the given variable and the given object
     * @see ExprFactory#eq(Object, Object)
     */
    public static Expr eq(String varName, Node node) {
        return exprFactory.eq(NodeFactory.createVariable(varName), node);
    }

    public static Expr langFilter(String varName, String lang) {


        return or(
                exprFactory.langMatches(exprFactory.lang(NodeFactory.createVariable(varName)), lang),
                exprFactory.eq(exprFactory.lang(NodeFactory.createVariable(varName)), "")
        );
    }

    /**
     * Append a VALUES clause to the given select if values are not empty,
     *
     * @param where  the WhereClause to update
     * @param varName the variable name
     * @param values  the list of values to put in the VALUES set
     * @throws SPARQLDeserializerNotFoundException if no {@link SPARQLDeserializer} is found for an element of values
     * @see <a href=www.w3.org/TR/2013/REC-sparql11-query-20130321/#inline-data>W3C SPARQL VALUES specifications</a>
     * @see SelectBuilder#addWhereValueVar(Object, Object...)
     * @see SPARQLDeserializers#getForClass(Class)
     */
    public static void addWhereValues(WhereClause<?> where, String varName, List<?> values) throws Exception {

        if (values.isEmpty())
            return;

        // convert list to JENA node array and return the new SelectBuilder
        Object[] nodes = new Node[values.size()];
        int i = 0;
        for (Object object : values) {
            nodes[i++] = SPARQLDeserializers.getForClass(object.getClass()).getNode(object);
        }
        where.addWhereValueVar(varName, nodes);
    }


    /**
     *
     * Update the given {@link SelectBuilder} by adding a list of FILTER clause or a VALUES ( ?var1 ?var2 ). <br>
     * If each {@link List} from varValuesMap has the same {@link List#size()}, then a VALUES clause is build. <br>
     *
     * e.g. given the following map :  { var1 -> {v1,v2} , var2 -> {v3,v4}}, the following VALUES clause will be built
     * <pre>
     * VALUES (?var1 ?var2) { (v1 v3) (v2 v4)}
     *</pre>
     *
     * Else we use a FILTER clause
     * e.g. given the following map { var1 -> {v1,v2} , var2 -> {v3,v4,v5}}, the following list of FILTER clause will be built
     *
     * <pre>
     * FILTER(?v1 = v1 || ?v1 = v2)
     * FILTER(?v2 = v3 || ?v2 = v4 || ?v2 = v5)
     *</pre>
     *
     * @param select the SelectBuilder to update
     * @param varValuesMap a map between variable name and the list of values for this variable
     * @throws Exception
     *
     *
     * @see <a href=www.w3.org/TR/2013/REC-sparql11-query-20130321/#inline-data> SPARQL VALUES</a>
     * @see <a href=https://www.w3.org/TR/sparql11-query/#func-logical-or> SPARQL LOGICAL OR</a>
     * @see <a href=https://www.w3.org/TR/sparql11-query/#expressions> SPARQL FILTER </a>
     */
        public static void addWhereValues(SelectBuilder select, Map<String, List<?>> varValuesMap) throws Exception {

            if (varValuesMap.isEmpty())
                return;

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
     * @param startDate        the start date
     * @param endDateVarName   the name of the endDate variable , should not be null if endDate is not null
     * @param endDate          the end date
     * @return an Expr according the two given LocalDate and variable names
     * <pre>
     *     null if startDate and endDate are both null
     *     an {@link E_LogicalAnd} if startDate and endDate are both non null
     *     an {@link E_GreaterThanOrEqual} if only startDate is not null
     *     an {@link E_LessThanOrEqual} if only endDate is not null
     * </pre>
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
}
