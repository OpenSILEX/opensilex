//******************************************************************************
//                          GermplasmDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.bson.Document;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.metadata.MetaDataDao;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static org.opensilex.core.experiment.dal.ExperimentDAO.appendUserExperimentsFilter;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Germplasm DAO
 *
 * @author Alice Boizet
 */
public class GermplasmDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final Node defaultGraph;
    protected final MongoCollection<MetaDataModel> attributeCollection;

    public static final String ATTRIBUTES_COLLECTION_NAME = "germplasmAttribute";

    public GermplasmDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
        try {
            defaultGraph = sparql.getDefaultGraph(GermplasmModel.class);
        } catch (SPARQLException e) {
            throw new RuntimeException("Unexpected error when retrieving GermplasmModel default graph", e);
        }
        attributeCollection = nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, MetaDataModel.class);
        attributeCollection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), new IndexOptions().unique(true));
    }

    public MongoCollection<MetaDataModel> getAttributesCollection() {
        return attributeCollection;
    }

    public GermplasmModel update(GermplasmModel model) throws Exception {
        MetaDataModel storedAttributes = getStoredAttributes(model.getUri());
        MetaDataModel attributeModel = model.getMetadata();

        if (((attributeModel == null || MapUtils.isEmpty(attributeModel.getAttributes())) && storedAttributes == null)) {
            sparql.update(model);
        } else {
            nosql.startTransaction();
            sparql.startTransaction();
            sparql.update(model);
            try {
                if (attributeModel != null && !MapUtils.isEmpty(attributeModel.getAttributes())) {
                    attributeModel.setUri(model.getUri());
                    if (storedAttributes != null) {
                        nosql.update(attributeModel, MetaDataModel.class, ATTRIBUTES_COLLECTION_NAME);
                    } else {
                        nosql.create(attributeModel, MetaDataModel.class, ATTRIBUTES_COLLECTION_NAME, null);
                    }
                } else {
                    // delete old attributes
                    attributeCollection.findOneAndDelete(nosql.getSession(), eq(MongoModel.URI_FIELD, model.getUri()));
                }
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }

        }
        return model;

    }

    public GermplasmModel create(GermplasmModel model) throws Exception {
        if (model.getMetadata() != null) {
            nosql.startTransaction();
            sparql.startTransaction();
            try {
                sparql.create(model);
                model.getMetadata().setUri(model.getUri());
                nosql.create(model.getMetadata(), MetaDataModel.class, ATTRIBUTES_COLLECTION_NAME, null);
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }
        } else {
            sparql.create(model);
        }

        return model;

    }

    public GermplasmModel get(URI uri, AccountModel user) throws Exception {
        GermplasmModel germplasm = sparql.getByURI(GermplasmModel.class, uri, user.getLanguage());
        if (germplasm != null) {
            MetaDataModel storedAttributes = getStoredAttributes(germplasm.getUri());
            if (storedAttributes != null) {
                germplasm.setMetadata(storedAttributes);
            }
        }
        return germplasm;
    }

    /**
     * @param searchFilter  search filter
     * @param fetchMetadata indicate if {@link GermplasmModel#getMetadata()} must be retrieved from mongodb
     * @return a {@link ListWithPagination} of {@link GermplasmModel}
     */
    public ListWithPagination<GermplasmModel> search(
            GermplasmSearchFilter searchFilter,
            boolean fetchMetadata) throws Exception {

        final Set<URI> filteredUris;
        if (searchFilter.getMetadata() != null) {
            MetaDataDao metaDataDao = new MetaDataDao(nosql);
            filteredUris = metaDataDao.searchUris(attributeCollection,Document.parse(searchFilter.getMetadata()));

            // no URI match the given metadata filter, return empty list
            if (filteredUris.isEmpty()) {
                return new ListWithPagination<>(Collections.emptyList());
            }
        } else {
            filteredUris = new HashSet<>();
        }

        if (!CollectionUtils.isEmpty(searchFilter.getUris())) {
            if (filteredUris.isEmpty()) {
                // only use selected uris
                filteredUris.addAll(searchFilter.getUris());
            } else {
                // metadata URI filter + selected uris, use Set intersection
                filteredUris.retainAll(searchFilter.getUris());
            }
        }

        // Filter by experiment if it has any species. Otherwise, don't apply any filter on experiments (because it
        // doesn't make sens).
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

                    appendExperimentFilter(select, finalExperiment);

                    initialSelect.set(select);
                },
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result, searchFilter.getLang()),
                searchFilter.getOrderByList(),
                searchFilter.getPage(),
                searchFilter.getPageSize()
        );

        // manually fetch synonyms with ListFetcher in optimized way

        Map<String, Boolean> fieldsToFetch = new HashMap<>();
        // The triple <?uri skos:altLabel ?synonym> is present into initial select if and only if a filter on name occurred
        fieldsToFetch.put(GermplasmModel.SYNONYM_VAR, StringUtils.isEmpty(searchFilter.getName()));

        SPARQLListFetcher<GermplasmModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                GermplasmModel.class,
                sparql.getDefaultGraph(GermplasmModel.class),
                fieldsToFetch,
                initialSelect.get(),
                models.getList()
        );
        listFetcher.updateModels();

        if (fetchMetadata) {
            // get all Germplasm metadata with one query
            new MetaDataDao(nosql).getMetaDataAssociatedTo(
                    attributeCollection, // filter in Germplasm attribute collection
                    MongoModel.URI_FIELD, // use MetaData URI field
                    models.getList(), // get Metadata associated with Germplasm uris
                    GermplasmModel::setMetadata // update Germplasm metadata
            );
        }
        return models;
    }

    public int countInGroup(URI group, String lang) throws Exception {
        return sparql.count(
                defaultGraph,
                GermplasmModel.class,
                lang,
                (SelectBuilder select) -> {
                    appendGroupFilter(select, group);
                },
                null
        );
    }

    private void appendRegexUriFilter(SelectBuilder select, String uri) {
        if (uri != null) {
            try {
                uri = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri)).toString();
            } catch (Exception e) {
            } finally {
                select.addFilter(SPARQLQueryHelper.regexFilterOnURI(GermplasmModel.URI_FIELD, uri, "i"));
            }
        }
    }

    private void appendGroupFilter(SelectBuilder select, URI group){
        if (group != null) {
            select.addWhere(SPARQLDeserializers.nodeURI(group), RDFS.member, makeVar(SPARQLResourceModel.URI_FIELD));
        }
    }

    private void appendUriFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString()))));
        }
    }

    private void appendRdfTypeFilter(SelectBuilder select, URI rdfType) throws Exception {
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

    private void appendSpeciesFilter(SelectBuilder select, URI species) throws Exception {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.SPECIES_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString())))
            ));
        }
    }

    private void appendVarietyFilter(SelectBuilder select, URI variety) throws Exception {
        if (variety != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.VARIETY_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString())))
            ));
        }
    }

    private void appendAccessionFilter(SelectBuilder select, URI accession) throws Exception {
        if (accession != null) {
            select.addFilter(SPARQLQueryHelper.or(
                    SPARQLQueryHelper.eq(GermplasmModel.ACCESSION_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString()))),
                    SPARQLQueryHelper.eq(GermplasmModel.URI_FIELD, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString())))
            ));
        }
    }

    private void appendRegexInstituteFilter(SelectBuilder select, String institute) throws Exception {
        if (!StringUtils.isEmpty(institute)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.INSTITUTE_SPARQL_VAR, institute));
        }
    }

    private void appendProductionYearFilter(SelectBuilder select, Integer productionYear) throws Exception {
        if (productionYear != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.PRODUCTION_YEAR_SPARQL_VAR, productionYear));
        }
    }

    public boolean isGermplasmType(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.Germplasm)
        );
    }

    public void delete(URI uri) throws Exception {
        MetaDataModel attributes = getStoredAttributes(uri);
        if (attributes != null) {
            nosql.startTransaction();
            sparql.startTransaction();
            try {
                sparql.delete(GermplasmModel.class, uri);
                MongoCollection<MetaDataModel> collection = getAttributesCollection();
                collection.findOneAndDelete(nosql.getSession(), eq("uri", uri));
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }
        } else {
            sparql.delete(GermplasmModel.class, uri);
        }
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
     * Get all germplasm attributes
     *
     * @return
     */
    public Set<String> getDistinctGermplasmAttributes() {
        return new MetaDataDao(nosql).getDistinctKeys(attributeCollection,null);
    }

    public Set<String> getDistinctGermplasmAttributesValues(String attribute, String attributeValue, int page, int pageSize) {
        return new MetaDataDao(nosql).getDistinctValues(attributeCollection, attribute,attributeValue,page,pageSize);
    }

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

    private void appendExperimentFilter(SelectBuilder select, URI xpUri) throws SPARQLException {
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

    private MetaDataModel getStoredAttributes(URI uri) {
        MetaDataModel storedAttributes = null;
        try {
            storedAttributes = nosql.findByURI(MetaDataModel.class, ATTRIBUTES_COLLECTION_NAME, uri);
        } catch (NoSQLInvalidURIException e) {
        }
        return storedAttributes;
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

}
