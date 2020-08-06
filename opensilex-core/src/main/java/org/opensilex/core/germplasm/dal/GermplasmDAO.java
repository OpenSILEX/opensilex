//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.query.BooleanExpression;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.service.NoSQLService;
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
    protected final NoSQLService nosql;

    public GermplasmDAO(SPARQLService sparql, NoSQLService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }

//    public GermplasmModel create(GermplasmModel instance) throws Exception {
//        sparql.create(instance);
//        return instance;
//    }
    public GermplasmModel update(GermplasmModel germplasm) throws Exception {
        sparql.update(germplasm);
        if (germplasm.getAttributes() != null) {
            GermplasmAttributeModel model = new GermplasmAttributeModel();
            model.setUri(germplasm.getUri());
            model.setAttribute(germplasm.getAttributes());
            addAttributes(model);
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
                .addWhere("?uri", RDFS.label, germplasm.getLabel());

        if (germplasm.getFromSpecies() != null) {
            askQuery.addWhere("?uri", Oeso.fromSpecies, SPARQLDeserializers.nodeURI(germplasm.getFromSpecies()));
        } else if (germplasm.getFromVariety() != null) {
            askQuery.addWhere("?uri", Oeso.fromVariety, SPARQLDeserializers.nodeURI(germplasm.getFromVariety()));
        } else if (germplasm.getFromAccession() != null) {
            askQuery.addWhere("?uri", Oeso.fromAccession, SPARQLDeserializers.nodeURI(germplasm.getFromAccession()));
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
        sparql.create(germplasm);

        if (germplasm.getAttributes() != null) {
            GermplasmAttributeModel model = new GermplasmAttributeModel();
            model.setUri(germplasm.getUri());
            model.setAttribute(germplasm.getAttributes());
            addAttributes(model);
        }

        return germplasm;
    }

    public GermplasmModel get(URI uri, UserModel user) throws Exception {
        GermplasmModel germplasm = sparql.getByURI(GermplasmModel.class, uri, user.getLanguage());
        GermplasmModel germplasmWithAttr = null;
        if (germplasm != null) {
            germplasmWithAttr = getAttributesByURI(germplasm);
        }

        return germplasmWithAttr;
    }

    public ListWithPagination<GermplasmModel> search(
            UserModel user,
            URI uri,
            URI rdfType,
            String label,
            URI species,
            URI variety,
            URI accession,
            String institute,
            Integer productionYear,
            URI experiment,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                GermplasmModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {
                    appendUriFilter(select, uri);
                    appendRdfTypeFilter(select, rdfType);
                    appendRegexLabelAndSynonymFilter(select, label);
                    appendSpeciesFilter(select, species);
                    appendVarietyFilter(select, variety);
                    appendAccessionFilter(select, accession);
                    appendInstituteFilter(select, institute);
                    appendProductionYearFilter(select, productionYear);
                    appendExperimentFilter(select, experiment);
                },
                orderByList,
                page,
                pageSize
        );
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
            select.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.NAME_FIELD, label));
        }
    }

    private void appendRegexLabelAndSynonymFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.or(SPARQLQueryHelper.regexFilter(GermplasmModel.NAME_FIELD, label), SPARQLQueryHelper.regexFilter(GermplasmModel.SYNONYM_VAR, label)));
        }
    }

    private void appendSpeciesFilter(SelectBuilder select, URI species) throws Exception {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.SPECIES_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(species.toString()))));
        }
    }

    private void appendVarietyFilter(SelectBuilder select, URI variety) throws Exception {
        if (variety != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.VARIETY_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(variety.toString()))));
        }
    }

    private void appendAccessionFilter(SelectBuilder select, URI accession) throws Exception {
        if (accession != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.ACCESSION_URI_SPARQL_VAR, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(accession.toString()))));
        }
    }

    private void appendInstituteFilter(SelectBuilder select, String institute) throws Exception {
        if (institute != null) {
            select.addFilter(SPARQLQueryHelper.eq(GermplasmModel.INSTITUTE_SPARQL_VAR, institute));
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
        sparql.delete(GermplasmModel.class, uri);
        deleteAttributes(uri);
    }

    public boolean hasRelation(URI uri, Property ontologyRelation) throws SPARQLException {
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(uriVar, ontologyRelation, SPARQLDeserializers.nodeURI(uri))
        );
    }

    public ListWithPagination<ExperimentModel> getExpFromGermplasm(
            UserModel currentUser,
            URI uri,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                ExperimentModel.class,
                currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    appendGermplasmFilter(select, uri);
                },
                orderByList,
                page,
                pageSize);

    }

    public ListWithPagination<GermplasmModel> getGermplasmFromExp(
            UserModel currentUser,
            URI uri,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) throws Exception {

        return sparql.searchWithPagination(GermplasmModel.class, currentUser.getLanguage(),
                (SelectBuilder select) -> {
                    appendExperimentFilter(select, uri);
                },
                orderByList, page, pageSize);

    }

    private void appendGermplasmFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addWhere(makeVar("so"), Oeso.hasGermplasm, SPARQLDeserializers.nodeURI(uri));
            select.addWhere(makeVar("so"), Oeso.participatesIn, makeVar(SPARQLResourceModel.URI_FIELD));
        }
    }

    private void appendExperimentFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addWhere(makeVar("so"), Oeso.hasGermplasm, makeVar(SPARQLResourceModel.URI_FIELD));
            //String uristring = SPARQLDeserializers.getExpandedURI(uri.toString());
            select.addWhere(makeVar("so"), Oeso.participatesIn, SPARQLDeserializers.nodeURI(uri));
        }
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

    private URI addAttributes(GermplasmAttributeModel model) throws NamingException, URISyntaxException, IOException {
        Boolean exist = existingAttributes(model.getUri());
        URI uri = new URI(SPARQLDeserializers.getExpandedURI(model.uri.toString()));
        model.setUri(uri);
        if (!exist) {
            nosql.create(model);
        } else {
            deleteAttributes(uri);
            nosql.create(model);
        }

        return model.getUri();
    }

    private GermplasmModel getAttributesByURI(GermplasmModel model) throws NamingException, IOException, URISyntaxException {
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {

            try (JDOQLTypedQuery<GermplasmAttributeModel> tq = persistenceManager.newJDOQLTypedQuery(GermplasmAttributeModel.class)) {
                QGermplasmAttributeModel cand = QGermplasmAttributeModel.candidate();

                URI uri = new URI(SPARQLDeserializers.getExpandedURI(model.getUri().toString()));
                BooleanExpression expr = cand.uri.eq(uri);

                GermplasmAttributeModel result = tq.filter(expr).executeUnique();

                if (result != null) {
                    model.setAttributes(result.attribute);
                }

                return model;
            }
        }
    }

    private Boolean existingAttributes(URI germplasmURI) throws URISyntaxException, IOException, NamingException {
        Boolean exist = false;
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {

            try (JDOQLTypedQuery<GermplasmAttributeModel> tq = persistenceManager.newJDOQLTypedQuery(GermplasmAttributeModel.class)) {
                QGermplasmAttributeModel cand = QGermplasmAttributeModel.candidate();

                URI uri = new URI(SPARQLDeserializers.getExpandedURI(germplasmURI.toString()));
                BooleanExpression expr = cand.uri.eq(uri);

                GermplasmAttributeModel result = tq.filter(expr).executeUnique();

                if (result != null) {
                    exist = true;
                }

                return exist;
            }

        }
    }

    private void deleteAttributes(URI uri) throws NamingException, IOException, URISyntaxException {
        try (PersistenceManager persistenceManager = nosql.getPersistentConnectionManager()) {
            try (JDOQLTypedQuery<GermplasmAttributeModel> tq = persistenceManager.newJDOQLTypedQuery(GermplasmAttributeModel.class)) {
                QGermplasmAttributeModel cand = QGermplasmAttributeModel.candidate();
                URI expandedURI = new URI(SPARQLDeserializers.getExpandedURI(uri.toString()));
                BooleanExpression expr = cand.uri.eq(expandedURI);

                GermplasmAttributeModel attribute = tq.filter(expr).executeUnique();
                if (attribute != null) {
                    persistenceManager.deletePersistent(attribute);
                }
            }
        }
    }

}
