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
import org.jetbrains.annotations.NotNull;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.schemaQuery.SparqlSchema;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaRootNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaSimpleNode;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

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

    public ProjectModel update(ProjectModel instance, AccountModel user) throws Exception {
        sparql.update(instance);
        return instance;
    }

    @Deprecated
    public void update(List<ProjectModel> instances) throws Exception {
        sparql.update(instances);
    }

    public void delete(URI uri, AccountModel user) throws Exception {
        sparql.delete(ProjectModel.class, uri);
    }

    public ProjectModel get(URI uri, AccountModel user) throws Exception {
        return sparql.getByURI(ProjectModel.class, uri, user.getLanguage());
    }

    @Deprecated
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
            Var homepageVar = makeVar(ProjectModel.HOMEPAGE_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(homepageVar);
            filterList.add(SPARQLQueryHelper.regexFilter(strUriExpr, homePage.toString(), null));
        }

        // append regex filter
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.NAME_FIELD, name));

        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.SHORTNAME_FIELD, shortname));
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.DESCRIPTION_FIELD, description));
        filterList.add(SPARQLQueryHelper.regexFilter(ProjectModel.OBJECTIVE_FIELD, objective));

        // append date filters
        if (!StringUtils.isEmpty(startDate)) {
            filterList.add(SPARQLQueryHelper.eq(ProjectModel.START_DATE_FIELD, LocalDate.parse(startDate)));
        }
        if (!StringUtils.isEmpty(endDate)) {
            filterList.add(SPARQLQueryHelper.eq(ProjectModel.END_DATE_FIELD, LocalDate.parse(endDate)));
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
        sparql.create(ProjectModel.class, instances);
    }

    public ListWithPagination<ProjectModel> search(String name, String term, String financialFunding, Integer year, AccountModel user, List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        Expr stringFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(ProjectModel.SHORTNAME_FIELD, name),
                SPARQLQueryHelper.regexFilter(ProjectModel.NAME_FIELD, name)
        );
         Expr termFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.regexFilter(ProjectModel.DESCRIPTION_FIELD, term),
                SPARQLQueryHelper.regexFilter(ProjectModel.OBJECTIVE_FIELD, term)
        );

        Expr financialFundingFilter = SPARQLQueryHelper.regexFilter(ProjectModel.FINANCIAL_FUNDING_FIELD, financialFunding);

       LocalDate startDate ;
        LocalDate endDate;
        if (year != null) {
            String yearString = Integer.toString(year);
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }else {
            startDate=null;
            endDate=null;
        }

        SparqlSchema<ProjectModel> schema = getSparqlSchema();

        return sparql.searchWithPaginationUsingSchema(
                ProjectModel.class,
                null,
                (SelectBuilder select) -> {
                    if (stringFilter != null) {
                        select.addFilter(stringFilter);
                    }
                    
                    if (termFilter != null) {
                        select.addFilter(termFilter);
                    }

                    if (financialFundingFilter != null) {
                        select.addFilter(financialFundingFilter);
                    }

                    appendDateFilters(select, startDate, endDate);
                },
                schema,
                orderByList,
                page,
                pageSize
        );
    }

    private SparqlSchema<ProjectModel> getSparqlSchema() throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        SparqlSchemaRootNode<ProjectModel> rootNode = new SparqlSchemaRootNode<>(
                sparql,
                ProjectModel.class,
                List.of(
                        new SparqlSchemaSimpleNode<>(PersonModel.class, ProjectModel.ADMINISTRATIVE_CONTACTS_FIELD),
                        new SparqlSchemaSimpleNode<>(PersonModel.class, ProjectModel.SCIENTIFIC_CONTACTS_FIELD),
                        new SparqlSchemaSimpleNode<>(PersonModel.class, ProjectModel.COORDINATORS_FIELD),
                        new SparqlSchemaSimpleNode<>(ProjectModel.class, ProjectModel.RELATED_PROJECTS_FIELD)
                ),
                false
        );

        SparqlSchema<ProjectModel> schema = new SparqlSchema<>(rootNode);
        return schema;
    }

    private void appendDateFilters(SelectBuilder select, LocalDate startDate, LocalDate endDate) throws Exception {
        

        if (startDate != null && endDate != null) {

            Expr dateRangeExpr = SPARQLQueryHelper.intervalDateRange(ProjectModel.START_DATE_FIELD, startDate, ProjectModel.END_DATE_FIELD, endDate);
            select.addFilter(dateRangeExpr);
        } else {
            if (startDate != null || endDate != null) {
                Expr dateRangeExpr = SPARQLQueryHelper.dateRange(ProjectModel.START_DATE_FIELD, startDate, ProjectModel.END_DATE_FIELD, endDate);
                select.addFilter(dateRangeExpr);
            }
        }

    }

    public List<ProjectModel> getList(List<URI> uris, AccountModel user) throws Exception {
        return sparql.getListByURIs(ProjectModel.class, uris, user.getLanguage());
    }
}
