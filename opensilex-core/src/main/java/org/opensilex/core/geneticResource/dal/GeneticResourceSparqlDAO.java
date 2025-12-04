//******************************************************************************
//                          GeneticResourceDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geneticResource.api.GeneticResourceSearchFilter;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.SecurityOntology;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.sparql.expr.Expr;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.opensilex.core.experiment.dal.ExperimentDAO.appendUserExperimentsFilter;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Data Access Object for Sparql part of GeneticResources.
 *
 * @author yvan.roux@inrae.fr
 */
public class GeneticResourceSparqlDAO {

    protected final SPARQLService sparql;
    protected final Node defaultGraph;

    public GeneticResourceSparqlDAO(SPARQLService sparql) {
        this.sparql = sparql;
        try {
            defaultGraph = sparql.getDefaultGraph(GeneticResourceModel.class);
        } catch (SPARQLException e) {
            throw new RuntimeException("Unexpected error when retrieving GeneticResourceModel default graph", e);
        }
    }

    public void update(GeneticResourceModel model) throws Exception {
        sparql.update(model);
    }

    public void update(GeneticResourceModel model, AccountModel user) throws Exception {
        validateGeneticResourceAccess(model.getUri(), user);
        sparql.update(model);
    }

    public void create(GeneticResourceModel model) throws Exception {
        sparql.create(model);
    }

    public GeneticResourceModel get(URI uri, AccountModel user, boolean withNested) throws Exception {
        validateGeneticResourceAccess(uri,user);
        GeneticResourceModel geneticResource = sparql.getByURI(GeneticResourceModel.class, uri, user.getLanguage());
        if (geneticResource != null) {
            //Load basic data about nested objects (parent geneticResources, etc...)
            if (withNested) {
                List<GeneticResourceModel> singletonList = Collections.singletonList(geneticResource);
                fetchGeneticResourcesOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGeneticResourceM, (model) -> model.setParentMGeneticResources(new LinkedList<>()));
                fetchGeneticResourcesOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGeneticResourceF, (model) -> model.setParentFGeneticResources(new LinkedList<>()));
                fetchGeneticResourcesOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGeneticResource, (model) -> model.setParentGeneticResources(new LinkedList<>()));
            }
        }
        return geneticResource;
    }

    /**
     * Paginated search of {@link GeneticResourceModel} according to the criteria of a {@link GeneticResourceSearchFilter}.
     * <p>
     * Applies SPARQL filters (name, species, variety, accession, institute, visibility, groups, etc.)
     * and handles pagination and sorting. If {@code fetchNestedObjects} is enabled, parent
     * relationships (maternal, paternal, general) are also loaded. Synonyms are enriched
     * via a {@link SPARQLListFetcher}.
     * </p>
     *
     * @param searchFilter        search criteria (filters, pagination, sorting, access rights)
     * @param fetchNestedObjects  {@code true} to also load parent relationships
     * @return a paginated list of {@link GeneticResourceModel} matching the criteria
     * @throws Exception if an error occurs during the execution of the SPARQL query
     */

    public ListWithPagination<GeneticResourceModel> search(
            GeneticResourceSearchFilter searchFilter,
            boolean fetchNestedObjects) throws Exception {

        // retrieve information directly from the searchFilter
        Boolean isPublic = searchFilter.isPublic();
        List<URI> groups = searchFilter.getGroups();
        boolean admin = searchFilter.getUser() != null && searchFilter.getUser().isAdmin();

        final Set<URI> filteredUris = new HashSet<>();
        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            filteredUris.addAll(searchFilter.getUris());
        }

        final URI finalExperiment = searchFilter.getExperiment();

        SparqlNoProxyFetcher<GeneticResourceModel> customFetcher = new SparqlNoProxyFetcher<>(GeneticResourceModel.class, sparql);
        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();

        ListWithPagination<GeneticResourceModel> models = sparql.searchWithPagination(
                sparql.getDefaultGraph(GeneticResourceModel.class),
                GeneticResourceModel.class,
                searchFilter.getLang(),
                (SelectBuilder select) -> {

                    appendRegexUriFilter(select, searchFilter.getUri());
                    appendRdfTypeFilter(select, searchFilter.getType());
                    appendRegexLabelAndSynonymFilter(select, searchFilter.getName());
                    appendSpeciesFilter(select, searchFilter.getSpecies());
                    appendVarietyFilter(select, searchFilter.getVariety());
                    appendAccessionFilter(select, searchFilter.getAccession());
                    appendRegexInstituteFilter(select, searchFilter.getInstitute());
                    appendProductionYearFilter(select, searchFilter.getProductionYear());
                    appendURIsFilter(select, filteredUris);
                    appendGroupFilter(select, searchFilter.getGroup());
                    appendParentFilter(select, searchFilter.getParentGeneticResources());
                    appendParentMFilter(select, searchFilter.getParentMGeneticResources());
                    appendParentFFilter(select, searchFilter.getParentFGeneticResources());
                    appendPublicFilter(select, isPublic);
                    appendgroupsListFilters(select, admin, groups);
                    appendExperimentFilter(select, finalExperiment);
                    appendUserGeneticResourceFilter(select, searchFilter.getUser());

                    initialSelect.set(select);
                },
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result, searchFilter.getLang()),
                searchFilter.getOrderByList(),
                searchFilter.getPage(),
                searchFilter.getPageSize()
        );

        if (fetchNestedObjects) {
            fetchGeneticResourcesOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGeneticResourceM, (model) -> model.setParentMGeneticResources(new LinkedList<>()));
            fetchGeneticResourcesOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGeneticResourceF, (model) -> model.setParentFGeneticResources(new LinkedList<>()));
            fetchGeneticResourcesOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGeneticResource, (model) -> model.setParentGeneticResources(new LinkedList<>()));
        }

        SPARQLListFetcher<GeneticResourceModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                GeneticResourceModel.class,
                sparql.getDefaultGraph(GeneticResourceModel.class),
                Collections.singleton(GeneticResourceModel.SYNONYM_VAR),
                models.getList()
        );
        listFetcher.updateModels();

        return models;
    }


    public int countInGroup(URI group, String lang) throws Exception {
        return sparql.count(
                defaultGraph,
                GeneticResourceModel.class,
                lang,
                (SelectBuilder select) -> appendGroupFilter(select, group),
                null
        );
    }


    public boolean isGeneticResourceType(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.GeneticResource)
        );
    }

    public void delete(URI uri, AccountModel user) throws Exception {
        validateGeneticResourceAccess(uri, user);
        sparql.delete(GeneticResourceModel.class, uri);
    }

    public boolean isLinkedToSth(GeneticResourceModel geneticResource) throws SPARQLException {
        Var subject = makeVar("s");
        Var object = makeVar("o");
        Var parentGeneticResourceSubclass = makeVar("parentGeneticResourceSubclass");
        return sparql.executeAskQuery(
                new AskBuilder()
                        .addWhere(subject, Oeso.fromSpecies, SPARQLDeserializers.nodeURI(geneticResource.getUri()))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, Oeso.fromVariety, SPARQLDeserializers.nodeURI(geneticResource.getUri())))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, Oeso.fromAccession, SPARQLDeserializers.nodeURI(geneticResource.getUri())))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, parentGeneticResourceSubclass, SPARQLDeserializers.nodeURI(geneticResource.getUri()))
                                .addWhere(parentGeneticResourceSubclass, Ontology.subPropertyAny, Oeso.hasParentGeneticResource)
                        )
                        .addUnion(new WhereBuilder()
                                .addWhere(SPARQLDeserializers.nodeURI(geneticResource.getUri()), parentGeneticResourceSubclass, object)
                                .addWhere(parentGeneticResourceSubclass, Ontology.subPropertyAny, Oeso.hasParentGeneticResource)

                        )
        );
    }

    /**
     * Get all experiments associated with a geneticResource
     */
    //@Todo: put this method in the experiment package ? It's an experiment fetcher with a geneticResource filter so it's not really a geneticResource method. - Yvan 06/06/2024
    public ListWithPagination<ExperimentModel> getExpFromGeneticResource(
            AccountModel currentUser,
            URI uri,
            String name,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                ExperimentModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    appendGeneticResourceFilter(select, uri);
                    if (!StringUtils.isEmpty(name)) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.NAME_FIELD, name));
                        appendUserExperimentsFilter(select, currentUser);
                    }
                },
                orderByList,
                page,
                pageSize);

    }

    public ListWithPagination<GeneticResourceModel> brapiSearch(AccountModel user, URI geneticResourceDbId, String geneticResourceName, String geneticResourceSpecies, int page, int pageSize) throws Exception {

        final Set<URI> speciesURIs;
        if (geneticResourceSpecies != null) {
            List<URI> species = sparql.searchURIs(
                    GeneticResourceModel.class,
                    user.getLanguage(),
                    (SelectBuilder select) -> {
                        appendRdfTypeFilter(select, new URI(Oeso.Species.toString()));
                        appendRegexLabelFilter(select, geneticResourceSpecies);
                    });

            speciesURIs = new HashSet<>(species);
        } else {
            speciesURIs = new HashSet<>();
        }

        return sparql.searchWithPagination(
                GeneticResourceModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {
                    appendRdfTypeFilter(select, new URI(Oeso.Accession.toString()));
                    appendUriFilter(select, geneticResourceDbId);
                    appendRegexLabelFilter(select, geneticResourceName);
                    appendSpeciesListFilter(select, speciesURIs);
                },
                null,
                page,
                pageSize);
    }

    //#region private SPARQL Query Helper Methods
    private static void addToANestedPropertyList(GeneticResourceModel containerGeneticResource, GeneticResourceModel nestedModel, Property predicate) {
        if (predicate.getURI().equals(Oeso.hasParentGeneticResource.getURI())) {
            containerGeneticResource.getParentGeneticResources().add(nestedModel);
        } else if (predicate.getURI().equals(Oeso.hasParentGeneticResourceM.getURI())) {
            containerGeneticResource.getParentMGeneticResources().add(nestedModel);
        } else {
            containerGeneticResource.getParentFGeneticResources().add(nestedModel);
        }
    }


    private void fetchGeneticResourcesOfRelation(List<GeneticResourceModel> models, String lang, Property predicate, Consumer<GeneticResourceModel> setEmptyList) throws SPARQLException, ParseException {

        if (models.isEmpty()) {
            return;
        }
        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            GeneticResourceModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            setEmptyList.accept(model);
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        GeneticResourceGroupDAO.createManyToManySPARQLRequest(
                defaultGraph,
                defaultGraph,
                uris,
                (GeneticResourceGroupDAO.ManyToManyNestedUpdateData manyToManyNestedUpdateData) -> {
                    Integer groupIdx = modelsIndexes.get(manyToManyNestedUpdateData.subjectUri);
                    GeneticResourceModel group = models.get(groupIdx);
                    GeneticResourceModel nestedModelAsGeneticResource = GeneticResourceModel.fromSPARQLNamedResourceModel(manyToManyNestedUpdateData.nestedModel);
                    addToANestedPropertyList(group, nestedModelAsGeneticResource, predicate);
                },
                predicate,
                Oeso.GeneticResource,
                models.size(),
                sparql,
                lang
        );
    }

    /**
     * Checks whether a user has access to a given geneticResource.
     * <p>
     * - If the URI does not exist, a {@link NotFoundURIException} is thrown. <br>
     * - If the user is an administrator, access is always granted. <br>
     * - Otherwise, access is granted only if the geneticResource is:
     *   <ul>
     *     <li>public,</li>
     *     <li>linked to a group the user belongs to,</li>
     *     <li>or published by the user.</li>
     *   </ul>
     * </p>
     *
     * @param geneticResourceURI URI of the geneticResource to validate
     * @param user         user requesting access
     * @throws NotFoundURIException if the geneticResource does not exist
     * @throws Exception            if an error occurs during the construction or execution of the SPARQL query
     */

    public void validateGeneticResourceAccess(URI geneticResourceURI, AccountModel user) throws Exception {

        if (!sparql.uriExists(GeneticResourceModel.class, geneticResourceURI)) {
            throw new NotFoundURIException("GeneticResource URI not found: ", geneticResourceURI);
        }

        if (user.isAdmin()) {
            return;
        }

        AskBuilder ask = sparql.getUriExistsQuery(GeneticResourceModel.class, geneticResourceURI);

        Node uriVar = SPARQLDeserializers.nodeURI(geneticResourceURI);
        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_user");
        Var groupVar = makeVar(GeneticResourceModel.GROUP_USER_FIELD);

        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());

        ElementGroup optionals = new ElementGroup();
        optionals.addTriplePattern(new Triple(uriVar, SecurityOntology.hasGroup.asNode(), groupVar));
        optionals.addTriplePattern(new Triple(groupVar, SecurityOntology.hasUserProfile.asNode(), userProfileVar));
        optionals.addTriplePattern(new Triple(userProfileVar, SecurityOntology.hasUser.asNode(), userVar));
        ask.getWhereHandler().getClause().addElement(new ElementOptional(optionals));
        Expr inGroup = SPARQLQueryHelper.eq(userVar, userNodeURI);

        Var publisherVar = makeVar(GeneticResourceModel.PUBLISHER_FIELD);
        ask.addOptional(new Triple(uriVar, DCTerms.publisher.asNode(), publisherVar));
        Expr isPublisher = SPARQLQueryHelper.eq(publisherVar, userNodeURI);


        Var isPublicVar = makeVar(GeneticResourceModel.IS_PUBLIC_FIELD);
        ask.addOptional(new Triple(uriVar, Oeso.isPublic.asNode(), isPublicVar));
        Expr isPublic = SPARQLQueryHelper.eq(isPublicVar, Boolean.TRUE);

        ask.addFilter(
                SPARQLQueryHelper.or(
                        isPublisher,
                        inGroup,
                        isPublic
                )
        );

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(geneticResourceURI);
        }
    }

    private void appendRegexUriFilter(SelectBuilder select, String uri) {
        if (!StringUtils.isEmpty(uri)) {
            try {
                uri = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri)).toString();
            } catch (Exception e) {
            } finally {
                select.addFilter(SPARQLQueryHelper.regexFilterOnURI(GeneticResourceModel.URI_FIELD, uri, "i"));
            }
        }
    }

    private void appendGroupFilter(SelectBuilder select, URI group) {
        if (group != null) {
            select.addWhere(SPARQLDeserializers.nodeURI(group), RDFS.member, makeVar(SPARQLResourceModel.URI_FIELD));
        }
    }

    private void appendParentFilter(SelectBuilder select, List<URI> parentUris) {
        if (!CollectionUtils.isEmpty(parentUris)) {
            Var predicateVar = makeVar("hasParent");
            select.addWhere(makeVar(GeneticResourceModel.URI_FIELD), predicateVar, makeVar(GeneticResourceModel.PARENT_VAR));
            select.addWhere(predicateVar, Ontology.subPropertyAny, Oeso.hasParentGeneticResource);
            select.addFilter(SPARQLQueryHelper.inURIFilter(GeneticResourceModel.PARENT_VAR, parentUris));
        }
    }

    /**
     * Adds SPARQL filters related to a user's access rights to the {@link SelectBuilder}.
     * <p>
     * If the user is {@code null} or an administrator, no filters are added.
     * Otherwise, the geneticResource is accessible if:
     * <ul>
     *   <li>the user belongs to a group linked to the geneticResource,</li>
     *   <li>the geneticResource is marked as public,</li>
     *   <li>or the user is the publisher of the geneticResource.</li>
     * </ul>
     * </p>
     *
     * @param select SPARQL query being constructed
     * @param user   current user (or {@code null})
     * @throws Exception if an error occurs while constructing the filter
     */

    public static void appendUserGeneticResourceFilter(SelectBuilder select, AccountModel user) throws Exception {
        if (user == null || user.isAdmin()) {
            return;
        }

        Var uriVar = makeVar(GeneticResourceModel.URI_FIELD);
        Var userProfileVar = makeVar("_userProfile");
        Var userVar = makeVar("_user");
        Var groupVar = makeVar(GeneticResourceModel.GROUP_USER_FIELD);

        Node userNodeURI = SPARQLDeserializers.nodeURI(user.getUri());

        ElementGroup optionals = new ElementGroup();
        optionals.addTriplePattern(new Triple(uriVar, SecurityOntology.hasGroup.asNode(), groupVar));
        optionals.addTriplePattern(new Triple(groupVar, SecurityOntology.hasUserProfile.asNode(), userProfileVar));
        optionals.addTriplePattern(new Triple(userProfileVar, SecurityOntology.hasUser.asNode(), userVar));
        select.getWhereHandler().getClause().addElement(new ElementOptional(optionals));
        Expr inGroup = SPARQLQueryHelper.eq(userVar, userNodeURI);


        Var isPublicVar = makeVar(GeneticResourceModel.IS_PUBLIC_FIELD);
        select.addOptional(new Triple(uriVar, Oeso.isPublic.asNode(), isPublicVar));
        Expr isPublic = SPARQLQueryHelper.eq(isPublicVar, Boolean.TRUE);

        Var publisherVar = makeVar(GeneticResourceModel.PUBLISHER_FIELD);
        select.addOptional(new Triple(uriVar, DCTerms.publisher.asNode(), publisherVar));
        Expr isPublisher = SPARQLQueryHelper.eq(publisherVar, userNodeURI);

        select.addFilter(SPARQLQueryHelper.or(
                inGroup,
                isPublic,
                isPublisher
        ));
    }
    private void appendParentMFilter(SelectBuilder select, List<URI> parentMUris) {
        if (!CollectionUtils.isEmpty(parentMUris)) {
            select.addWhere(makeVar(GeneticResourceModel.URI_FIELD), Oeso.hasParentGeneticResourceM, makeVar(GeneticResourceModel.PARENT_M_VAR));
            select.addFilter(SPARQLQueryHelper.inURIFilter(GeneticResourceModel.PARENT_M_VAR, parentMUris));
        }
    }

    private void appendParentFFilter(SelectBuilder select, List<URI> parentFUris) {
        if (!CollectionUtils.isEmpty(parentFUris)) {
            select.addWhere(makeVar(GeneticResourceModel.URI_FIELD), Oeso.hasParentGeneticResourceF, makeVar(GeneticResourceModel.PARENT_F_VAR));
            select.addFilter(SPARQLQueryHelper.inURIFilter(GeneticResourceModel.PARENT_F_VAR, parentFUris));
        }
    }

    private void appendUriFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addFilter(SPARQLQueryHelper.eq(GeneticResourceModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString()))));
        }
    }

    private void appendRdfTypeFilter(SelectBuilder select, URI rdfType) {
        if (rdfType != null) {
            select.addFilter(SPARQLQueryHelper.eq(GeneticResourceModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
        }
    }

    private void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GeneticResourceModel.LABEL_FIELD, label));
        }
    }

    private void appendRegexLabelAndSynonymFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addOptional(new Triple(makeVar(GeneticResourceModel.URI_FIELD), SKOS.altLabel.asNode(), makeVar(GeneticResourceModel.SYNONYM_VAR)));
            //select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(GeneticResourceModel.URI_FIELD), SKOS.altLabel.asNode(), makeVar(GeneticResourceModel.SYNONYM_VAR)));
            select.addFilter(SPARQLQueryHelper.or(SPARQLQueryHelper.regexFilter(GeneticResourceModel.LABEL_FIELD, label), SPARQLQueryHelper.regexFilter(GeneticResourceModel.SYNONYM_VAR, label)));
        }
    }

    private void appendSpeciesFilter(SelectBuilder select, URI species) {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GeneticResourceModel.SPECIES_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString()))),
                    SPARQLQueryHelper.eq(GeneticResourceModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString())))
            ));
        }
    }

    private void appendVarietyFilter(SelectBuilder select, URI variety) {
        if (variety != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GeneticResourceModel.VARIETY_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString()))),
                    SPARQLQueryHelper.eq(GeneticResourceModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString())))
            ));
        }
    }

    private void appendAccessionFilter(SelectBuilder select, URI accession) {
        if (accession != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GeneticResourceModel.ACCESSION_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString()))),
                    SPARQLQueryHelper.eq(GeneticResourceModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString())))
            ));
        }
    }

    private void appendRegexInstituteFilter(SelectBuilder select, String institute) {
        if (!StringUtils.isEmpty(institute)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GeneticResourceModel.INSTITUTE_SPARQL_VAR, institute));
        }
    }

    private void appendProductionYearFilter(SelectBuilder select, Integer productionYear) throws Exception {
        if (productionYear != null) {
            select.addFilter(SPARQLQueryHelper.eq(GeneticResourceModel.PRODUCTION_YEAR_SPARQL_VAR, productionYear));
        }
    }

    private void appendGeneticResourceFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            WhereBuilder builder = new WhereBuilder();
            builder.addGraph(makeVar(SPARQLResourceModel.URI_FIELD), new Triple(makeVar("so"), NodeFactory.createURI(Oeso.hasGeneticResource.toString()), NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString())));

            WhereBuilder builder2 = new WhereBuilder();
            builder2.addWhere(makeVar("gpl"), makeVar("p"), NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString()));
            builder2.addWhere(makeVar("gpl"), RDF.type, makeVar("gplType"));
            builder2.addWhere(makeVar("gplType"), Ontology.subClassAny, Oeso.GeneticResource);
            builder2.addGraph(makeVar(SPARQLResourceModel.URI_FIELD), new Triple(makeVar("so"), NodeFactory.createURI(Oeso.hasGeneticResource.toString()), makeVar("gpl")));

            builder.addUnion(builder2);
            select.addWhere(builder);
        }
    }

    private void appendPublicFilter(SelectBuilder select, Boolean isPublic) throws Exception {
        if (isPublic != null) {
            select.addFilter(SPARQLQueryHelper.eq(GeneticResourceModel.IS_PUBLIC_FIELD, isPublic));
        }
    }

    /**
     * Adds SPARQL filters related to the user's groups to the {@link SelectBuilder}.
     * <p>
     * - If {@code admin} is true: no filter is applied (full access). <br>
     * - If the user has no groups: only geneticResources without groups (public) are visible. <br>
     * - Otherwise: visible geneticResources are those that are public or linked to one of the user's groups.
     * </p>
     *
     * @param select      SPARQL query being constructed
     * @param admin       indicates whether the user is an administrator
     * @param groups list of groups the user belongs to (can be empty)
     */

    private void appendgroupsListFilters(SelectBuilder select, boolean admin, List<URI> groups) {

        if (admin) {
            return;
        }

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        Var groupVar = makeVar(ExperimentModel.GROUP_FIELD);
        Triple groupTriple = new Triple(makeVar(GeneticResourceModel.URI_FIELD), SecurityOntology.hasGroup.asNode(), groupVar);

        // resources without a group (i.e., public)
        Expr publicExpr = exprFactory.notexists(new WhereBuilder().addWhere(groupTriple));

        if (CollectionUtils.isEmpty(groups)) {
            // user has no groups → can only see public resources
            select.addFilter(publicExpr);
        } else {
            // resources associated with the user's groups
            Expr groupInExpr = exprFactory.in(groupVar, groups.stream()
                    .map(uri -> NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString())))
                    .toArray());

            Expr groupExpr = exprFactory.exists(new WhereBuilder()
                    .addWhere(groupTriple)
                    .addFilter(groupInExpr));

            // combined filter: public OR in the user's groups
            select.addFilter(exprFactory.or(publicExpr, groupExpr));
        }
    }

    private void appendExperimentFilter(SelectBuilder select, URI xpUri) {
        if (xpUri != null) {
            Var scientificObjectVar = makeVar("scientificObject");
            Var rdfTypeVar = makeVar("scientificObjectType");
            Var geneticResourceVar = makeVar("uri");

            WhereBuilder whereInExperiment = new WhereBuilder();
            whereInExperiment.addWhere(scientificObjectVar, RDF.type.asNode(), rdfTypeVar);
            whereInExperiment.addWhere(scientificObjectVar, Oeso.hasGeneticResource.asNode(), geneticResourceVar);
            select.addGraph(SPARQLDeserializers.nodeURI(xpUri), whereInExperiment);
            select.addWhere(rdfTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode());
        }
    }

    private void appendURIsFilter(SelectBuilder select, Set<URI> uris) {
        if (uris != null && !uris.isEmpty()) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(GeneticResourceModel.URI_FIELD, uris));
        }
    }

    private void appendSpeciesListFilter(SelectBuilder select, Set<URI> species) {
        if (species != null && !species.isEmpty()) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(GeneticResourceModel.SPECIES_URI_SPARQL_VAR, species));
        }
    }
    //#endregion
}
