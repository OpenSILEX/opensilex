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
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @author vidalmor
 */
public class ProjectDAO {

    protected final SPARQLService sparql;

    public ProjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
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

    public ProjectModel get(URI instanceURI, String lang) throws Exception {
        return sparql.getByURI(ProjectModel.class, instanceURI, lang);
    }

    public ListWithPagination<ProjectModel> search(URI uri,
            String name, String shortname, String description, String startDate, String endDate, URI homePage, String objective,
            List<OrderBy> orderByList, Integer page, Integer pageSize, String lang) throws Exception {

        List<Expr> filterList = new ArrayList<>();

        // append uri regex filter
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            filterList.add(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
        if (homePage != null) {
            Var homepageVar = makeVar(ProjectModel.HOMEPAGE_SPARQL_VAR);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(homepageVar);
            filterList.add(SPARQLQueryHelper.regexFilter(strUriExpr, homePage.toString(), null));
        }

        // append regex filter
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.LABEL_VAR, name));

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

    public ListWithPagination<ProjectModel> search(List<OrderBy> orderByList, int page, int pageSize, String lang) throws Exception {
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

    public ListWithPagination<ProjectModel> search(String shortname, String label, String financial, String startDate, String endDate, List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        return sparql.searchWithPagination(
                ProjectModel.class,
                null,
                (SelectBuilder select) -> {
                    appendRegexShortnameOrLabelFilter(select, shortname, label);
                    appendRegexFinancialFundingFilter(select, financial);
                    appendIntervalDateFilters(select, startDate, endDate);
                },
                orderByList,
                page,
                pageSize
        );
    }

    protected void appendRegexShortnameOrLabelFilter(SelectBuilder select, String shortname, String label) {
        if (!StringUtils.isEmpty(shortname) && !StringUtils.isEmpty(label)) {
            Expr shortnameRegexExpr = SPARQLQueryHelper.regexFilter(ProjectModel.SHORTNAME_SPARQL_VAR, shortname);
            Expr labelRegexExpr = SPARQLQueryHelper.regexFilter(ProjectModel.LABEL_VAR, label);
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
            select.addFilter(exprFactory.or(shortnameRegexExpr, labelRegexExpr));
        } else if (!StringUtils.isEmpty(shortname)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ProjectModel.SHORTNAME_SPARQL_VAR, shortname));
        } else if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ProjectModel.LABEL_VAR, label));
        }
    }

    protected void appendRegexFinancialFundingFilter(SelectBuilder select, String financial) {
        if (!StringUtils.isEmpty(financial)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ProjectModel.FINANCIALFUNDING_SPARQL_VAR, financial));
        }
    }

    protected void appendIntervalDateFilters(SelectBuilder select, String startDate, String endDate) throws Exception {

        if (startDate == null || endDate == null) {
            return;
        }

        LocalDate startLocateDate = LocalDate.parse(startDate);
        LocalDate endLocalDate = LocalDate.parse(endDate);

        Expr dateRangeExpr = SPARQLQueryHelper.intervalDateRange(ExperimentModel.START_DATE_SPARQL_VAR, startLocateDate, ExperimentModel.END_DATE_SPARQL_VAR, endLocalDate);
        select.addFilter(dateRangeExpr);
    }

}
