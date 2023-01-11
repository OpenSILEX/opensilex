/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.dal;

import java.net.URI;
import java.util.List;


import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vidalmor
 */
public final class GroupDAO {

    private final SPARQLService sparql;

    public GroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public GroupModel create(GroupModel group) throws Exception {
        sparql.create(group);
        return group;
    }

    public GroupModel get(URI uri) throws Exception {
        return sparql.getByURI(GroupModel.class, uri, null);
    }

    public void delete(URI groupURI) throws Exception {
        sparql.delete(GroupModel.class, groupURI);
    }

    public GroupModel update(GroupModel group) throws Exception {
        sparql.update(group);
        return group;
    }

    public List<URI> getGroupUriList(AccountModel user) throws Exception {

        return sparql.searchURIs(GroupModel.class, null, (SelectBuilder select) -> {
            Var groupUriVar = sparql.getURIFieldVar(GroupModel.class);
            Var groupUserProfileVar = makeVar("gup");
            select.addWhere(groupUriVar, SecurityOntology.hasUserProfile, groupUserProfileVar);
            select.addWhere(groupUserProfileVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(user.getUri()));
        });

    }

    public ListWithPagination<GroupModel> search(String namePattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(GroupModel.NAME_FIELD, namePattern);

        return sparql.searchWithPagination(
                GroupModel.class,
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

    public List<GroupModel> getUserGroups(URI userURI) throws Exception {

        Var userProfilesVar = makeVar(GroupModel.USER_PROFILES_FIELD);
        SPARQLClassObjectMapper<GroupModel> mapper = sparql.getForClass(GroupModel.class);
        return sparql.search(
                GroupModel.class,
                null,
                (SelectBuilder select) -> {
                    select.addWhere(mapper.getURIFieldVar(), SecurityOntology.hasUserProfile, userProfilesVar);
                    select.addWhere(userProfilesVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(userURI));
                }
        );
    }

    public boolean groupNameExists(String name) throws Exception {
        return sparql.existsByUniquePropertyValue(
                GroupModel.class,
                RDFS.label,
                name
        );
    }

    public List<GroupModel> getList(List<URI> uri) throws Exception {
        return sparql.getListByURIs(GroupModel.class, uri, null);
    }
}
