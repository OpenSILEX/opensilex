//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import java.net.URI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author vidalmor
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    public VariableDAO(SPARQLService sparql) {
        super(VariableModel.class, sparql);
    }

    static Var entityLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("entity"));
    static Var qualityLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("quality"));
    static Var methodLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("method"));
    static Var unitLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("unit"));

    /**
     * Contains the pre-computed list of SPARQL variables which could be used in order to filter or
     * entity, quality, method or unit name.
     * <p>
     * This {@link Map} is indexed by the variables names
     */
    static Map<String, Var> varsByVarName;

    static {

        varsByVarName = new HashMap<>();
        varsByVarName.put(entityLabelVar.getVarName(), entityLabelVar);
        varsByVarName.put(qualityLabelVar.getVarName(), qualityLabelVar);
        varsByVarName.put(methodLabelVar.getVarName(), methodLabelVar);
        varsByVarName.put(unitLabelVar.getVarName(), unitLabelVar);
    }

    /**
     * Read each orderBy of orderByList and then :
     * <pre>
     *     - Append an ORDER BY {@link Expr} into the orderByExprList if the given orderBy field name
     *     is one of {@link #varsByVarName}
     *     - Else append the given orderBy into the unmatchingOrderByList
     * </pre>
     *
     * @param orderByList           the initial {@link OrderBy} list to read
     * @param orderByExprMap        the new list of ORDER BY {@link Expr}.
     *                              An {@link Expr} is created for each {@link OrderBy} which have a field name present in {@link #varsByVarName}
     * @param unmatchingOrderByList the new  list of {@link OrderBy} which doesn't have a field name present in {@link #varsByVarName}
     * @see OrderBy#getFieldName()
     */
    private void appendSpecificOrderBy(List<OrderBy> orderByList, Map<Expr, Order> orderByExprMap, List<OrderBy> unmatchingOrderByList) {

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        for (OrderBy orderBy : orderByList) {
            Var correspondingVar = varsByVarName.get(orderBy.getFieldName());

            if (correspondingVar == null) {
                unmatchingOrderByList.add(orderBy);
            } else {
                Expr orderByExpr = exprFactory.lcase(exprFactory.str(correspondingVar));
                orderByExprMap.put(orderByExpr, orderBy.getOrder());
            }
        }

    }

    /**
     * Search all variables with a name, a long name, an entity name or a quality name
     * corresponding with the given stringPattern.
     *
     * <br></br>
     * <br> The following SPARQL variables are used  : </br>
     * <pre>
     *     _entity_name : the name of the variable entity
     *     _quality_name : the name of the variable quality
     *     _method_name : the name of the variable method
     *     _unit_name : the name of the variable unit
     * </pre>
     * <p>
     * You can use them into the orderByList
     *
     * @param stringPattern the string pattern to search
     * @param orderByList   the {@link List} of {@link OrderBy} to apply on query
     * @param page          the current page
     * @param pageSize      the maximum page size
     * @return the list of {@link VariableModel} founds
     * @see VariableModel#getName()
     * @see VariableModel#getLongName()
     * @see EntityModel#getName()
     * @see QualityModel#getName()
     */
    public ListWithPagination<VariableModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        boolean hasStringFilter = !StringUtils.isEmpty(stringPattern);

        if (!hasStringFilter && CollectionUtils.isEmpty(orderByList)) {
            return sparql.searchWithPagination(
                VariableModel.class,
                null,
                (SelectBuilder select) -> {
                },
                orderByList,
                page,
                pageSize
            );
        }


        Map<Expr, Order> orderByExprMap = new HashMap<>();
        List<OrderBy> newOrderByList = new LinkedList<>();
        appendSpecificOrderBy(orderByList, orderByExprMap, newOrderByList);

        return sparql.searchWithPagination(
            VariableModel.class,
            null,
            (SelectBuilder select) -> {

                ExprFactory exprFactory = select.getExprFactory();
                Expr uriStrRegex = exprFactory.str(exprFactory.asVar(VariableModel.URI_FIELD));

                if (hasStringFilter) {

                    // append string regex matching on entity, quality, method and unit name
                    Expr[] regexExprArray = varsByVarName.values().stream()
                        .map(var -> SPARQLQueryHelper.regexFilter(var.getVarName(), stringPattern))
                        .toArray(Expr[]::new);

                    // set the string regex filter on entity, quality, method, unit  name,long name and URI
                    select.addFilter(
                        SPARQLQueryHelper.or(
                            regexExprArray,
                            SPARQLQueryHelper.regexFilter(VariableModel.NAME_FIELD, stringPattern),
                            SPARQLQueryHelper.regexFilter(VariableModel.LONG_NAME_FIELD_NAME, stringPattern),
                            SPARQLQueryHelper.regexFilter(uriStrRegex, stringPattern, null)
                        ));
                }
                // append specific(s) ORDER BY based on entity, quality, method and unit
                orderByExprMap.forEach(select::addOrderBy);
            },
            newOrderByList,
            page,
            pageSize
        );
    }

}

