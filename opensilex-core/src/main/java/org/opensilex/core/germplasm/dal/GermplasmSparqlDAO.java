//******************************************************************************
//                          GermplasmDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
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
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.account.dal.AccountModel;
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

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.opensilex.core.experiment.dal.ExperimentDAO.appendUserExperimentsFilter;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Data Access Object for Sparql part of Germplasms.
 *
 * @author yvan.roux@inrae.fr
 */
public class GermplasmSparqlDAO {

    protected final SPARQLService sparql;
    protected final Node defaultGraph;

    public GermplasmSparqlDAO(SPARQLService sparql) {
        this.sparql = sparql;
        try {
            defaultGraph = sparql.getDefaultGraph(GermplasmModel.class);
        } catch (SPARQLException e) {
            throw new RuntimeException("Unexpected error when retrieving GermplasmModel default graph", e);
        }
    }

    public void update(GermplasmModel model) throws Exception {
        sparql.update(model);
    }

    public void create(GermplasmModel model) throws Exception {
        sparql.create(model);
    }

    public GermplasmModel get(URI uri, AccountModel user, boolean withNested) throws Exception {
        GermplasmModel germplasm = sparql.getByURI(GermplasmModel.class, uri, user.getLanguage());
        if (germplasm != null) {
            //Load basic data about nested objects (parent germplasms, etc...)
            if (withNested) {
                List<GermplasmModel> singletonList = Collections.singletonList(germplasm);
                fetchGermplasmsOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGermplasmM, (model) -> model.setParentMGermplasms(new LinkedList<>()));
                fetchGermplasmsOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGermplasmF, (model) -> model.setParentFGermplasms(new LinkedList<>()));
                fetchGermplasmsOfRelation(singletonList, user.getLanguage(), Oeso.hasParentGermplasm, (model) -> model.setParentGermplasms(new LinkedList<>()));
            }
        }
        return germplasm;
    }

    /**
     * @param searchFilter search filter
     * @param fetchNestedObjects if true, fetch nested objects (parent germplasms)
     * @return a {@link ListWithPagination} of {@link GermplasmModel}
     */
    public ListWithPagination<GermplasmModel> search(
            GermplasmSearchFilter searchFilter,
            boolean fetchNestedObjects) throws Exception {

        final Set<URI> filteredUris = new HashSet<>();
        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            filteredUris.addAll(searchFilter.getUris());
        }



        final URI finalExperiment;
        if (searchFilter.getExperiment() != null) {
            finalExperiment = searchFilter.getExperiment();
        } else {
            finalExperiment = null;
        }

        SparqlNoProxyFetcher<GermplasmModel> customFetcher = new SparqlNoProxyFetcher<>(GermplasmModel.class, sparql);
        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();

        ListWithPagination<GermplasmModel> models = sparql.searchWithPagination(
                sparql.getDefaultGraph(GermplasmModel.class),
                GermplasmModel.class,
                searchFilter.getLang(),
                (SelectBuilder select) -> {

                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();

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
                    appendParentFilter(select, searchFilter.getParentGermplasms());
                    appendParentMFilter(select, searchFilter.getParentMGermplasms());
                    appendParentFFilter(select, searchFilter.getParentFGermplasms());
                    appendExperimentFilter(select, finalExperiment);

                    initialSelect.set(select);
                },
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result, searchFilter.getLang()),
                searchFilter.getOrderByList(),
                searchFilter.getPage(),
                searchFilter.getPageSize()
        );

        //Load nested objets (parent germplasms, etc...)
        if (fetchNestedObjects) {
            fetchGermplasmsOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGermplasmM, (model) -> model.setParentMGermplasms(new LinkedList<>()));
            fetchGermplasmsOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGermplasmF, (model) -> model.setParentFGermplasms(new LinkedList<>()));
            fetchGermplasmsOfRelation(models.getList(), searchFilter.getLang(), Oeso.hasParentGermplasm, (model) -> model.setParentGermplasms(new LinkedList<>()));
        }

        // manually fetch synonyms with ListFetcher in optimized way
        SPARQLListFetcher<GermplasmModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                GermplasmModel.class,
                sparql.getDefaultGraph(GermplasmModel.class),
                Collections.singleton(GermplasmModel.SYNONYM_VAR),
                models.getList()
        );
        listFetcher.updateModels();

        return models;
    }

    public int countInGroup(URI group, String lang) throws Exception {
        return sparql.count(
                defaultGraph,
                GermplasmModel.class,
                lang,
                (SelectBuilder select) -> appendGroupFilter(select, group),
                null
        );
    }


    public boolean isGermplasmType(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.Germplasm)
        );
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(GermplasmModel.class, uri);
    }

    public boolean isLinkedToSth(GermplasmModel germplasm) throws SPARQLException {
        Var subject = makeVar("s");
        return sparql.executeAskQuery(
                new AskBuilder()
                        .addWhere(subject, Oeso.fromSpecies, SPARQLDeserializers.nodeURI(germplasm.getUri()))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, Oeso.fromVariety, SPARQLDeserializers.nodeURI(germplasm.getUri())))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, Oeso.fromAccession, SPARQLDeserializers.nodeURI(germplasm.getUri())))
                        .addUnion(new WhereBuilder()
                                .addWhere(subject, Oeso.hasGermplasm, SPARQLDeserializers.nodeURI(germplasm.getUri()))
                        )
        );

    }

    /**
     * Get all experiments associated with a germplasm
     */
    //@Todo: put this method in the experiment package ? It's an experiment fetcher with a germplasm filter so it's not really a germplasm method. - Yvan 06/06/2024
    public ListWithPagination<ExperimentModel> getExpFromGermplasm(
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
                    appendGermplasmFilter(select, uri);
                    if (!StringUtils.isEmpty(name)) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.NAME_FIELD, name));
                        appendUserExperimentsFilter(select, currentUser);
                    }
                },
                orderByList,
                page,
                pageSize);

    }

    public ListWithPagination<GermplasmModel> brapiSearch(AccountModel user, URI germplasmDbId, String germplasmName, String germplasmSpecies, int page, int pageSize) throws Exception {

        final Set<URI> speciesURIs;
        if (germplasmSpecies != null) {
            List<URI> species = sparql.searchURIs(
                    GermplasmModel.class,
                    user.getLanguage(),
                    (SelectBuilder select) -> {
                        appendRdfTypeFilter(select, new URI(Oeso.Species.toString()));
                        appendRegexLabelFilter(select, germplasmSpecies);
                    });

            speciesURIs = new HashSet<>(species);
        } else {
            speciesURIs = new HashSet<>();
        }

        return sparql.searchWithPagination(
                GermplasmModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {
                    appendRdfTypeFilter(select, new URI(Oeso.Accession.toString()));
                    appendUriFilter(select, germplasmDbId);
                    appendRegexLabelFilter(select, germplasmName);
                    appendSpeciesListFilter(select, speciesURIs);
                },
                null,
                page,
                pageSize);
    }

    //#region private SPARQL Query Helper Methods
    private static void addToANestedPropertyList(GermplasmModel containerGermplasm, GermplasmModel nestedModel, Property predicate) {
        if (predicate.getURI().equals(Oeso.hasParentGermplasm.getURI())) {
            containerGermplasm.getParentGermplasms().add(nestedModel);
        } else if (predicate.getURI().equals(Oeso.hasParentGermplasmM.getURI())) {
            containerGermplasm.getParentMGermplasms().add(nestedModel);
        } else {
            containerGermplasm.getParentFGermplasms().add(nestedModel);
        }
    }


    private void fetchGermplasmsOfRelation(List<GermplasmModel> models, String lang, Property predicate, Consumer<GermplasmModel> setEmptyList) throws SPARQLException, ParseException {

        if (models.isEmpty()) {
            return;
        }
        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            GermplasmModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            setEmptyList.accept(model);
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        GermplasmGroupDAO.createManyToManySPARQLRequest(
                defaultGraph,
                defaultGraph,
                uris,
                (GermplasmGroupDAO.ManyToManyNestedUpdateData manyToManyNestedUpdateData) -> {
                    Integer groupIdx = modelsIndexes.get(manyToManyNestedUpdateData.subjectUri);
                    GermplasmModel group = models.get(groupIdx);
                    GermplasmModel nestedModelAsGermplasm = GermplasmModel.fromSPARQLNamedResourceModel(manyToManyNestedUpdateData.nestedModel);
                    addToANestedPropertyList(group, nestedModelAsGermplasm, predicate);
                },
                predicate,
                Oeso.Germplasm,
                models.size(),
                sparql,
                lang
        );
    }

    private void appendRegexUriFilter(SelectBuilder select, String uri) {
        if (!StringUtils.isEmpty(uri)) {
            try {
                uri = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri)).toString();
            } catch (Exception e) {
            } finally {
                select.addFilter(SPARQLQueryHelper.regexFilterOnURI(GermplasmModel.URI_FIELD, uri, "i"));
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
            select.addWhere(makeVar(GermplasmModel.URI_FIELD), predicateVar, makeVar(GermplasmModel.PARENT_VAR));
            select.addWhere(predicateVar, Ontology.subPropertyAny, Oeso.hasParentGermplasm);
            select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.PARENT_VAR, parentUris));
        }
    }

    private void appendParentMFilter(SelectBuilder select, List<URI> parentMUris) {
        if (!CollectionUtils.isEmpty(parentMUris)) {
            select.addWhere(makeVar(GermplasmModel.URI_FIELD), Oeso.hasParentGermplasmM, makeVar(GermplasmModel.PARENT_M_VAR));
            select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.PARENT_M_VAR, parentMUris));
        }
    }

    private void appendParentFFilter(SelectBuilder select, List<URI> parentFUris) {
        if (!CollectionUtils.isEmpty(parentFUris)) {
            select.addWhere(makeVar(GermplasmModel.URI_FIELD), Oeso.hasParentGermplasmF, makeVar(GermplasmModel.PARENT_F_VAR));
            select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.PARENT_F_VAR, parentFUris));
        }
    }

    private void appendUriFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString()))));
        }
    }

    private void appendRdfTypeFilter(SelectBuilder select, URI rdfType) {
        if (rdfType != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.TYPE_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
        }
    }

    private void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_FIELD, label));
        }
    }

    private void appendRegexLabelAndSynonymFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addOptional(new Triple(makeVar(GermplasmModel.URI_FIELD), SKOS.altLabel.asNode(), makeVar(GermplasmModel.SYNONYM_VAR)));
            //select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(GermplasmModel.URI_FIELD), SKOS.altLabel.asNode(), makeVar(GermplasmModel.SYNONYM_VAR)));
            select.addFilter(SPARQLQueryHelper.or(SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_FIELD, label), SPARQLQueryHelper.regexFilter(GermplasmModel.SYNONYM_VAR, label)));
        }
    }

    private void appendSpeciesFilter(SelectBuilder select, URI species) {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.SPECIES_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString())))
            ));
        }
    }

    private void appendVarietyFilter(SelectBuilder select, URI variety) {
        if (variety != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.VARIETY_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString())))
            ));
        }
    }

    private void appendAccessionFilter(SelectBuilder select, URI accession) {
        if (accession != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.ACCESSION_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString())))
            ));
        }
    }

    private void appendRegexInstituteFilter(SelectBuilder select, String institute) {
        if (!StringUtils.isEmpty(institute)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.INSTITUTE_SPARQL_VAR, institute));
        }
    }

    private void appendProductionYearFilter(SelectBuilder select, Integer productionYear) throws Exception {
        if (productionYear != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.PRODUCTION_YEAR_SPARQL_VAR, productionYear));
        }
    }

    private void appendGermplasmFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            WhereBuilder builder = new WhereBuilder();
            builder.addGraph(makeVar(SPARQLResourceModel.URI_FIELD), new Triple(makeVar("so"), NodeFactory.createURI(Oeso.hasGermplasm.toString()), NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString())));

            WhereBuilder builder2 = new WhereBuilder();
            builder2.addWhere(makeVar("gpl"), makeVar("p"), NodeFactory.createURI(SPARQLDeserializers.nodeURI(uri).toString()));
            builder2.addWhere(makeVar("gpl"), RDF.type, makeVar("gplType"));
            builder2.addWhere(makeVar("gplType"), Ontology.subClassAny, Oeso.Germplasm);
            builder2.addGraph(makeVar(SPARQLResourceModel.URI_FIELD), new Triple(makeVar("so"), NodeFactory.createURI(Oeso.hasGermplasm.toString()), makeVar("gpl")));

            builder.addUnion(builder2);
            select.addWhere(builder);
        }
    }

    private void appendExperimentFilter(SelectBuilder select, URI xpUri) {
        if (xpUri != null) {
            Var scientificObjectVar = makeVar("scientificObject");
            Var rdfTypeVar = makeVar("scientificObjectType");
            Var germplasmVar = makeVar("uri");

            WhereBuilder whereInExperiment = new WhereBuilder();
            whereInExperiment.addWhere(scientificObjectVar, RDF.type.asNode(), rdfTypeVar);
            whereInExperiment.addWhere(scientificObjectVar, Oeso.hasGermplasm.asNode(), germplasmVar);
            select.addGraph(SPARQLDeserializers.nodeURI(xpUri), whereInExperiment);
            select.addWhere(rdfTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode());
        }
    }

    private void appendURIsFilter(SelectBuilder select, Set<URI> uris) {
        if (uris != null && !uris.isEmpty()) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.URI_FIELD, uris));
        }
    }

    private void appendSpeciesListFilter(SelectBuilder select, Set<URI> species) {
        if (species != null && !species.isEmpty()) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.SPECIES_URI_SPARQL_VAR, species));
        }
    }
    //#endregion
}
