//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.profile.dal;

import java.net.URI;
import java.util.List;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vincent
 */
public class ProfileDAO {

    private SPARQLService sparql;

    public ProfileDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ProfileModel create(
            URI uri,
            String name,
            List<String> credentials
    ) throws Exception {
        ProfileModel profile = new ProfileModel();
        profile.setUri(uri);
        profile.setName(name);
        profile.setCredentials(credentials);

        sparql.create(profile);

        return profile;
    }

    public ProfileModel get(URI uri) throws Exception {
        return sparql.getByURI(ProfileModel.class, uri, null);
    }

    public void delete(URI instanceURI) throws Exception {
        try {
            sparql.startTransaction();
            // Delete existing user profile group relations
            sparql.deleteByObjectRelation(GroupUserProfileModel.class, GroupUserProfileModel.PROFILE_FIELD, instanceURI);
            // Delete user
            sparql.delete(ProfileModel.class, instanceURI);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            throw ex;
        }
    }

    public ProfileModel update(
            URI uri,
            String name,
            List<String> credentials
    ) throws Exception {
        ProfileModel profile = new ProfileModel();
        profile.setUri(uri);
        profile.setName(name);
        profile.setCredentials(credentials);

        sparql.update(profile);

        return profile;
    }

    public ListWithPagination<ProfileModel> search(String namePattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(ProfileModel.NAME_FIELD, namePattern);

        return sparql.searchWithPagination(
                ProfileModel.class,
                null,
                (SelectBuilder select) -> {
                    if (nameFilter != null) {
                        select.addFilter(nameFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<ProfileModel> getAll(List<OrderBy> orderByList) throws Exception {
        return sparql.search(
                ProfileModel.class,
                null,
                orderByList
        );
    }

    public List<ProfileModel> getByUserURI(URI uri) throws Exception {
        return sparql.search(
                ProfileModel.class,
                null,
                (SelectBuilder select) -> {
                    SPARQLClassObjectMapper<SPARQLResourceModel> profileMapper = SPARQLClassObjectMapper.getForClass(ProfileModel.class);
                    Var profileURIVar = profileMapper.getURIFieldVar();
                    Var groupURIVar = makeVar("__userProfileURI");
                    WhereHandler whereHandler = new WhereHandler();
                    whereHandler.addWhere(select.makeTriplePath(groupURIVar, SecurityOntology.hasProfile, profileURIVar));
                    whereHandler.addWhere(select.makeTriplePath(groupURIVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(uri)));

                    SPARQLClassObjectMapper<GroupUserProfileModel> groupUserProfileMapper = SPARQLClassObjectMapper.getForClass(GroupUserProfileModel.class);
                    ElementNamedGraph elementNamedGraph = new ElementNamedGraph(groupUserProfileMapper.getDefaultGraph(), whereHandler.getElement());
                    select.getWhereHandler().getClause().addElement(elementNamedGraph);
                }
        );
    }

    public boolean profileNameExists(String name) throws Exception {
        return sparql.existsByUniquePropertyValue(
                ProfileModel.class,
                DCTerms.title,
                name
        );
    }
}
