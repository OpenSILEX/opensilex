//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal.variable;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.core.AbstractSparqlDao;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.util.List;

/**
 *
 * @author vidalmor
 */
public class BaseVariableDAO<T extends SPARQLNamedResourceModel<T>> extends AbstractSparqlDao<T> {

    public BaseVariableDAO(Class<T> objectClass, SPARQLService sparql) {
        super(objectClass,sparql);
    }

    public ListWithPagination<T> search(String labelPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        Expr labelFilter = SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, labelPattern);
        return sparql.searchWithPagination(
                objectClass,
                null,
                (SelectBuilder select) -> {
                    if (labelFilter != null) {
                        select.addFilter(labelFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }
}
