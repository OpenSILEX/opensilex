/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.dal;

import java.net.URI;
import java.util.List;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vidalmor
 */
public class GroupDAO {

    private SPARQLService sparql;

    public GroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public GroupModel create(GroupModel group) throws Exception {
        sparql.startTransaction();
        try {
            sparql.create(group.getUserProfiles());
            sparql.create(group);
            sparql.commitTransaction();
            return group;
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            throw ex;
        }

    }

    public GroupModel get(URI uri) throws Exception {
        return sparql.getByURI(GroupModel.class, uri, null);
    }

    private List<URI> getGroupUserProfileURIsList(URI groupURI) throws Exception {
        List<URI> userProfilesURIs = sparql.searchURIs(GroupUserProfileModel.class, null, (SelectBuilder select) -> {
            WhereHandler whereHandler = new WhereHandler();
            Node groupUriNode = SPARQLDeserializers.nodeURI(groupURI);
            whereHandler.addWhere(select.makeTriplePath(groupUriNode, SecurityOntology.hasUserProfile, SPARQLClassObjectMapper.getForClass(GroupModel.class).getURIFieldVar()));
            Node graph = SPARQLClassObjectMapper.getForClass(GroupModel.class).getDefaultGraph();
            ElementNamedGraph elementNamedGraph = new ElementNamedGraph(graph, whereHandler.getElement());
            select.getWhereHandler().getClause().addElement(elementNamedGraph);
        });

        return userProfilesURIs;
    }

    public void delete(URI groupURI) throws Exception {
        sparql.startTransaction();
        try {
            List<URI> userProfilesURIs = getGroupUserProfileURIsList(groupURI);
            sparql.delete(GroupModel.class, groupURI);
            sparql.delete(GroupUserProfileModel.class, userProfilesURIs);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            throw ex;
        }
    }

    public GroupModel update(GroupModel group) throws Exception {
        sparql.startTransaction();
        try {
            List<URI> userProfilesURIs = getGroupUserProfileURIsList(group.getUri());
            sparql.delete(GroupUserProfileModel.class, userProfilesURIs);
            Node graph = SPARQLClassObjectMapper.getForClass(GroupModel.class).getDefaultGraph();
            sparql.deleteObjectRelations(graph, group.getUri(), SecurityOntology.hasUserProfile, userProfilesURIs);
            sparql.create(group.getUserProfiles());
            sparql.update(group);
            sparql.commitTransaction();
            return group;
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            throw ex;
        }

    }

    public List<URI> getGroupUriList(UserModel user) throws Exception {

        return sparql.searchURIs(GroupModel.class, null, (SelectBuilder select) -> {
            Var groupUriVar = SPARQLClassObjectMapper.getForClass(GroupModel.class).getURIFieldVar();
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
}
