//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.util.List;

/**
 * @author vidalmor
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    public VariableDAO(SPARQLService sparql) {
        super(VariableModel.class, sparql);
    }

    static Var entityLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("entity"));
    static Var qualityLabelVar =  SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("quality"));
    static Var methodLabelVar =  SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("method"));
    static Var unitLabelVar =  SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName("unit"));

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
     *
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

        return sparql.searchWithPagination(
                VariableModel.class,
                null,
                (SelectBuilder select) -> {

                    ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
                    Expr uriStrRegex = exprFactory.str(exprFactory.asVar(VariableModel.URI_FIELD));

                    if (hasStringFilter) {
                        select.addFilter(
                            SPARQLQueryHelper.or(
                                SPARQLQueryHelper.regexFilter(VariableModel.NAME_FIELD, stringPattern),
                                SPARQLQueryHelper.regexFilter(VariableModel.LONG_NAME_FIELD_NAME, stringPattern),
                                SPARQLQueryHelper.regexFilter(uriStrRegex,stringPattern,null),
                                SPARQLQueryHelper.regexFilter(entityLabelVar.getVarName(), stringPattern),
                                SPARQLQueryHelper.regexFilter(qualityLabelVar.getVarName(), stringPattern),
                                SPARQLQueryHelper.regexFilter(methodLabelVar.getVarName(), stringPattern),
                                SPARQLQueryHelper.regexFilter(unitLabelVar.getVarName(), stringPattern)
                            ));
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

}

