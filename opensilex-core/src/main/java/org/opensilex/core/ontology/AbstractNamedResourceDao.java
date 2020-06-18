package org.opensilex.core.ontology;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.util.List;

public abstract class AbstractNamedResourceDao<T extends SPARQLNamedResourceModel<T>> extends AbstractSparqlDao<T> {

    public AbstractNamedResourceDao(Class<T> objectClass, SPARQLService sparql) {
        super(objectClass, sparql);
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
