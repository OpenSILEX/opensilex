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
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
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

    public SPARQLTreeListModel<InfrastructureModel> searchTree(String pattern, UserModel user) throws Exception {
        Set<URI> infras = getUserInfrastructures(user);
        if (infras != null && infras.isEmpty()) {
            return new SPARQLTreeListModel<>();
        }

        return sparql.searchResourceTree(
                InfrastructureModel.class,
                user.getLanguage(),
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

    public Set<URI> getUserInfrastructures(UserModel user) throws Exception {
        if (user == null || user.isAdmin()) {
            return null;
        }

        String lang = user.getLanguage();
        Set<URI> userInfras = new HashSet<>();
        List<URI> infras = sparql.searchURIs(InfrastructureModel.class, lang, (SelectBuilder select) -> {
            Var userProfileVar = makeVar("_userProfile");
            select.addWhere(makeVar(InfrastructureModel.GROUP_FIELD), SecurityOntology.hasUserProfile, userProfileVar);
            select.addWhere(userProfileVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(user.getUri()));
            select.addFilter(SPARQLQueryHelper.eq(InfrastructureModel.GROUP_FIELD, user.getUri()));
        });

        userInfras.addAll(infras);

        return userInfras;
    }

    public InfrastructureModel create(InfrastructureModel instance) throws Exception {
        sparql.create(instance);

        return instance;
    }

    public InfrastructureModel get(URI uri, String lang) throws Exception {
        return sparql.getByURI(InfrastructureModel.class, uri, lang);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(InfrastructureModel.class, instanceURI);
    }

    public InfrastructureModel update(InfrastructureModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public InfrastructureDeviceModel createDevice(InfrastructureDeviceModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public InfrastructureDeviceModel getDevice(URI uri, String lang) throws Exception {
        return sparql.getByURI(InfrastructureDeviceModel.class, uri, lang);
    }

    public void deleteDevice(URI instanceURI) throws Exception {
        sparql.delete(InfrastructureDeviceModel.class, instanceURI);
    }

    public InfrastructureDeviceModel updateDevice(InfrastructureDeviceModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }
}
