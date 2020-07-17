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
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.apache.commons.collections4.CollectionUtils.select;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.infrastructure.dal.InfrastructureFacilityModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.user.dal.UserModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

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

    public ExperimentModel update(ExperimentModel instance, UserModel user) throws Exception {
        validateExperimentAccess(instance.getUri(), user);
        sparql.update(instance);
        return instance;
    }

    public void updateWithVariables(URI xpUri, List<URI> variablesUris, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(xpUri), xpUri, Oeso.measures, variablesUris);
    }

    public void updateWithSensors(URI xpUri, List<URI> sensorsUris, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), sensorsUris, Oeso.participatesIn, xpUri);
    }

    public void updateWithFactors(URI xpUri, List<URI> factorsUris, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), factorsUris, Oeso.studyEffectOf, xpUri);
    }

    public void delete(URI xpUri, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void delete(List<URI> xpUris, UserModel user) throws Exception {
        for (URI xpUri : xpUris) {
            validateExperimentAccess(xpUri, user);
        }
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, null);
        if (xp != null) {
            filterExperimentSensors(xp);
        }
        return xp;
    }

    /**
     * Remove all URI from {@link ExperimentModel#getSensors()} method which
     * don't represents a {@link Oeso#SensingDevice} in the SPARQL Graph
     *
     * @param xp the {@link ExperimentModel} to filter
     */
    private void filterExperimentSensors(ExperimentModel xp) {
        if (xp.getSensors().isEmpty()) {
            return;
        }

        // #TODO don't fetch URI which don't represents sensors and delete this method
        xp.getSensors().removeIf(sensor -> {
            try {
                return !sparql.uriExists(new URI(Oeso.SensingDevice.getURI()), sensor);
            } catch (SPARQLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Deprecated
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
                    appendUriRegexFilter(select, uri);
                    appendRegexLabelFilter(select, label);
                    appendDateFilters(select, isEnded, null, null);
                    appendCampaignFilter(select, campaign);
                    appendVariablesListFilter(select, variables);
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

    @Deprecated
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

                    LocalDate startDateObj = null;
                    if (startDate != null) {
                        startDateObj = LocalDate.parse(startDate);
                    }
                    LocalDate endDateObj = null;
                    if (endDate != null) {
                        endDateObj = LocalDate.parse(endDate);
                    }
                    appendDateFilters(select, isEnded, startDateObj, endDateObj);
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

    public ListWithPagination<ExperimentModel> search(
            String label,
            List<URI> species,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isEnded,
            List<URI> projects,
            Boolean isPublic,
            UserModel user,
            List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    appendRegexLabelFilter(select, label);
                    appendSpeciesFilter(select, species);
                    appendDateFilters(select, isEnded, startDate, endDate);
                    appendProjectListFilter(select, projects);
                    appendUserExperimentsFilter(select, user);
                    appendPublicFilter(select, isPublic);
                },
                orderByList,
                page,
                pageSize
        );

        return xps;

    }

    private void appendCampaignFilter(SelectBuilder select, Integer campaign) throws Exception {
        if (campaign != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_FIELD, campaign));
        }
    }

    private void appendSpeciesFilter(SelectBuilder select, List<URI> species) throws Exception {
        if (species != null && !species.isEmpty()) {
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

    private void appendDateFilters(SelectBuilder select, Boolean ended, LocalDate startDate, LocalDate endDate) throws Exception {
        if (ended != null) {
            Node endDateVar = NodeFactory.createVariable(ExperimentModel.END_DATE_FIELD);
            Node currentDateNode = SPARQLDeserializers.getForClass(LocalDate.class).getNode(LocalDate.now());

            // an experiment is ended if the end date is less than the the current date
            if (ended) {
                select.addFilter(SPARQLQueryHelper.getExprFactory().le(endDateVar, currentDateNode));
            } else {
                ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
                Expr noEndDateFilter = exprFactory.not(exprFactory.bound(endDateVar));
                select.addFilter(exprFactory.or(noEndDateFilter, exprFactory.gt(endDateVar, currentDateNode)));
            }
        }

        Expr dateRangeExpr = SPARQLQueryHelper.intervalDateRange(ExperimentModel.START_DATE_FIELD, startDate, ExperimentModel.END_DATE_FIELD, endDate);
        if (dateRangeExpr != null) {
            select.addFilter(dateRangeExpr);
        }
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
        Triple groupTriple = new Triple(makeVar(ExperimentModel.URI_FIELD), SecurityOntology.hasGroup.asNode(), groupVar);

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

    private void appendPublicFilter(SelectBuilder select, Boolean isPublic) throws Exception {
        if (isPublic != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.IS_PUBLIC_FIELD, isPublic));
        }
    }

    public Set<URI> getUserExperiments(UserModel user) throws Exception {
        String lang = user.getLanguage();
        Set<URI> userExperiments = new HashSet<>();
        List<URI> xps = sparql.searchURIs(ExperimentModel.class, lang, (SelectBuilder select) -> {
            Var userProfileVar = makeVar("_userProfile");
            select.addWhere(makeVar(ExperimentModel.URI_FIELD), SecurityOntology.hasGroup, makeVar(ExperimentModel.GROUP_FIELD));
            select.addWhere(makeVar(ExperimentModel.GROUP_FIELD), SecurityOntology.hasUserProfile, userProfileVar);
            select.addWhere(userProfileVar, SecurityOntology.hasUser, SPARQLDeserializers.nodeURI(user.getUri()));
        });

        userExperiments.addAll(xps);

        return userExperiments;
    }

    private void appendUserExperimentsFilter(SelectBuilder select, UserModel user) throws Exception {
        if (user == null || user.isAdmin()) {
            return;
        }

        Var uriVar = makeVar(ExperimentModel.URI_FIELD);
        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_user");
        Var groupVar = makeVar(ExperimentModel.GROUP_FIELD);

        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());

        ElementGroup optionals = new ElementGroup();
        optionals.addTriplePattern(new Triple(uriVar, SecurityOntology.hasGroup.asNode(), groupVar));
        optionals.addTriplePattern(new Triple(groupVar, SecurityOntology.hasUserProfile.asNode(), userProfileVar));
        optionals.addTriplePattern(new Triple(userProfileVar, SecurityOntology.hasUser.asNode(), userVar));
        select.getWhereHandler().getClause().addElement(new ElementOptional(optionals));
        Expr inGroup = SPARQLQueryHelper.eq(userVar, userNodeURI);

        Var scientificSupervisorVar = makeVar(ExperimentModel.SCIENTIFIC_SUPERVISOR_FIELD);
        select.addOptional(new Triple(uriVar, Oeso.hasScientificSupervisor.asNode(), scientificSupervisorVar));
        Expr hasScientificSupervisor = SPARQLQueryHelper.eq(scientificSupervisorVar, userNodeURI);

        Var technicalSupervisorVar = makeVar(ExperimentModel.TECHNICAL_SUPERVISOR_FIELD);
        select.addOptional(new Triple(uriVar, Oeso.hasTechnicalSupervisor.asNode(), technicalSupervisorVar));
        Expr hasTechnicalSupervisor = SPARQLQueryHelper.eq(technicalSupervisorVar, userNodeURI);

        Var isPublicVar = makeVar(ExperimentModel.IS_PUBLIC_FIELD);
        select.addOptional(new Triple(uriVar, Oeso.isPublic.asNode(), isPublicVar));
        Expr isPublic = SPARQLQueryHelper.eq(isPublicVar, Boolean.TRUE);

        select.addFilter(SPARQLQueryHelper.or(
                inGroup,
                hasScientificSupervisor,
                hasTechnicalSupervisor,
                isPublic
        ));
    }

    public void validateExperimentAccess(URI experimentURI, UserModel user) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, experimentURI)) {
            throw new NotFoundURIException(experimentURI);
        }

        if (user.isAdmin()) {
            return;
        }

        AskBuilder ask = sparql.getUriExistsQuery(ExperimentModel.class, experimentURI);

        Var uriVar = makeVar(ExperimentModel.URI_FIELD);
        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_user");
        Var groupVar = makeVar(ExperimentModel.GROUP_FIELD);

        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());

        ElementGroup optionals = new ElementGroup();
        optionals.addTriplePattern(new Triple(uriVar, SecurityOntology.hasGroup.asNode(), groupVar));
        optionals.addTriplePattern(new Triple(groupVar, SecurityOntology.hasUserProfile.asNode(), userProfileVar));
        optionals.addTriplePattern(new Triple(userProfileVar, SecurityOntology.hasUser.asNode(), userVar));
        ask.getWhereHandler().getClause().addElement(new ElementOptional(optionals));
        Expr inGroup = SPARQLQueryHelper.eq(userVar, userNodeURI);

        Var scientificSupervisorVar = makeVar(ExperimentModel.SCIENTIFIC_SUPERVISOR_FIELD);
        ask.addOptional(new Triple(uriVar, Oeso.hasScientificSupervisor.asNode(), scientificSupervisorVar));
        Expr hasScientificSupervisor = SPARQLQueryHelper.eq(scientificSupervisorVar, userNodeURI);

        Var technicalSupervisorVar = makeVar(ExperimentModel.TECHNICAL_SUPERVISOR_FIELD);
        ask.addOptional(new Triple(uriVar, Oeso.hasTechnicalSupervisor.asNode(), technicalSupervisorVar));
        Expr hasTechnicalSupervisor = SPARQLQueryHelper.eq(technicalSupervisorVar, userNodeURI);

        Var isPublicVar = makeVar(ExperimentModel.IS_PUBLIC_FIELD);
        ask.addOptional(new Triple(uriVar, Oeso.isPublic.asNode(), isPublicVar));
        Expr isPublic = SPARQLQueryHelper.eq(isPublicVar, Boolean.TRUE);

        ask.addFilter(
                SPARQLQueryHelper.or(
                        inGroup,
                        hasScientificSupervisor,
                        hasTechnicalSupervisor,
                        isPublic
                )
        );

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(experimentURI);
        }
    }

    public void setFacilities(URI xpUri, List<URI> facilities, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);

        Node xpGraph = SPARQLDeserializers.nodeURI(xpUri);
        sparql.startTransaction();
        try {
            List<URI> existingFacilities = sparql.searchPrimitives(xpGraph, xpUri, Oeso.hasFacility, URI.class);
            sparql.deletePrimitives(xpGraph, xpUri, Oeso.hasFacility);
            sparql.insertPrimitives(xpGraph, xpUri, Oeso.hasFacility, facilities, URI.class);
            sparql.deleteAll(xpGraph, existingFacilities);
            sparql.copyAll(sparql.getDefaultGraph(InfrastructureModel.class), facilities, xpGraph);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            throw ex;
        }
    }

    public List<InfrastructureFacilityModel> getFacilities(URI xpUri, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);

        Node xpGraph = SPARQLDeserializers.nodeURI(xpUri);

        List<URI> facilitiesURIs = sparql.searchPrimitives(xpGraph, xpUri, Oeso.hasFacility, URI.class);

        if (facilitiesURIs.size() > 0) {
            return sparql.search(InfrastructureFacilityModel.class, user.getLanguage(), (select) -> {
                SPARQLQueryHelper.inURI(select, InfrastructureFacilityModel.URI_FIELD, facilitiesURIs);
            });
        } else {
            return new ArrayList<>();
        }
    }

    public List<InfrastructureFacilityModel> getAvailableFacilities(URI xpUri, UserModel user) throws Exception {
        validateExperimentAccess(xpUri, user);

        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, user.getLanguage());

        List<InfrastructureModel> infrastructures = xp.getInfrastructures();

        List<URI> infraURIs = new ArrayList<>();
        infrastructures.forEach(infra -> {
            infraURIs.add(infra.getUri());
        });

        return sparql.search(InfrastructureFacilityModel.class, user.getLanguage(), (select) -> {
            SPARQLQueryHelper.inURI(select, InfrastructureFacilityModel.INFRASTRUCTURE_FIELD, infraURIs);
        });
    }

}
