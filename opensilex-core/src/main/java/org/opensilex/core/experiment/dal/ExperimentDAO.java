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
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        checkURIs(instance);
        sparql.create(instance);
        return instance;
    }

    public ExperimentModel update(ExperimentModel instance) throws Exception {
        checkURIs(instance);
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

    /**
     * check if all URI from uris have the typeResource as {@link RDF#type} into the SPARQL graph
     *
     * @param uris         the {@link List} of {@link URI} to check
     * @param typeResource the {@link Resource} indicating the {@link RDF#type
     */
    protected void checkURIs(List<URI> uris, Resource typeResource) throws URISyntaxException, SPARQLException {

        if (uris == null || uris.isEmpty()) {
            return;
        }
        for (URI uri : uris) {
            if (!sparql.uriExists(new URI(typeResource.getURI()), uri)) {
                throw new IllegalArgumentException("Trying to insert an experiment with an unknown " + typeResource.getLocalName() + " : " + uri);
            }
        }
    }

    /**
     * Check that all URI(s) which refers to a non {@link org.opensilex.sparql.annotations.SPARQLResource}-compliant model exists.
     *
     * @param model the experiment for which we check if all URI(s) exists
     * @throws SPARQLException          if the SPARQL uri checking query fail
     * @throws IllegalArgumentException if the given model contains a unknown URI
     */
    protected void checkURIs(ExperimentModel model) throws SPARQLException, IllegalArgumentException, URISyntaxException {

        // #TODO use a method to test in one query if multiple URI(s) exists and are of a given type, or use SHACL validation

        checkURIs(model.getInfrastructures(), (Oeso.Infrastructure));
        checkURIs(model.getSensors(), (Oeso.SensingDevice));
        checkURIs(model.getVariables(), (Oeso.Variable));
        checkURIs(model.getDevices(), (Oeso.Installation));

        if (model.getSpecies() != null && !sparql.uriExists(new URI(Oeso.Species.getURI()), model.getSpecies())) {
            throw new IllegalArgumentException("Trying to insert an experiment with an unknown species : " + model.getSpecies());
        }

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
    protected void filterExperimentSensors(ExperimentModel xp) {
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

//    public ListWithPagination<ExperimentModel> getAllXp(List<OrderBy> orderByList, int page, int pageSize) throws Exception {
//
//        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(ExperimentModel.class, null, null, orderByList, page, pageSize);
//        for (ExperimentModel xp : xps.getList()) {
//            filterExperimentSensors(xp);
//        }
//        return xps;
//    }


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
                                                      URI species,
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

    protected void appendCampaignFilter(SelectBuilder select, Integer campaign) throws Exception {
        if (campaign != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_SPARQL_VAR, campaign));
        }
    }

    protected void appendSpeciesFilter(SelectBuilder select, URI species) throws Exception {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.SPECIES_SPARQL_VAR, species));
        }
    }


    protected void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.LABEL_VAR, label));
        }
    }

    protected void appendUriRegexFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
    }

    protected void appendDateFilters(SelectBuilder select, Boolean ended, String startDate, String endDate) throws Exception {

        if (ended != null) {

            Node endDateVar = NodeFactory.createVariable(ExperimentModel.END_DATE_SPARQL_VAR);
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
        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(ExperimentModel.START_DATE_SPARQL_VAR,LocalDate.parse(startDate),ExperimentModel.END_DATE_SPARQL_VAR,LocalDate.parse(endDate));
        select.addFilter(dateRangeExpr);
    }

    protected void appendProjectListFilter(SelectBuilder select, List<URI> projects) throws Exception {

        if (projects != null && !projects.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasProject, ExperimentModel.PROJECT_URI_SPARQL_VAR);
            SPARQLQueryHelper.addWhereValues(select, ExperimentModel.PROJECT_URI_SPARQL_VAR, projects);
        }
    }

    protected static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    protected void appendGroupsListFilters(SelectBuilder select, boolean admin, Boolean isPublic, List<URI> groups) {

        if (admin) {
            // add no filter on groups for the admin
            return;
        }
        Var groupVar = makeVar(ExperimentModel.GROUP_SPARQL_VAR);
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

        protected void appendVariablesListFilter(SelectBuilder select, List<URI> variables) throws Exception {
        if (variables != null && !variables.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.measures, ExperimentModel.VARIABLES_SPARQL_VAR);
            SPARQLQueryHelper.addWhereValues(select, ExperimentModel.VARIABLES_SPARQL_VAR, variables);
        }
    }

}
