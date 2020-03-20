//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.infrastructure.dal;

import java.net.URI;
import java.util.ArrayList;
import static java.util.Collections.list;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.core.ontology.Oeso;
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

    public Set<URI> getUserInfrastructuresURIs(URI userURI, String lang) throws Exception {
        Set<URI> infras = new HashSet<>();

        List<URI> assignedInfra = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            if (userURI != null) {
                select.addFilter(SPARQLQueryHelper.eq(InfrastructureModel.USERS_FIELD, userURI));
            }
        });

        infras.addAll(assignedInfra);
        getInfraChildren(infras, assignedInfra, lang);

        return infras;
    }

    public void getInfraChildren(Set<URI> foundInfra, List<URI> parentsInfra, String lang) throws Exception {
        List<URI> childrenInfra = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            SPARQLQueryHelper.in(
                    select,
                    InfrastructureModel.CHILDREN_FIELD,
                    Oeso.hasChild,
                    InfrastructureModel.URI_FIELD,
                    parentsInfra);
        });

        List<URI> descendentToExplore = new ArrayList<>();

        for (URI childInfra : childrenInfra) {
            if (!foundInfra.contains(childInfra)) {
                descendentToExplore.add(childInfra);
                foundInfra.add(childInfra);
            }
        }

        if (descendentToExplore.size() > 0) {
            getInfraChildren(foundInfra, descendentToExplore, lang);
        }
    }

}
