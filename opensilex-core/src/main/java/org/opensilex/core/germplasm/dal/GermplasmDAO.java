//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.dal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * Germplasm DAO
 * @author Alice Boizet
 */
public class GermplasmDAO {
    
    private static final Cache<Key, Set> cache= Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build();
    
    protected final SPARQLService sparql;
    
    public GermplasmDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public GermplasmModel create(GermplasmModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }
    
    public GermplasmModel update(GermplasmModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }
   
    public boolean labelExistsCaseSensitive(String label, URI rdfType) throws Exception {       
        AskBuilder askQuery = new AskBuilder()
                .from(sparql.getDefaultGraph(GermplasmModel.class).toString())
                .addWhere("?uri", RDF.type, SPARQLDeserializers.nodeURI(rdfType))
                .addWhere("?uri",RDFS.label, GermplasmModel.LABEL_VAR);
                //.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_VAR, "^" + label + "$", "i"));
        
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
                .addWhere("?uri",RDFS.label, "?label");
            
            List<SPARQLResult> results = sparql.executeSelectQuery(query);
            
            for (SPARQLResult result:results) {
                labels.add(result.getStringValue("label").toLowerCase());
            }            
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        
        return labels;
    }
    
    private Set getAllLabels(Key key) {
        return getAllLabels(key.rdfType);
    }
 
    public GermplasmModel create(
            UserModel user,
            URI uri, 
            String label, 
            URI rdfType, 
            URI fromSpecies, 
            URI fromVariety, 
            URI fromAccession,
            String comment,
            String institute,
            Integer productionYear
    ) throws Exception {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setUri(uri);
        germplasm.setLabel(new SPARQLLabel(label, null));
        germplasm.setType(rdfType);        
        
        if (fromAccession != null) {
            GermplasmModel accession = new GermplasmModel();
            accession.setUri(fromAccession);
            germplasm.setAccession(accession);
        }

        if (fromVariety != null) {
            GermplasmModel variety = new GermplasmModel();
            variety.setUri(fromVariety);        
            germplasm.setVariety(variety);
        }
        
        if (fromSpecies != null) {
            GermplasmModel species = new GermplasmModel();
            species.setUri(fromSpecies);
            germplasm.setSpecies(species);             
        }  
        
        germplasm.setComment(comment);
        germplasm.setInstitute(institute);
        germplasm.setProductionYear(productionYear);
        
        sparql.create(germplasm);
        
        return germplasm;
    }  
    
    public GermplasmModel get(URI uri, UserModel user) throws Exception {
        return sparql.getByURI(GermplasmModel.class, uri, user.getLanguage());
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
            List<OrderBy> orderByList, 
            Integer page, 
            Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                GermplasmModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {
                    appendUriFilter(select, uri);
                    appendRdfTypeFilter(select, rdfType);
                    appendRegexLabelFilter(select, label);
                    appendSpeciesFilter(select, species);
                    appendVarietyFilter(select, variety);
                    appendAccessionFilter(select, accession);
                    appendInstituteFilter(select, institute);
                    appendProductionYearFilter(select, productionYear);
                },
                orderByList,
                page,
                pageSize
        );
    }
    
    private void appendUriFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            select.addFilter(SPARQLQueryHelper.eq
            (GermplasmModel.TYPE_FIELD,  NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString()))));
        }
    }
    private void appendRdfTypeFilter(SelectBuilder select, URI rdfType) throws Exception {
        if (rdfType != null) {
            select.addFilter(SPARQLQueryHelper.eq
            (GermplasmModel.TYPE_FIELD,  NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(rdfType.toString()))));
        }
    }

    private void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(GermplasmModel.LABEL_VAR, label));
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
    }

    public boolean hasRelation(URI uri, Property ontologyRelation) throws SPARQLException {
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        return sparql.executeAskQuery(new AskBuilder()
                .addWhere(uriVar, ontologyRelation, SPARQLDeserializers.nodeURI(uri))
        );
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
