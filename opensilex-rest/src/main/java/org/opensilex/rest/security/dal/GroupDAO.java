/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.dal;

import java.net.URI;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

public class GroupDAO {
    private SPARQLService sparql;

    public GroupDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public GroupModel create(GroupModel model) throws Exception {
        sparql.create(model);
        return model;
    }
    
    public GroupModel getByURI(URI uri) throws Exception {
        return sparql.getByURI(GroupModel.class, uri);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(GroupModel.class, instanceURI);
    }

    public GroupModel update(GroupModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public ListWithPagination<GroupModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(GroupModel.DESCRIPTION_FIELD, stringPattern),
                SPARQLQueryHelper.regexFilter(GroupModel.NAME_FIELD, stringPattern)
        );

        return sparql.searchWithPagination(
                GroupModel.class,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<GroupModel> getByUserURI(URI uri) throws Exception {
        return sparql.search(
                GroupModel.class,
                (SelectBuilder select) -> {
                    select.addWhere(SPARQLQueryHelper.getUriFieldVar(GroupModel.class), SecurityOntology.hasUser, uri);
                }
        );
    }
}
