//******************************************************************************
//                          ExperimentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;


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

    public ExperimentModel update(ExperimentModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void updateWithVariables(URI xpUri, List<URI> variablesUris) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, xpUri)) {
            throw new IllegalArgumentException("Unknown experiment " + xpUri);
        }
        sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(xpUri), xpUri, Oeso.measures, variablesUris);
    }

    public void updateWithSensors(URI xpUri, List<URI> sensorsUris) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, xpUri)) {
            throw new IllegalArgumentException("Unknown experiment " + xpUri);
        }
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), sensorsUris, Oeso.participatesIn, xpUri);
    }

    public void updateWithFactors(URI xpUri, List<URI> factorsUris) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, xpUri)) {
            throw new IllegalArgumentException("Unknown experiment " + xpUri);
        }
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), factorsUris, Oeso.influencedBy, xpUri);
    }

    public void delete(URI xpUri) throws Exception {
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void delete(List<URI> xpUris) throws Exception {
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri) throws Exception {
        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, null);
        if (xp != null) {
            filterExperimentSensors(xp);
        }
        return xp;
    }

    /**
     * Remove all URI from {@link ExperimentModel#getSensors()} method which don't represents a {@link Oeso#SensingDevice}
     * in the SPARQL Graph
     *
     * @param xp the {@link ExperimentModel} to filter
     */
    private void filterExperimentSensors(ExperimentModel xp) {
        if (xp.getSensors().isEmpty())
            return;

        // #TODO don't fetch URI which don't represents sensors and delete this method
        xp.getSensors().removeIf(sensor -> {
            try {
                return !sparql.uriExists(new URI(Oeso.SensingDevice.getURI()), sensor);
            } catch (SPARQLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public ListWithPagination<ExperimentModel> search(
            URI uri,
            String label,
            Integer campaign,
            Boolean isEnded,
            List<URI> variables, List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    appendUriRegexFilter(select,uri);
                    appendRegexLabelFilter(select,label);
                    appendDateFilters(select,isEnded,null,null);
                    appendCampaignFilter(select,campaign);
                    appendVariablesListFilter(select,variables);
                },
                orderByList,
                page,
                pageSize
        );
        for (ExperimentModel xp : xps.getList()) {
            filterExperimentSensors(xp);
        }
        return xps;
    }

    public ListWithPagination<ExperimentModel> search(URI uri,
            Integer campaign,
            String label,
            List<URI> species,
            String startDate, String endDate,
            Boolean isEnded,
            List<URI> projects,
            Boolean isPublic,
            List<URI> groups, boolean admin,
            List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    appendUriRegexFilter(select, uri);
                    appendRegexLabelFilter(select, label);
                    appendCampaignFilter(select, campaign);
                    appendSpeciesFilter(select, species);
                    appendDateFilters(select, isEnded, startDate, endDate);
                    appendGroupsListFilters(select, admin, isPublic, groups);
                    appendProjectListFilter(select, projects);
                },
                orderByList,
                page,
                pageSize
        );
        for (ExperimentModel xp : xps.getList()) {
            filterExperimentSensors(xp);
        }
        return xps;

    }

    private void appendCampaignFilter(SelectBuilder select, Integer campaign) throws Exception {
        if (campaign != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_FIELD, campaign));
        }
    }

    private void appendSpeciesFilter(SelectBuilder select, List<URI> species) throws Exception {
        if (species != null && ! species.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasSpecies, ExperimentModel.SPECIES_FIELD);
            SPARQLQueryHelper.addWhereValues(select, ExperimentModel.SPECIES_FIELD, species);
        }
    }


    private void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.LABEL_FIELD, label));
        }
    }

    private void appendUriRegexFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
    }

    private void appendDateFilters(SelectBuilder select, Boolean ended, String startDate, String endDate) throws Exception {

        if (ended != null) {

            Node endDateVar = NodeFactory.createVariable(ExperimentModel.END_DATE_FIELD);
            Node currentDateNode = SPARQLDeserializers.getForClass(LocalDate.class).getNode(LocalDate.now());

            // an experiment is ended if the end date is less than the the current date
            if (ended) {
                select.addFilter(SPARQLQueryHelper.getExprFactory().le(endDateVar, currentDateNode));
            } else {
                ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
                Expr noEndDateFilter = exprFactory.not(exprFactory.bound(endDateVar));
                select.addFilter(exprFactory.or(noEndDateFilter,exprFactory.gt(endDateVar, currentDateNode)));
            }
        }
        if(startDate == null && endDate == null){
            return;
        }

        LocalDate startLocateDate = startDate == null ? null : LocalDate.parse(startDate);
        LocalDate endLocalDate = endDate == null ? null : LocalDate.parse(endDate);

        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(ExperimentModel.START_DATE_FIELD,startLocateDate,ExperimentModel.END_DATE_FIELD,endLocalDate);
        select.addFilter(dateRangeExpr);
    }

    private void appendProjectListFilter(SelectBuilder select, List<URI> projects) throws Exception {

        if (projects != null && !projects.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasProject, ExperimentModel.PROJECT_URI_FIELD);
            SPARQLQueryHelper.addWhereValues(select, ExperimentModel.PROJECT_URI_FIELD, projects);
        }
    }

    private static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    private void appendGroupsListFilters(SelectBuilder select, boolean admin, Boolean isPublic, List<URI> groups) {

        if (admin) {
            // add no filter on groups for the admin
            return;
        }
        Var groupVar = makeVar(ExperimentModel.GROUP_FIELD);
        Triple groupTriple = new Triple(makeVar(ExperimentModel.URI_FIELD), Oeso.hasGroup.asNode(), groupVar);

        if (CollectionUtils.isEmpty(groups) || (isPublic != null && isPublic)) {
            // get experiment without any group
            select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(groupTriple)));
        } else {
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            // get experiment with no group specified or in the given list
            ElementGroup rootFilteringElem = new ElementGroup();
            ElementGroup optionals = new ElementGroup();
            optionals.addTriplePattern(groupTriple);

            Expr boundExpr = exprFactory.not(exprFactory.bound(groupVar));
            Expr groupInUrisExpr = exprFactory.in(groupVar, groups.stream()
                    .map(uri -> NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString())))
                    .toArray());

            rootFilteringElem.addElement(new ElementOptional(optionals));
            rootFilteringElem.addElementFilter(new ElementFilter(SPARQLQueryHelper.or(boundExpr, groupInUrisExpr)));
            select.getWhereHandler().getClause().addElement(rootFilteringElem);
        }
    }

    private void appendVariablesListFilter(SelectBuilder select, List<URI> variables) throws Exception {
        if (variables != null && !variables.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.measures, ExperimentModel.VARIABLES_FIELD);
            SPARQLQueryHelper.addWhereValues(select, ExperimentModel.VARIABLES_FIELD, variables);
        }
    }

}
