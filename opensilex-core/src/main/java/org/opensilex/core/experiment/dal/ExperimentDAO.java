/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

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
     * @param xpUri        the experiment to update URI
     * @param variableUris list of {@link VariableModel} URI
     * @return the updated experiment
     */
    public ExperimentModel updateWithVariableList(URI xpUri, List<URI> variableUris) throws Exception {
        return get(xpUri);
    }

    /**
     * Update the given experiment in the TripleStore
     *
     * @param xpUri       the experiment to update URI
     * @param sensorsUris list of sensor URI
     * @return the updated experiment
     */
    public ExperimentModel updateWithSensorList(URI xpUri, List<URI> sensorsUris) throws Exception {
        return get(xpUri);
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
     * @param searchDTO
     * @return
     * @throws Exception
     */
    protected List<Expr> extractFilters(ExperimentSearchDTO searchDTO) throws Exception {

        if (searchDTO == null)
            return Collections.emptyList();

        List<Expr> exprList = new ArrayList<>();

        // build equality filters
        if (searchDTO.getUri() != null) {
            exprList.add(SPARQLQueryHelper.eq(SPARQLResourceModel.URI_FIELD, searchDTO.getUri()));
        }
        if (searchDTO.getCampaign() != null) {
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_SPARQL_FIELD, searchDTO.getCampaign()));
        }

        // build regex based filter
        if (searchDTO.getObjective() != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.OBJECTIVE_SPARQL_FIELD, searchDTO.getObjective()));
        }
        if (searchDTO.getLabel() != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.LABEL_FIELD, searchDTO.getLabel()));
        }
        if (searchDTO.getComment() != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(ExperimentModel.COMMENT_SPARQL_FIELD, searchDTO.getComment()));
        }

        Boolean isEnded = searchDTO.isEnded();
        if (isEnded != null) {

            Node endDateVar = NodeFactory.createVariable(ExperimentModel.END_DATE_SPARQL_VAR);
            Node currentDateNode = SPARQLDeserializers.getForClass(LocalDate.class).getNode(LocalDate.now());

            // an experiment is ended if the end date is less than the the current date
            if (isEnded) {
                exprList.add(SPARQLQueryHelper.getExprFactory().le(endDateVar, currentDateNode));
            } else {
                exprList.add(SPARQLQueryHelper.getExprFactory().gt(endDateVar, currentDateNode));
            }
        }

        // get an Expr build according startDate and endDate
        Expr dateExpr = SPARQLQueryHelper.dateRange(
            ExperimentModel.START_DATE_SPARQL_VAR, searchDTO.getStartDate(),
            ExperimentModel.END_DATE_SPARQL_VAR, searchDTO.getEndDate()
        );
        if (dateExpr != null) {
            exprList.add(dateExpr);
        }

        return exprList;
    }

    /**
     * @param searchDTO
     * @return for each attribute variable name, the list of values to put in the VALUES set
     */
    protected Map<String, List<?>> getValuesByVarName(ExperimentSearchDTO searchDTO) {

        if (searchDTO == null) {
            return Collections.emptyMap();
        }

        Map<String, List<?>> valuesByVar = new HashMap<>();

        if (!searchDTO.getKeywords().isEmpty()) {
            valuesByVar.put(ExperimentModel.KEYWORD_SPARQL_FIELD, searchDTO.getKeywords());
        }
        if (!searchDTO.getProjects().isEmpty()) {
            valuesByVar.put(ExperimentModel.PROJECT_URI_SPARQL_VAR, searchDTO.getProjects());
        }
        if(!searchDTO.getSpecies().isEmpty()){
            valuesByVar.put(ExperimentModel.SPECIES_SPARQL_FIELD,searchDTO.getSpecies());
        }
        if (!searchDTO.getScientificSupervisors().isEmpty()) {
            valuesByVar.put(ExperimentModel.SCIENTIFIC_SUPERVISOR_SPARQL_VAR, searchDTO.getScientificSupervisors());
        }
        if (!searchDTO.getTechnicalSupervisors().isEmpty()) {
            valuesByVar.put(ExperimentModel.TECHNICAL_SUPERVISOR_SPARQL_VAR, searchDTO.getTechnicalSupervisors());
        }
        if (!searchDTO.getGroups().isEmpty()) {
            valuesByVar.put(ExperimentModel.GROUP_SPARQL_VAR, searchDTO.getGroups());
        }
        return valuesByVar;
    }

    /**
     * @param searchDTO   DTO which contains a subset of attribute values for an {@link ExperimentModel}
     * @param orderByList an OrderBy List
     * @param page        the current page
     * @param pageSize    the page size
     * @return the ExperimentModel list
     * @throws Exception
     */
    public ListWithPagination<ExperimentModel> search(ExperimentSearchDTO searchDTO, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                ExperimentModel.class,
                (SelectBuilder select) -> {

                    // add a filter for each non null data/object property
                    extractFilters(searchDTO).forEach(select::addFilter);

                    // add a WHERE { ?var} VALUES { v1 v2} clause for each non empty data/object list property
                    getValuesByVarName(searchDTO).forEach((var, values) -> {
                        try {
                            SPARQLQueryHelper.addWhereValues(select, var, values);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                },
                orderByList,
                page,
                pageSize
        );
    }
}
