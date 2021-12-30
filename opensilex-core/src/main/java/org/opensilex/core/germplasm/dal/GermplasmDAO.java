//******************************************************************************
//                          GermplasmDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import static com.mongodb.client.model.Projections.computed;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
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
import static org.opensilex.core.experiment.dal.ExperimentDAO.appendUserExperimentsFilter;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * Germplasm DAO
 *
 * @author Alice Boizet
 */
public class GermplasmDAO {

    private static final Cache<Key, Set> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final Node defaultGraph;

    public static final String ATTRIBUTES_COLLECTION_NAME = "germplasmAttribute";

    public GermplasmDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
        try {
            defaultGraph = sparql.getDefaultGraph(GermplasmModel.class);
        } catch (SPARQLException e) {
           throw new RuntimeException("Unexpected error when retrieving GermplasmModel default graph",e);
        }
    }

    public MongoCollection getAttributesCollection() {
        return nosql.getDatabase().getCollection(ATTRIBUTES_COLLECTION_NAME, GermplasmAttributeModel.class);
    }

    public GermplasmModel update(GermplasmModel germplasm) throws Exception {
        GermplasmAttributeModel storedAttributes = getStoredAttributes(germplasm.getUri());

        if ((germplasm.getAttributes() == null || germplasm.getAttributes().isEmpty()) && storedAttributes == null) {
            sparql.update(germplasm);
        } else {
            nosql.startTransaction();
            sparql.startTransaction();
            sparql.update(germplasm);
            MongoCollection collection = getAttributesCollection();
            collection.createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));            

            try {
                if (germplasm.getAttributes() != null && !germplasm.getAttributes().isEmpty()) {
                    GermplasmAttributeModel model = new GermplasmAttributeModel();
                    model.setUri(germplasm.getUri());
                    model.setAttribute(germplasm.getAttributes());
                    if (storedAttributes != null) {
                        nosql.update(model, GermplasmAttributeModel.class, ATTRIBUTES_COLLECTION_NAME);
                    } else {
                        nosql.create(model, GermplasmAttributeModel.class, ATTRIBUTES_COLLECTION_NAME, null);
                    }
                } else {
                    collection.findOneAndDelete(nosql.getSession(), eq("uri", germplasm.getUri()));
                }
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }

        }
        return germplasm;

    }

    public boolean labelExistsCaseSensitive(String label, URI rdfType) throws Exception {
        AskBuilder askQuery = new AskBuilder()
                .from(sparql.getDefaultGraph(GermplasmModel.class).toString())
                .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(rdfType))
                .addWhere("?uri", RDFS.label, label);
        //.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_VAR, "^" + label + "$", "i"));

        return sparql.executeAskQuery(askQuery);
    }

    public boolean labelExistsCaseSensitiveBySpecies(GermplasmCreationDTO germplasm) throws Exception {
        AskBuilder askQuery = new AskBuilder()
                .from(sparql.getDefaultGraph(GermplasmModel.class).toString())
                .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(germplasm.getRdfType()))
                .addWhere("?uri", RDFS.label, germplasm.getName());

        if (germplasm.getSpecies() != null) {
            askQuery.addWhere("?uri", Oeso.fromSpecies, SPARQLDeserializers.nodeURI(germplasm.getSpecies()));
        } else if (germplasm.getVariety() != null) {
            askQuery.addWhere("?uri", Oeso.fromVariety, SPARQLDeserializers.nodeURI(germplasm.getVariety()));
        } else if (germplasm.getAccession() != null) {
            askQuery.addWhere("?uri", Oeso.fromAccession, SPARQLDeserializers.nodeURI(germplasm.getAccession()));
        }

        return sparql.executeAskQuery(askQuery);
    }

    public boolean labelExistsCaseInsensitiveWithCache(String label, URI rdfType) {
        Set<String> labelsSet = cache.get(new Key(rdfType), this::getAllLabels);
        return (labelsSet.contains(label.toLowerCase()));
    }

    private Set getAllLabels(URI rdfType) {
        HashSet<String> labels = new HashSet();

        try {
            SelectBuilder query = new SelectBuilder()
                    .addVar("?label")
                    .from(sparql.getDefaultGraph(GermplasmModel.class).toString())
                    .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(rdfType))
                    .addWhere("?uri", RDFS.label, "?label");

            List<SPARQLResult> results = sparql.executeSelectQuery(query);

            results.forEach(result -> {
                labels.add(result.getStringValue("label").toLowerCase());
            });
        } catch (SPARQLException error) {
            throw new RuntimeException(error);
        }

        return labels;
    }

    private Set getAllLabels(Key key) {
        return getAllLabels(key.rdfType);
    }

    public GermplasmModel create(GermplasmModel germplasm, UserModel user) throws Exception {
        if (germplasm.getAttributes() != null) {
            getAttributesCollection().createIndex(Indexes.ascending("uri"), new IndexOptions().unique(true));
            nosql.startTransaction();
            sparql.startTransaction();
            try {
                sparql.create(germplasm);
                GermplasmAttributeModel model = new GermplasmAttributeModel();
                model.setUri(germplasm.getUri());
                model.setAttribute(germplasm.getAttributes());
                nosql.create(model, GermplasmAttributeModel.class, ATTRIBUTES_COLLECTION_NAME, null);
                nosql.commitTransaction();
                sparql.commitTransaction();
            } catch (Exception ex) {
                nosql.rollbackTransaction();
                sparql.rollbackTransaction(ex);
            }
        } else {
            sparql.create(germplasm);
        }

        return germplasm;

    }

    public GermplasmModel get(URI uri, UserModel user) throws Exception {
        GermplasmModel germplasm = sparql.getByURI(GermplasmModel.class, uri, user.getLanguage());
        if (germplasm != null) {
            GermplasmAttributeModel storedAttributes = getStoredAttributes(germplasm.getUri());
            if (storedAttributes != null) {
                germplasm.setAttributes(storedAttributes.getAttribute());
            }
        }
        return germplasm;

    }

    public List<GermplasmModel> getList(List<URI> uris, String lang, Boolean withMetadata) throws Exception {
        List<GermplasmModel> germplasmList = sparql.getListByURIs(GermplasmModel.class, uris, lang);
        
        if (withMetadata) {
            //get metadata part from mongo
            for (GermplasmModel germplasm:germplasmList) {
                GermplasmAttributeModel storedAttributes = getStoredAttributes(germplasm.getUri());
                if (storedAttributes != null) {
                    germplasm.setAttributes(storedAttributes.getAttribute());
                }
            }
        }
        
        return germplasmList;
        
    }

    public ListWithPagination<GermplasmModel> search(
            UserModel user,
            String uri,
            URI rdfType,
            String label,
            URI species,
            URI variety,
            URI accession,
            String institute,
            Integer productionYear,
            URI experiment,
            Document metadata,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {
        
        final Set<URI> filteredUris;
        if (metadata != null) {
            filteredUris = filterURIsOnAttributes(metadata);
        } else {
            filteredUris = null;
        }

        if (metadata != null && CollectionUtils.isEmpty(filteredUris)) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        return sparql.searchWithPagination(
                GermplasmModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {

                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();

                    appendRegexUriFilter(select, uri);
                    appendRdfTypeFilter(select, rdfType);
                    appendRegexLabelAndSynonymFilter(select, label);
                    appendSpeciesFilter(select, species);
                    appendVarietyFilter(select, variety);
                    appendAccessionFilter(select, accession);
                    appendRegexInstituteFilter(select, institute);
                    appendProductionYearFilter(select, productionYear);
                    appendURIsFilter(select, filteredUris);

                    ElementGroup germplasmGraphElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, defaultGraph);
                    appendExperimentFilter(select, experiment, germplasmGraphElem);
                },
                orderByList,
                page,
                pageSize
        );
    }

    public List<GermplasmModel> searchForExport(
            UserModel user,
            String uri,
            URI rdfType,
            String label,
            URI species,
            URI variety,
            URI accession,
            String institute,
            Integer productionYear,
            URI experiment,
            Document metadata
    ) throws Exception {        

       ListWithPagination<GermplasmModel> germplasmList = search(
               user,
               uri,
               rdfType,
               label,
               species,
               variety,
               accession,
               institute,
               productionYear,
               experiment,
               metadata,
               null,
               null,
               null
       );


        //get metadata part from mongo
        for (GermplasmModel germplasm:germplasmList.getList()) {
            GermplasmAttributeModel storedAttributes = getStoredAttributes(germplasm.getUri());
            if (storedAttributes != null) {
                germplasm.setAttributes(storedAttributes.getAttribute());
            }
        }
        return germplasmList.getList();
    }

    public List<URI> getGermplasmURIsBySpecies(List<URI> species, String lang) throws Exception {
        return sparql.searchURIs(
                GermplasmModel.class,
                lang,
                (SelectBuilder select) -> {
                    select.addFilter(SPARQLQueryHelper.inURIFilter(GermplasmModel.SPECIES_URI_SPARQL_VAR, species));
                }
        );
    }

    private void appendRegexUriFilter(SelectBuilder select, String uri) {
        if (uri != null) {
            try {
                uri = NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri)).toString();
            } catch (Exception e) {                
            } finally {
                select.addFilter(SPARQLQueryHelper.regexFilterOnURI(GermplasmModel.URI_FIELD, uri,"i"));
            }            
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

    public boolean isPlantMaterialLot(URI rdfType) throws SPARQLException {
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(rdfType), Ontology.subClassAny, Oeso.PlantMaterialLot)
        );
    }

    public void delete(URI uri) throws Exception {
        GermplasmAttributeModel attributes = getStoredAttributes(uri);
        if (attributes != null) {
            nosql.startTransaction();
            sparql.startTransaction();
            try {
                sparql.delete(GermplasmModel.class, uri);
                MongoCollection collection = getAttributesCollection();
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
     * @return 
     */
    public List<String> getDistinctGermplasAttributes(){
        // Make an aggregation on ATTRIBUTES_COLLECTION
        
        // 1.project - Transform document to multiple array elements
        // 2.unwind - Transform multiple array elements to simple array
        // 3.group - Return unique arrays keys only
        Set<Document> germplasAttributesKeys = nosql.aggregate(ATTRIBUTES_COLLECTION_NAME, Arrays.asList(
            project(computed("attributes", new Document("$objectToArray", "$attribute"))),
            unwind("$attributes"),
            group("$attributes.k")
        ));
        
        // Return a list of unique arrays keys
        List<String> germplasAttributesKeysList = new ArrayList<>();
        for (Document germplasAttribute : germplasAttributesKeys){
            germplasAttributesKeysList.add((String) germplasAttribute.get("_id"));
        }
        return germplasAttributesKeysList;
    }

    public ListWithPagination<ExperimentModel> getExpFromGermplasm(
            UserModel currentUser,
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

    private void appendExperimentFilter(SelectBuilder select, URI xpUri, ElementGroup germplasmGraphElem) throws SPARQLException {
        if (xpUri != null) {

            // search into global experiment graph, which species are associated to this xp
            Node xpGlobalGraphNode = sparql.getDefaultGraph(ExperimentModel.class);
            Node xpNode = SPARQLDeserializers.nodeURI(xpUri);
            Node hasSpecies = SPARQLDeserializers.nodeURI(Oeso.hasSpecies);
            Var speciesVar = makeVar("species");
            select.addGraph(xpGlobalGraphNode,xpNode,hasSpecies,speciesVar);

            // join xp species to germplasm associated to these species
            Var germplasmVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Node fromSpecies = SPARQLDeserializers.nodeURI(Oeso.fromSpecies);
            germplasmGraphElem.addTriplePattern(new Triple(germplasmVar,fromSpecies,speciesVar));
        }
    }

    private GermplasmAttributeModel getStoredAttributes(URI uri) {
        GermplasmAttributeModel storedAttributes = null;
        try {
            storedAttributes = nosql.findByURI(GermplasmAttributeModel.class, ATTRIBUTES_COLLECTION_NAME, uri);
        } catch (NoSQLInvalidURIException e) {
        }
        return storedAttributes;

    }

    private Set<URI> filterURIsOnAttributes(Document metadata) {
        Document filter = new Document();
        if (metadata != null) {
            for (String key:metadata.keySet()) {
                Document regexFilter = new Document();
                regexFilter.put("$regex", ".*" + Pattern.quote(metadata.get(key).toString()) + ".*" );
                // Case ignore
                regexFilter.put("$options", "i" );
                
                filter.put("attribute." + key, regexFilter);
            }
        }
        Set<URI> germplasmURIs = nosql.distinct("uri", URI.class, ATTRIBUTES_COLLECTION_NAME, filter);
        return germplasmURIs;
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
    
    public ListWithPagination<GermplasmModel> brapiSearch(UserModel user, URI germplasmDbId, String germplasmName, String germplasmSpecies, int page, int pageSize) throws Exception {
        
        final Set<URI> speciesURIs;
        if (germplasmSpecies != null) {
            List<URI> species = sparql.searchURIs(
                                GermplasmModel.class,
                                user.getLanguage(),
                                (SelectBuilder select) -> {                              
                                    appendRdfTypeFilter(select, new URI(Oeso.Species.toString()));                            
                                    appendRegexLabelFilter(select, germplasmSpecies);
                                });

            speciesURIs = new HashSet(species);
        } else {
            speciesURIs = new HashSet();
        }    
        
        ListWithPagination<GermplasmModel> germplasmList = sparql.searchWithPagination(
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
        
        return germplasmList;
    }


    private static class Key {

        final URI rdfType;

        public Key(URI rdfType) {
            this.rdfType = rdfType;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.rdfType);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (!Objects.equals(this.rdfType, other.rdfType)) {
                return false;
            }
            return true;
        }
    }

}
