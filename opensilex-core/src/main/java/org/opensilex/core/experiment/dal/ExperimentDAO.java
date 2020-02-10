//******************************************************************************
//                          ExperimentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
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
        checkURIs(instance);
        sparql.create(instance);
        return instance;
    }
    
    public void createAll(List<ExperimentModel> instances) throws Exception {
        sparql.create(instances);
    }

    public ExperimentModel update(ExperimentModel instance) throws Exception {
        checkURIs(instance);
        sparql.update(instance);
        return instance;
    }

    /**
     * Check that all URI(s) which refers to a non {@link org.opensilex.sparql.annotations.SPARQLResource}-compliant model exists.
     * @param model the experiment for which we check if all URI(s) exists
     * @throws SPARQLException
     * @throws IllegalArgumentException if the given model contains a unknown URI
     */
    protected void checkURIs(ExperimentModel model) throws SPARQLException, IllegalArgumentException {

        if(model.getSpecies() != null && ! sparql.uriExists(model.getSpecies())){
            throw new IllegalArgumentException("Trying to insert an experiment with an unknown species : "+model.getSpecies());
        }
        for(URI infraUri : model.getInfrastructures()){
            if(! sparql.uriExists(infraUri)){
                throw new IllegalArgumentException("Trying to insert an experiment with an unknown infrastructure : "+infraUri);
            }
        }

    }

    public void delete(URI xpUri) throws Exception {
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void delete(List<URI> xpUris) throws Exception {
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri) throws Exception {
        return sparql.getByURI(ExperimentModel.class, xpUri);
    }

    /**
     * @param searchDTO a search DTO which contains all attributes about an {@link ExperimentModel} search
     * @return the list of {@link Expr} extracted from the given searchDTO.
     * @throws Exception
     * @see SPARQLQueryHelper the utility class used to build Expr
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
        if(searchDTO.getSpecies() != null){
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.SPECIES_SPARQL_FIELD, searchDTO.getSpecies()));
        }
        if(searchDTO.getIsPublic() != null){
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.IS_PUBLIC_SPARQL_VAR, searchDTO.getIsPublic()));
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

        if (searchDTO.getStartDate() != null) {
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.START_DATE_SPARQL_VAR, LocalDate.parse(searchDTO.getStartDate())));
        }
        if (searchDTO.getEndDate() != null) {
            exprList.add(SPARQLQueryHelper.eq(ExperimentModel.END_DATE_SPARQL_VAR,  LocalDate.parse(searchDTO.getEndDate())));
        }

        // get an Expr build according startDate and endDate
//        LocalDate startDate = searchDTO.getStartDate() != null ? LocalDate.parse(searchDTO.getStartDate()) : null;
//        LocalDate endDate = searchDTO.getEndDate() != null ? LocalDate.parse(searchDTO.getEndDate()) : null;
//
//        Expr dateExpr = SPARQLQueryHelper.dateRange(ExperimentModel.START_DATE_SPARQL_VAR, startDate, ExperimentModel.END_DATE_SPARQL_VAR, endDate);
//        if (dateExpr != null) {
//            exprList.add(dateExpr);
//        }

        return exprList;
    }

    /**
     * @param searchDTO a search DTO which contains all attributes about an {@link ExperimentModel} search
     * @return a {@link Map} between SPARQL variable name and the list of values for this variable.
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
        if (!searchDTO.getScientificSupervisors().isEmpty()) {
            valuesByVar.put(ExperimentModel.SCIENTIFIC_SUPERVISOR_SPARQL_VAR, searchDTO.getScientificSupervisors());
        }
        if (!searchDTO.getTechnicalSupervisors().isEmpty()) {
            valuesByVar.put(ExperimentModel.TECHNICAL_SUPERVISOR_SPARQL_VAR, searchDTO.getTechnicalSupervisors());
        }
        if (!searchDTO.getGroups().isEmpty()) {
            valuesByVar.put(ExperimentModel.GROUP_SPARQL_VAR, searchDTO.getGroups());
        }
        if(! searchDTO.getVariables().isEmpty()){
            valuesByVar.put(ExperimentModel.VARIABLES_SPARQL_VAR, searchDTO.getVariables());
        }
        if(! searchDTO.getSensors().isEmpty()){
            valuesByVar.put(ExperimentModel.SENSORS_SPARQL_VAR, searchDTO.getSensors());
        }
        if(! searchDTO.getInfrastructures().isEmpty()){
            valuesByVar.put(ExperimentModel.INFRASTRUCTURE_SPARQL_VAR, searchDTO.getInfrastructures());
        }
        if(! searchDTO.getInstallations().isEmpty()){
            valuesByVar.put(ExperimentModel.DISPOSITIVES_SPARQL_VAR, searchDTO.getInstallations());
        }
        return valuesByVar;
    }

    /**
     * @param searchDTO a search DTO which contains all attributes about an {@link ExperimentModel} search
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
