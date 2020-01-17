/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author vidalmor
 */
public class GroupDAO {

    private SPARQLService sparql;

    public GroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public GroupModel create(GroupModel group) throws Exception {
        sparql.create(group);
        return group;
    }

    public GroupModel get(URI uri) throws Exception {
        return sparql.getByURI(GroupModel.class, uri);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(GroupModel.class, instanceURI);
    }

    public GroupModel update(GroupModel group) throws Exception {
        sparql.update(group);
        return group;
    }

    public ListWithPagination<GroupModel> search(String namePattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr nameFilter = SPARQLQueryHelper.regexFilter(GroupModel.NAME_FIELD, namePattern);

        return sparql.searchWithPagination(
                GroupModel.class,
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
