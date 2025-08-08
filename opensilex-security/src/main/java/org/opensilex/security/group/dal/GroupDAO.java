/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.dal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.api.GroupDTO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.profile.dal.ProfileDAO;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.schemaQuery.SparqlSchema;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaRootNode;
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

    public ListWithPagination<GroupModel> search(String namePattern, String lang, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(GroupModel.NAME_FIELD, namePattern);

        SparqlSchemaNode<ProfileModel> profileNode = new SparqlSchemaNode<>(
                ProfileModel.class,
                GroupUserProfileModel.PROFILE_FIELD,
                new ArrayList<>(),
                false,
                false
        );

        SparqlSchemaNode<PersonModel> personNode = new SparqlSchemaNode<>(
                PersonModel.class,
                AccountModel.LINKED_PERSON_FIELD,
                new ArrayList<>(),
                false,
                false
        );

        SparqlSchemaNode<AccountModel> accountNode = new SparqlSchemaNode<>(
                AccountModel.class,
                GroupUserProfileModel.USER_FIELD,
                Collections.singletonList(personNode),
                false,
                false
        );

        SparqlSchemaNode<GroupUserProfileModel> groupUserProfileNode = new SparqlSchemaNode<>(
                GroupUserProfileModel.class,
                GroupModel.USER_PROFILES_FIELD,
                List.of(profileNode, accountNode),
                true,
                false
        );

        SparqlSchemaRootNode<GroupModel> rootNode = new SparqlSchemaRootNode<>(
                GroupModel.class,
                Collections.singletonList(groupUserProfileNode),
                false
        );

        SparqlSchema<GroupModel> schema = new SparqlSchema<>(rootNode);

        ListWithPagination<GroupModel> models = sparql.searchWithPaginationUsingSchema(
                sparql.getDefaultGraph(GroupModel.class),
                GroupModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (nameFilter != null) {
                        select.addFilter(nameFilter);
                    }
                },
                Collections.emptyMap(),
                schema,
                orderByList,
                page,
                pageSize
        );

        return models;
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
