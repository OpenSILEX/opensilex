/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.dal;

import java.net.URI;
import java.util.List;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.rest.security.SecurityOntology;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

public class SecurityProfileModelDAO {

    private SPARQLService sparql;

    public SecurityProfileModelDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public SecurityProfileModel create(SecurityProfileModel model) throws Exception {
        sparql.create(model);
        return model;
    }

    public SecurityProfileModel getByURI(URI uri) throws Exception {
        return sparql.getByURI(SecurityProfileModel.class, uri);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(SecurityProfileModel.class, instanceURI);
    }

    public SecurityProfileModel update(SecurityProfileModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public ListWithPagination<SecurityProfileModel> search(String stringPattern, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.regexFilter(SecurityProfileModel.NAME_FIELD, stringPattern);

        return sparql.searchWithPagination(
                SecurityProfileModel.class,
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

    public List<SecurityProfileModel> getByUserURI(URI uri) throws Exception {
        Var groupUriVar = makeVar("__groupURI");
        Var profileUriVar = SPARQLQueryHelper.getUriFieldVar(SecurityProfileModel.class);
        
        return sparql.search(
                SecurityProfileModel.class,
                (SelectBuilder select) -> {
                    select.addWhere(groupUriVar, SecurityOntology.hasProfile, profileUriVar);
                    select.addWhere(groupUriVar, SecurityOntology.hasUser, uri);
                }
        );
    }
}
