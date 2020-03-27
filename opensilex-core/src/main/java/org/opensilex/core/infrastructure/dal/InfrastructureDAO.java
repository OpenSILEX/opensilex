//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.infrastructure.dal;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.tree.ResourceTree;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLQueryHelper;

/**
 * @author vidalmor
 */
public class InfrastructureDAO {

    protected final SPARQLService sparql;

    public InfrastructureDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ResourceTree<InfrastructureModel> searchTree(String pattern, UserModel user, String lang) throws Exception {
        Set<URI> infras = getUserInfrastructures(user, lang);
        if (infras != null && infras.isEmpty()) {
            return new ResourceTree<>();
        }

        return sparql.searchResourceTree(
                InfrastructureModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (pattern != null && !pattern.isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(InfrastructureModel.NAME_FIELD, pattern));
                    }

                    if (infras != null) {
                        SPARQLQueryHelper.inURI(select, InfrastructureModel.URI_FIELD, infras);
                    }
                }
        );
    }

    private Set<URI> getUserInfrastructures(UserModel user, String lang) throws Exception {
        if (user == null || user.isAdmin()) {
            return null;
        }

        Set<URI> userInfras = new HashSet<>();
        List<URI> infras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            select.addFilter(SPARQLQueryHelper.eq(InfrastructureModel.USERS_FIELD, user.getUri()));
        });

        userInfras.addAll(infras);

        getAlldescendants(userInfras, infras, lang);

        getAllParents(userInfras, infras, lang);

        return userInfras;
    }

    private void getAlldescendants(Set<URI> userInfras, List<URI> parentsInfra, String lang) throws Exception {
        List<URI> childrenInfras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            SPARQLQueryHelper.inProperty(select, InfrastructureModel.PARENT_FIELD, Oeso.hasPart, InfrastructureModel.URI_FIELD, parentsInfra);
        });

        userInfras.addAll(childrenInfras);
        if (childrenInfras.size() > 0) {
            getAlldescendants(userInfras, childrenInfras, lang);
        }

    }

    private void getAllParents(Set<URI> userInfras, List<URI> parentsInfra, String lang) throws Exception {
        List<URI> parentsInfras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            SPARQLQueryHelper.inProperty(select, InfrastructureModel.URI_FIELD, Oeso.hasPart, InfrastructureModel.CHILDREN_FIELD, parentsInfra);
        });

        userInfras.addAll(parentsInfras);
        if (parentsInfras.size() > 0) {
            getAllParents(userInfras, parentsInfras, lang);
        }
    }

    public InfrastructureModel create(InfrastructureModel instance) throws Exception {
        sparql.create(instance);

        return instance;
    }

    public InfrastructureModel get(URI uri) throws Exception {
        return sparql.getByURI(InfrastructureModel.class, uri, null);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(InfrastructureModel.class, instanceURI);
    }

    public InfrastructureModel update(InfrastructureModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }
}
