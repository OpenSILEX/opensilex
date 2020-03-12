//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;

/**
 * @author vidalmor
 */
public class ProjectDAO {

    protected final SPARQLService sparql;
    protected final String lang;

    public ProjectDAO(SPARQLService sparql, String lang) {
        this.sparql = sparql;
        this.lang = lang;
    }

    public ProjectModel create(ProjectModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public ProjectModel update(ProjectModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void update(List<ProjectModel> instances) throws Exception {
        sparql.update(instances);
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ProjectModel.class, instanceURI);
    }

    public ProjectModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(ProjectModel.class, instanceURI, lang);
    }

    public ListWithPagination<ProjectModel> search(URI uri,
                                                   String name, String shortname, String description, String startDate, String endDate, URI homePage, String objective,
                                                   List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        List<Expr> filterList = new ArrayList<>();

        // append uri regex filter
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            filterList.add(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(),null));
        }
        if (homePage != null) {
            Var homepageVar = makeVar(ProjectModel.HOMEPAGE_SPARQL_VAR);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(homepageVar);
            filterList.add(SPARQLQueryHelper.regexFilter(strUriExpr,homePage.toString(),null));
        }

        // append regex filter
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.NAME_SPARQL_VAR, name));
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.SHORTNAME_SPARQL_VAR, shortname));
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.DESCRIPTION_SPARQL_VAR, description));
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.OBJECTIVE_SPARQL_VAR, objective));

        // append date filters
        if (!StringUtils.isEmpty(startDate)) {
            filterList.add(SPARQLQueryHelper.eq(ProjectModel.START_DATE_SPARQL_VAR, LocalDate.parse(startDate)));
        }
        if (!StringUtils.isEmpty(endDate)) {
            filterList.add(SPARQLQueryHelper.eq(ProjectModel.END_DATE_SPARQL_VAR, LocalDate.parse(endDate)));
        }

        return sparql.searchWithPagination(
                ProjectModel.class,
                lang,
                (SelectBuilder select) -> {
                   filterList.stream().filter(Objects::nonNull).forEach(select::addFilter);
                },
                orderByList,
                page,
                pageSize
        );
    }

    public void create(List<ProjectModel> instances) throws Exception {
        sparql.create(instances);
    }

    public ListWithPagination<ProjectModel> search(List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        return sparql.searchWithPagination(
                ProjectModel.class,
                lang,
                (SelectBuilder select) -> {
                    // TODO implements filters
                },
                orderByList,
                page,
                pageSize
        );
    }
}
