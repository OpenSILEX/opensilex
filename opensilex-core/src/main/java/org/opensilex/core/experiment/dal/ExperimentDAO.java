//******************************************************************************
//                          ExperimentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.exception.DuplicateNameException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.schemaQuery.SparqlSchema;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaRootNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaSimpleNode;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import static org.opensilex.sparql.service.SPARQLService.TYPE_VAR;
import static org.opensilex.sparql.service.SPARQLService.URI_VAR;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDAO.class);

    public ExperimentDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }

    public ExperimentModel create(ExperimentModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public ExperimentModel update(ExperimentModel instance, AccountModel user) throws Exception {
        validateExperimentAccess(instance.getUri(), user);
        sparql.update(instance);
        return instance;
    }

    public void updateWithVariables(URI xpUri, List<URI> variablesUris, AccountModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(xpUri), xpUri, Oeso.measures, variablesUris);
    }

    public void updateWithFactors(URI xpUri, List<URI> factorsUris, AccountModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), factorsUris, Oeso.studyEffectOf, xpUri);
    }

    public void delete(URI xpUri, AccountModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void delete(List<URI> xpUris, AccountModel user) throws Exception {
        for (URI xpUri : xpUris) {
            validateExperimentAccess(xpUri, user);
        }
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri, AccountModel user) throws Exception {
        validateExperimentAccess(xpUri, user);
        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, user.getLanguage());
        return xp;
    }

    @Deprecated
    public ListWithPagination<ExperimentModel> search(
            URI uri,
            String name,
            Integer campaign,
            Boolean isEnded,
            List<URI> variables, List<OrderBy> orderByList, int page, int pageSize) throws Exception {

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    appendUriRegexFilter(select, uri);
                    appendRegexLabelFilter(select, name);
                    appendIsActiveFilter(select, isEnded);
                    appendVariablesListFilter(select, variables);
                },
                orderByList,
                page,
                pageSize
        );
        return xps;
    }

    @Deprecated
    public ListWithPagination<ExperimentModel> search(URI uri,
            Integer campaign,
            String name,
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
                    appendRegexLabelFilter(select, name);
                    appendSpeciesFilter(select, species);
                    appendGroupsListFilters(select, admin, isPublic, groups);
                    appendProjectListFilter(select, projects);
                },
                orderByList,
                page,
                pageSize
        );

        return xps;

    }

    /**
     *
     * @param filter search filter to use
     * @param fetchProjects, if true then fetch projects associated to experiments, otherwise no extra request will be made.
     * @param fetchScientificSupervisors, if true then fetch scientific supervisors, otherwise no extra request will be made.
     * @param fetchTechnicalSupervisors, if true then fetch technical supervisors, otherwise no extra request will be made.
     * @return list of experiments, with embedded fields loaded according to the used sparql schema
     * @throws Exception
     */
    public ListWithPagination<ExperimentModel> search(
            ExperimentSearchFilter filter,
            boolean fetchProjects,
            boolean fetchScientificSupervisors,
            boolean fetchTechnicalSupervisors
            ) throws Exception {
        LocalDate startDate;
        LocalDate endDate;
        if (filter.getYear() != null) {
            startDate = LocalDate.of(filter.getYear(), 1, 1);
            endDate = LocalDate.of(filter.getYear(), 12, 31);
        } else {
            startDate = null;
            endDate = null;
        }

        SparqlSchema<ExperimentModel> schema = getSparqlSchema(fetchProjects, fetchScientificSupervisors, fetchTechnicalSupervisors);

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPaginationUsingSchema(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    appendRegexLabelFilter(select, filter.getName());
                    appendSpeciesFilter(select, filter.getSpecies());
                    appendFactorFilter(select, filter.getFactorCategories());
                    appendIsActiveFilter(select, filter.isEnded());
                    appendDateFilter(select, startDate, endDate);
                    appendProjectListFilter(select, filter.getProjects());
                    appendUserExperimentsFilter(select, filter.getUser());
                    appendPublicFilter(select, filter.isPublic());
                    appendFacilitiesFilter(select, filter.getFacilities());
                },
                schema,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );

        return xps;

    }

    private SparqlSchema<ExperimentModel> getSparqlSchema(boolean fetchProjects, boolean fetchScientificSupervisors, boolean fetchTechnicalSupervisors) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        List<SparqlSchemaSimpleNode<?>> childrenOfRoot = new ArrayList<>(List.of(
                new SparqlSchemaSimpleNode<>(FacilityModel.class, ExperimentModel.FACILITY_FIELD),
                new SparqlSchemaSimpleNode<>(SpeciesModel.class, ExperimentModel.SPECIES_FIELD)
        ));
        if(fetchProjects){
            childrenOfRoot.add(new SparqlSchemaSimpleNode<>(ProjectModel.class, ExperimentModel.PROJECT_URI_FIELD));
        }
        if(fetchScientificSupervisors){
            childrenOfRoot.add(new SparqlSchemaSimpleNode<>(PersonModel.class, ExperimentModel.SCIENTIFIC_SUPERVISOR_FIELD));
        }
        if(fetchTechnicalSupervisors){
            childrenOfRoot.add(new SparqlSchemaSimpleNode<>(PersonModel.class, ExperimentModel.TECHNICAL_SUPERVISOR_FIELD));
        }

        SparqlSchemaRootNode<ExperimentModel> rootNode = new SparqlSchemaRootNode<>(
                sparql,
                ExperimentModel.class,
                childrenOfRoot,
                false
        );

        SparqlSchema<ExperimentModel> schema = new SparqlSchema<>(rootNode);
        return schema;
    }

    private void appendSpeciesFilter(SelectBuilder select, List<URI> species) throws Exception {
        if (species != null && !species.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasSpecies, ExperimentModel.SPECIES_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(ExperimentModel.SPECIES_FIELD, species));
        }
    }

    private void appendFacilitiesFilter(SelectBuilder select, List<URI> facilities) throws Exception {
        if (facilities != null && !facilities.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.usesFacility, ExperimentModel.FACILITY_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(ExperimentModel.FACILITY_FIELD, facilities));
        }
    }

    private void appendFactorFilter(SelectBuilder select, List<URI> factorCategories) throws Exception {
        if (factorCategories != null && !factorCategories.isEmpty()) {
            Var factors = makeVar(ExperimentModel.FACTORS_FIELD);
            Var xpUri = makeVar(ExperimentModel.URI_FIELD);
            Var category = makeVar(ExperimentModel.FACTORS_CATEGORIES_FIELD);
            Var categories = makeVar("_categories");

            select.addWhere(factors, Oeso.studiedEffectIn,xpUri );
            select.addOptional(xpUri, Oeso.studyEffectOf, factors);
            select.addWhere(categories, Ontology.subClassAny, category);
            select.addWhere(factors, Oeso.hasCategory, categories);
            select.addFilter(SPARQLQueryHelper.inURIFilter(category, factorCategories));
        }
    }

    private void appendRegexLabelFilter(SelectBuilder select, String name) {
        if (!StringUtils.isEmpty(name)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.NAME_FIELD, name));
        }
    }

    private void appendUriRegexFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
    }

    private void appendDateFilter(SelectBuilder select, LocalDate startDate, LocalDate endDate) throws Exception {

        if (startDate != null && endDate != null) {

            Expr dateRangeExpr = SPARQLQueryHelper.intervalDateRange(ExperimentModel.START_DATE_FIELD, startDate, ExperimentModel.END_DATE_FIELD, endDate);
            select.addFilter(dateRangeExpr);
        } else {
            if (startDate != null || endDate != null) {
                Expr dateRangeExpr = SPARQLQueryHelper.dateRange(ExperimentModel.START_DATE_FIELD, startDate, ExperimentModel.END_DATE_FIELD, endDate);
                select.addFilter(dateRangeExpr);

            }

        }

    }

    private void appendIsActiveFilter(SelectBuilder select, Boolean ended) throws Exception {
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

    }

    private void appendProjectListFilter(SelectBuilder select, List<URI> projects) throws Exception {

        if (projects != null && !projects.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasProject, ExperimentModel.PROJECT_URI_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(ExperimentModel.PROJECT_URI_FIELD, projects));
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

    public Set<URI> getUserExperiments(AccountModel user) throws Exception {
        String lang = user.getLanguage();
        Set<URI> userExperiments = new HashSet<>();
        List<URI> xps = sparql.searchURIs(ExperimentModel.class, lang, (SelectBuilder select) -> {
            appendUserExperimentsFilter(select, user);
        });

        userExperiments.addAll(xps);

        return userExperiments;
    }
    
    /**
     * Get only running experiments available for a selected user
     * @param user current user
     * @return List of current experiment that are not ended
     * @throws Exception 
     */
    public Set<URI> getRunningUserExperiments(AccountModel user) throws Exception {
        String lang = user.getLanguage();
        Set<URI> userExperiments = new HashSet<>(); 
        
        List<URI> xps = sparql.searchURIs(ExperimentModel.class, lang, (SelectBuilder select) -> {
            appendUserExperimentsFilter(select, user); 
            Var uriVar = makeVar(ExperimentModel.URI_FIELD);
            Var endDateField = makeVar(ExperimentModel.END_DATE_FIELD);
            Triple endDateTriple = new Triple(uriVar, Oeso.endDate.asNode(), endDateField);
            select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(endDateTriple))); 
        });

        userExperiments.addAll(xps);

        return userExperiments;
    }
    
    public static void appendUserExperimentsFilter(SelectBuilder select, AccountModel user) throws Exception {
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

        Var publisherVar = makeVar(ExperimentModel.PUBLISHER_FIELD);
        select.addOptional(new Triple(uriVar, DCTerms.publisher.asNode(), publisherVar));
        Expr isPublisher = SPARQLQueryHelper.eq(publisherVar, userNodeURI);

        select.addFilter(SPARQLQueryHelper.or(
                inGroup,
                hasScientificSupervisor,
                hasTechnicalSupervisor,
                isPublic,
                isPublisher
        ));
    }

    public void validateExperimentAccess(Collection<URI> experiments, AccountModel user) throws Exception {

        var unknownXps = sparql.getExistingUris(ExperimentModel.class, experiments, false);
        if(! unknownXps.isEmpty()){
            throw new SPARQLInvalidUriListException("Experiments URI(s) not found", unknownXps);
        }

        if (Boolean.TRUE.equals(user.isAdmin())) {
            return;
        }

        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_user");
        Var groupVar = makeVar(ExperimentModel.GROUP_FIELD);

        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());
        Var publisherVar = makeVar(SPARQLResourceModel.PUBLISHER_FIELD);
        Var scientificSupervisorVar = makeVar(ExperimentModel.SCIENTIFIC_SUPERVISOR_FIELD);
        Var technicalSupervisorVar = makeVar(ExperimentModel.TECHNICAL_SUPERVISOR_FIELD);
        Var isPublicVar = makeVar(ExperimentModel.IS_PUBLIC_FIELD);

        // Sub-query : Retrieve all experiments which are accessible according user/group and experiment access rules (publisher, supervisor, public)
        WhereBuilder where = new WhereBuilder()
                .addWhere(URI_VAR, RDF.type, TYPE_VAR)
                .addWhere(TYPE_VAR, Ontology.subClassAny, sparql.getRDFType(ExperimentModel.class))
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, SecurityOntology.hasGroup.asNode(), groupVar)
                        .addWhere(groupVar, SecurityOntology.hasUserProfile.asNode(), userProfileVar)
                        .addWhere(userProfileVar, SecurityOntology.hasUser.asNode(), userVar))
                .addOptional(URI_VAR, DCTerms.publisher.asNode(), publisherVar)
                .addOptional(URI_VAR, Oeso.hasScientificSupervisor.asNode(), scientificSupervisorVar)
                .addOptional(URI_VAR, Oeso.hasTechnicalSupervisor.asNode(), technicalSupervisorVar)
                .addOptional(URI_VAR, Oeso.isPublic.asNode(), isPublicVar);

        // Filter : get experiments which are accessible by the user
        where.addFilter(
                SPARQLQueryHelper.or(
                        SPARQLQueryHelper.eq(publisherVar, userNodeURI),
                        SPARQLQueryHelper.eq(userVar, userNodeURI),
                        SPARQLQueryHelper.eq(scientificSupervisorVar, userNodeURI),
                        SPARQLQueryHelper.eq(technicalSupervisorVar, userNodeURI),
                        SPARQLQueryHelper.eq(isPublicVar, Boolean.TRUE)
                )
        );

        SelectBuilder select = new SelectBuilder().addVar(URI_VAR);
        select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(where));

        SPARQLQueryHelper.addWhereUriStringValues(
                select,
                URI_VAR.getVarName(),
                experiments.stream().map(URI::toString),
                true,
                experiments.size()
        );

        // Compute the subset of experiments which are not present in the set of accessible experiments
        List<URI> forbiddenXps = sparql.executeSelectQueryAsStream(select)
                .map(result -> URIDeserializer.formatURI(result.getStringValue(URI_VAR.getVarName())))
                .collect(Collectors.toList());

        if(! forbiddenXps.isEmpty()){
            throw new ForbiddenURIAccessException(forbiddenXps, "Forbidden Experiment access");
        }
    }

    public void validateExperimentAccess(URI experiment, AccountModel user) throws Exception {
        Objects.requireNonNull(experiment);
        validateExperimentAccess(Collections.singletonList(experiment),user);
    }

    /**
     *
     * @param experiment experiment
     * @return a non-null Stream of facilities URI (with a prefixed form) which are accessible inside an experiment.
     * A facility is accessible if :
     * <ul>
     *     <li>The facility is linked to an experiment with the {@link Oeso#usesFacility} relation</li>
     *     <li>The experiment has no explicit facility and the facility is linked to an organisation (with {@link Oeso#isHosted}) which is used by the experiment (with {@link Oeso#usesOrganization}}</li>
     * </ul>
     *
     * @throws IllegalArgumentException if experiment is null
     * @throws SPARQLException if an error occurs during SPARQL query evaluation
     * @apiNote
     *
     * <pre>
     * This method first search for experiment facilities, and check for organization facilities if and only if the experiment has no direct relation (trough {@link Oeso#isHosted})
     * with a facility.
     *
     * <b>SPARQL query : </b>
     *
     * {@code
     * PREFIX vocabulary: <http://www.opensilex.org/vocabulary/oeso#>
     * SELECT DISTINCT ?facility ?experiment_facilities WHERE {
     *          GRAPH <../set/experiment> {
     *              :experiment vocabulary:usesFacility ?experiment_facilities
     *          }
     *          UNION {
     *              GRAPH <../set/experiment> {
     *                  :experiment vocabulary:usesOrganization ?experiment_facilities
     *              }
     *              GRAPH <../set/organization>{
     *   	            ?organization vocabulary:isHosted ?facility
     *              }
     *              FILTER BOUND(?experiment_facilities)
     *          }
     * }
     *
     * }
     * </pre>
     */
    public Stream<String> getAvailableFacilitiesURIs(URI experiment) throws SPARQLException {

        Node experimentNode = SPARQLDeserializers.nodeURI(experiment);
        Node experimentGraph = sparql.getDefaultGraph(ExperimentModel.class);

        Var xpFacility = makeVar("experiment_"+ OrganizationModel.FACILITIES_FIELD);
        Var facility = makeVar(OrganizationModel.FACILITIES_FIELD);
        Var organization = makeVar(ExperimentModel.ORGANIZATION_FIELD);

        SelectBuilder query = new SelectBuilder()
                .setDistinct(true)
                .addVar(facility)
                .addVar(xpFacility)
                .addGraph(experimentGraph,experimentNode,Oeso.usesFacility,xpFacility)
                .addUnion(new WhereBuilder()
                        .addGraph(experimentGraph, experimentNode, Oeso.usesOrganization, organization)
                        .addGraph(sparql.getDefaultGraph(OrganizationModel.class), organization, Oeso.isHosted, facility)
                        .addFilter(SPARQLQueryHelper.getExprFactory().bound(xpFacility)) // don't retrieve facilities from experiment organizations, if some facilities were found via experiment
                );

        // evaluate query and iterate of facilities URI stream
        return sparql.executeSelectQueryAsStream(query)
                .map(result -> StringUtils.isEmpty(result.getStringValue(facility.getVarName())) ?
                                result.getStringValue(xpFacility.getVarName()) :
                                result.getStringValue(facility.getVarName())
                )
                .map(URIDeserializer::formatURIAsStr);  // map each facility URI in a prefixed form
    }

    public List<FacilityModel> getAvailableFacilities(URI xpUri, AccountModel user) throws Exception {
        validateExperimentAccess(xpUri, user);

        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, user.getLanguage());

        Map<URI, FacilityModel> availableFacilities = xp.getFacilities()
                .stream().collect(Collectors.toMap(FacilityModel::getUri, Function.identity()));

        List<URI> organizationUriFilter = xp.getOrganizations()
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql.getServiceV2());

        if (!organizationUriFilter.isEmpty()) {
            facilityLogic.search(new FacilitySearchFilter()
                    .setUser(user)
                    .setOrganizations(organizationUriFilter)
            ).getList().forEach(facility -> availableFacilities.put(facility.getUri(), facility));
        }

        return new ArrayList<>(availableFacilities.values());
    }

    public List<ExperimentModel> getByURIs(List<URI> uris, AccountModel currentUser) throws Exception {
        return sparql.getListByURIs(ExperimentModel.class, uris, currentUser.getLanguage());
    }

    public int count() throws Exception {
        return sparql.count(ExperimentModel.class);
    }

    public ExperimentModel getByName(String name) throws Exception {
        //pageSize=2 in order to detect duplicated names
        ListWithPagination<ExperimentModel> results = sparql.searchWithPagination(
            ExperimentModel.class,
            null,
            (SelectBuilder select) -> {
                appendRegexLabelFilter(select, name);
            },
            null,
            0,
            2
        );
        
        if (results.getList().isEmpty()) {
            return null;
        }
        
        if (results.getList().size() > 1) {
            throw new DuplicateNameException(name);
        }

        return results.getList().get(0);
    }

    public ExperimentModel getExperimentByNameOrURI(String expNameOrUri, AccountModel user) throws Exception {
        ExperimentModel exp = null;
        if (URIDeserializer.validateURI(expNameOrUri)) {
            URI expUri = URI.create(expNameOrUri);
            exp = get(expUri, user);

        } else {
            exp = getByName(expNameOrUri);
        }
        return exp;
    }

    /**
     * Update the experiment species from the germplasms of their scientific objects. Three requests are performed :
     *
     * First query (delete) :
     *
     * <pre>
     * delete where {
     *     graph <../set/experiment> {
     *         <experimentUri> vocabulary:hasSpecies ?oldSpecies .
     *     }
     * }
     * </pre>
     *
     * Second query (insert germplasms that are species) :
     *
     * <pre>
     * insert {
     *     graph <../set/experiment> {
     *         <experimentUri> vocabulary:hasSpecies ?germplasm .
     *     }
     * } where {
     *     graph <experimentUri> {
     *         ?scientificObject a ?rdfType ;
     *             vocabulary:hasGermplasm ?germplasm;
     *     }
     *     ?rdfType rdfs:subClassOf* vocabulary:ScientificObject .
     *     ?germplasm a/rdfs:subClassOf* vocabulary:Species .
     * }
     * </pre>
     *
     * Third query (insert species the germplasms derive from) :
     *
     * <pre>
     * insert {
     *     graph <../set/experiment> {
     *         <experimentUri> vocabulary:hasSpecies ?newSpecies .
     *     }
     * } where {
     *     graph <experimentUri> {
     *         ?scientificObject a ?rdfType ;
     *             vocabulary:hasGermplasm ?germplasm;
     *     }
     *     ?rdfType rdfs:subClassOf* vocabulary:ScientificObject .
     *     ?germplasm vocabulary:fromSpecies ?newSpecies .
     * }
     * </pre>
     *
     * @param experimentUri
     * @throws Exception
     */
    public void updateExperimentSpeciesFromScientificObjects(URI experimentUri) throws Exception {
        // Vars
        Var oldSpeciesVar = makeVar("oldSpecies");
        Var newSpeciesVar = makeVar("newSpecies");
        Var scientificObjectVar = makeVar("scientificObject");
        Var germplasmVar = makeVar("germplasm");
        Var rdfTypeVar = makeVar("rdfType");

        // Uris
        Node experimentGraph = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(ExperimentModel.class));
        Node experimentUriNode = SPARQLDeserializers.nodeURI(experimentUri);

        // Update statement building
        UpdateBuilder updateDelete = new UpdateBuilder();
        UpdateBuilder updateInsert1 = new UpdateBuilder();
        UpdateBuilder updateInsert2 = new UpdateBuilder();
        updateDelete.addDelete(experimentGraph, experimentUriNode, Oeso.hasSpecies.asNode(), oldSpeciesVar);
        updateInsert1.addInsert(experimentGraph, experimentUriNode, Oeso.hasSpecies.asNode(), germplasmVar);
        updateInsert2.addInsert(experimentGraph, experimentUriNode, Oeso.hasSpecies.asNode(), newSpeciesVar);

        // Delete - where
        WhereBuilder deleteWhere = new WhereBuilder();
        deleteWhere.addGraph(experimentGraph, experimentUriNode, Oeso.hasSpecies.asNode(), oldSpeciesVar);
        updateDelete.addWhere(deleteWhere);

        // Insert - where statement building
        WhereBuilder insertWhere1 = new WhereBuilder();
        WhereBuilder insertWhere2 = new WhereBuilder();

        // Selection of the scientific object and its germplasm
        WhereBuilder whereInExperiment = new WhereBuilder();
        whereInExperiment.addWhere(scientificObjectVar, RDF.type.asNode(), rdfTypeVar);
        whereInExperiment.addWhere(scientificObjectVar, Oeso.hasGermplasm.asNode(), germplasmVar);
        insertWhere1.addGraph(experimentUriNode, whereInExperiment);
        insertWhere1.addWhere(rdfTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode());
        insertWhere2.addGraph(experimentUriNode, whereInExperiment);
        insertWhere2.addWhere(rdfTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode());

        // The two cases for the species
        insertWhere1.addWhere(germplasmVar, Ontology.typeSubClassAny, Oeso.Species.asNode());
        insertWhere2.addWhere(germplasmVar, Oeso.fromSpecies.asNode(), newSpeciesVar);

        // Add the where clauses to the queries
        updateInsert1.addWhere(insertWhere1);
        updateInsert2.addWhere(insertWhere2);

        sparql.startTransaction();
        try {
            sparql.executeUpdateQuery(updateDelete);
            sparql.executeUpdateQuery(updateInsert1);
            sparql.executeUpdateQuery(updateInsert2);
            sparql.commitTransaction();
        } catch (SPARQLException e) {
            LOGGER.error("Error while updating species of experiment " + experimentUri, e);
            sparql.rollbackTransaction();
        }
    }
}
