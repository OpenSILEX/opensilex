//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.infrastructure.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vidalmor
 */
public class InfrastructureDAO {

    protected final SPARQLService sparql;

    public InfrastructureDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ListWithPagination<InfrastructureModel> search(String pattern, URI parent, URI userURI, String lang, List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        return sparql.searchWithPagination(
                InfrastructureModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (pattern != null) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(InfrastructureModel.NAME_FIELD, pattern));
                    }

                    if (parent != null) {
                        select.addFilter(SPARQLQueryHelper.eq(InfrastructureModel.PARENT_FIELD, parent));
                    }

                    if (userURI != null) {
                        select.addFilter(SPARQLQueryHelper.eq(InfrastructureModel.USERS_FIELD, userURI));
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

}
