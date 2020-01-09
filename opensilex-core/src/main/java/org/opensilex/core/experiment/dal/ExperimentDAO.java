/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentDAO {

    protected final SPARQLService sparql;

    public ExperimentDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ExperimentModel create(ExperimentModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public List<ExperimentModel> createAll(List<ExperimentModel> instances) throws Exception {
        sparql.create(instances);
        return instances;
    }

    public ExperimentModel update(ExperimentModel xp) throws Exception {
        sparql.update(xp);
        return xp;
    }

    /**
     * Update the given experiment in the TripleStore
     *
     * @param xp the experiment to update
     * @param variableUris list of {@link VariableModel} URI
     * @return the updated experiment
     */
    public ExperimentModel updateWithVariableList(ExperimentModel xp, List<URI> variableUris) {
        return xp;
    }

    /**
     * Update the given experiment in the TripleStore
     *
     * @param xp  the experiment to update
     * @param sensorsUris list of sensor URI
     * @return the updated experiment
     */
    public ExperimentModel updateWithSensorList(ExperimentModel xp, List<URI> sensorsUris) {
        return xp;
    }

    public void delete(URI xpUri) throws Exception {
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void deleteAll(List<URI> xpUris) throws Exception {
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri) throws Exception {
        return sparql.getByURI(ExperimentModel.class, xpUri);
    }

    /**
     * @param xpGetDTO DTO which contains a subset of attribute values for an {@link ExperimentModel}
     * @param orderByList an OrderBy List
     * @param page the current page
     * @param pageSize the page size
     * @return the ExperimentModel list
     * @throws Exception
     */
    public ListWithPagination<ExperimentModel> search(ExperimentGetDTO xpGetDTO,
                                                      List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        List<Expr> exprList = new ArrayList<>();
        // add a filter for each non null data/object property

        if(xpGetDTO.getLabel() != null){
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.LABEL_FIELD, xpGetDTO.getLabel()));
        }
        if(xpGetDTO.getUri() != null){
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.URI_FIELD, xpGetDTO.getUri()));
        }
        if(xpGetDTO.getObjective() != null){
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.OBJECTIVE_SPARQL_FIELD, xpGetDTO.getObjective()));
        }
        if(xpGetDTO.getCampaign() != null){
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_SPARQL_FIELD, xpGetDTO.getCampaign()));
        }
        if(xpGetDTO.getComment() != null){
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.COMMENT_SPARQL_FIELD, xpGetDTO.getComment()));
        }

        // let the dateRange method handle the Expr to create according startDate and endDate nullity
        Expr dateExpr = SPARQLQueryHelper.dateRange(ExperimentModel.START_DATE_SPARQL_VAR, xpGetDTO.getStartDate(), ExperimentModel.END_DATE_SPARQL_VAR, xpGetDTO.getEndDate());
        if(dateExpr != null){
            exprList.add(dateExpr);
        }

        return sparql.searchWithPagination(
            ExperimentModel.class,
            (SelectBuilder select) -> {

                exprList.forEach(select::addFilter);

                // add a WHERE { ?var} VALUES { v1 v2} clause for each non empty data/object list property
                if (! xpGetDTO.getKeywords().isEmpty()) {
                    SPARQLQueryHelper.addWhereValues(select, ExperimentModel.KEYWORD_SPARQL_FIELD, xpGetDTO.getKeywords());
                }
                if (! xpGetDTO.getProjects().isEmpty()) {
                    SPARQLQueryHelper.addWhereValues(select, ExperimentModel.PROJECT_URI_SPARQL_VAR, xpGetDTO.getProjects());
                }
                if (!xpGetDTO.getScientificSupervisors().isEmpty()) {
                    SPARQLQueryHelper.addWhereValues(select, ExperimentModel.SCIENTIFIC_SUPERVISOR_SPARQL_VAR, xpGetDTO.getScientificSupervisors());
                }
                if (! xpGetDTO.getTechnicalSupervisors().isEmpty()) {
                    SPARQLQueryHelper.addWhereValues(select, ExperimentModel.TECHNICAL_SUPERVISOR_SPARQL_VAR, xpGetDTO.getTechnicalSupervisors());
                }
                if (! xpGetDTO.getGroups().isEmpty()) {
                    SPARQLQueryHelper.addWhereValues(select, ExperimentModel.GROUP_SPARQL_VAR, xpGetDTO.getGroups());
                }
            },
            orderByList,
            page,
            pageSize
        );
    }
}
