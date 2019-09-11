//******************************************************************************
//                                GeneticInformationDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 sept. 2019
// Contact:alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.GeneticInformation;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Genetic Information DAO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GeneticInformationDAO extends Rdf4jDAO<GeneticInformation> {
    final static Logger LOGGER = LoggerFactory.getLogger(GeneticInformationDAO.class);
    
    private final String GENUS = "genus";
    private final String GENUS_LABEL = "genusLabel";
    private final String SPECIES = "species";
    private final String SPECIES_LABEL = "speciesLabel";
    private final String VARIETY = "variety";
    private final String VARIETY_LABEL = "varietyLabel";
    private final String ACCESSION = "accession";
    private final String ACCESSION_NUMBER = "accessionNumber";
    private final String LOT = "lot";
    private final String LOT_LABEL = "lotLabel";
    
    public String uri;
    
    public GeneticInformationDAO(String uri) {
        this.uri = uri;
    }

    @Override
    public List<GeneticInformation> create(List<GeneticInformation> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<GeneticInformation> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<GeneticInformation> update(List<GeneticInformation> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GeneticInformation find(GeneticInformation object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GeneticInformation findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<GeneticInformation> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /***
     * 
     * @param uri
     * @return all genetic Information linked to this geneticResource 
     */
    public GeneticInformation findByURI(String uri) {
        String geneticResourceType = getGeneticResourceType(uri);        
        SPARQLQueryBuilder query = prepareSearchQuery(uri, geneticResourceType);
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        GeneticInformation geneticInfo = new GeneticInformation();
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                geneticInfo = getGeneticInfoFromBindingSet(bindingSet);                
            }
        }
        return geneticInfo;
    }

    private SPARQLQueryBuilder prepareSearchQuery(String uri, String type) {
        String language = "en";
        String filterType = new String();
        
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
                       
        if (type.equals(Oeso.CONCEPT_GENUS.toString())) {
            query.appendSelect("?" + GENUS_LABEL);
            query.appendToBody("?" + GENUS + "<" + RDF.type.toString() + ">" + "<" + type + ">" + " . ");
            query.appendAndFilter("REGEX ( str(?" + GENUS + "),\".*" + uri + ".*\",\"i\")");
            query.beginBodyOptional();
            query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();
            
        } else if (type.equals(Oeso.CONCEPT_SPECIES.toString())){ 
            query.appendSelect("?" + SPECIES_LABEL);   
            query.appendSelect("?" + GENUS_LABEL);
            query.appendToBody("?" + SPECIES + "<" + RDF.type.toString() + ">" + "<" + type + ">" + " . ");
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.beginBodyOptional();
            query.appendToBody("?" + GENUS + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
            query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")"); 
            query.appendAndFilter("REGEX ( str(?" + SPECIES + "),\".*" + uri + ".*\",\"i\")");
            
        } else if (type.equals(Oeso.CONCEPT_VARIETY.toString())) {
            query.appendSelect("?" + SPECIES_LABEL);   
            query.appendSelect("?" + GENUS_LABEL);
            query.appendSelect("?" + VARIETY_LABEL);
            query.appendToBody("?" + VARIETY + "<" + RDF.type.toString() + ">" + "<" + type + ">" + " . ");
            query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
            query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");            
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.beginBodyOptional();
            query.appendToBody("?" + GENUS + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
            query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")"); 
            query.appendAndFilter("REGEX ( str(?" + VARIETY + "),\".*" + uri + ".*\",\"i\")");
            
        } else if (type.equals(Oeso.CONCEPT_ACCESSION.toString())) {
            query.appendSelect("?" + ACCESSION_NUMBER);
            query.appendSelect("?" + SPECIES_LABEL);   
            query.appendSelect("?" + GENUS_LABEL);
            query.appendSelect("?" + VARIETY_LABEL);
            query.appendToBody("?" + ACCESSION + "<" + RDF.type.toString() + ">" + "<" + type + ">" + " . ");
            query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_HAS_ACCESSION_NUMBER.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
            query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
            
            query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
            query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");            
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.beginBodyOptional();
            query.appendToBody("?" + GENUS + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
            query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")"); 
            query.appendAndFilter("REGEX ( str(?" + ACCESSION + "),\".*" + uri + ".*\",\"i\")");
            
        } else if (type.equals(Oeso.CONCEPT_PLANT_MATERIAL_LOT.toString())) {
            query.appendSelect("?" + LOT_LABEL);
            query.appendSelect("?" + ACCESSION_NUMBER);
            query.appendSelect("?" + SPECIES_LABEL);   
            query.appendSelect("?" + GENUS_LABEL);
            query.appendSelect("?" + VARIETY_LABEL);
            query.appendToBody("?" + LOT + "<" + RDF.type.toString() + ">" + "<" + type + ">" + " . ");
            query.beginBodyOptional();        
            query.appendToBody("?" + LOT + " <" + RDFS.label.toString() + "> " + "?" + LOT_LABEL + " . ");
            query.endBodyOptional();
            
            query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_HAS_PLANT_MATERIAL_LOT.toString() + "> " + "?" + LOT + " . ");
            query.appendToBody("?" + ACCESSION + " <" + Oeso.RELATION_HAS_ACCESSION_NUMBER.toString() + "> " + "?" + ACCESSION_NUMBER + " . ");
            query.appendToBody("?" + VARIETY + " <" + Oeso.RELATION_HAS_ACCESSION.toString() + "> " + "?" + ACCESSION + " . ");
            
            query.appendToBody("?" + VARIETY + " <" + RDFS.label.toString() + "> " + "?" + VARIETY_LABEL + " . ");
            query.appendToBody("?" + SPECIES + " <" + Oeso.RELATION_HAS_VARIETY.toString() + "> " + "?" + VARIETY + " . ");            
            query.appendToBody("?" + SPECIES + " <" + RDFS.label.toString() + "> " + "?" + SPECIES_LABEL + " . ");
            query.beginBodyOptional();
            query.appendToBody("?" + GENUS + " <" + Oeso.RELATION_HAS_SPECIES.toString() + "> " + "?" + SPECIES + " . ");
            query.appendToBody("?" + GENUS + " <" + RDFS.label.toString() + "> " + "?" + GENUS_LABEL + " . ");
            query.endBodyOptional();
            query.appendAndFilter("LANG(?" + SPECIES_LABEL + ") = \"\" || LANGMATCHES(LANG(?" + SPECIES_LABEL + "), \"" + language + "\")"); 
            query.appendAndFilter("REGEX ( str(?" + LOT + "),\".*" + uri + ".*\",\"i\")");
        }  
        
        LOGGER.debug(SPARQL_QUERY + query.toString());              
                
        return query;
    }

    private GeneticInformation getGeneticInfoFromBindingSet(BindingSet bindingSet) {
        GeneticInformation geneticInfo = new GeneticInformation();
        if (bindingSet.getValue(LOT_LABEL) != null) {
           geneticInfo.setPlantMaterialLot(bindingSet.getValue(LOT_LABEL).stringValue());
        }
        if (bindingSet.getValue(ACCESSION_NUMBER) != null) {
           geneticInfo.setAccessionNumber(bindingSet.getValue(ACCESSION_NUMBER).stringValue());
        }
        if (bindingSet.getValue(VARIETY_LABEL) != null) {
           geneticInfo.setVarietyLabel(bindingSet.getValue(VARIETY_LABEL).stringValue());
        }   
        if (bindingSet.getValue(SPECIES_LABEL) != null) {
           geneticInfo.setSpeciesLabel(bindingSet.getValue(SPECIES_LABEL).stringValue());
        }
        if (bindingSet.getValue(GENUS_LABEL) != null) {
           geneticInfo.setGenusLabel(bindingSet.getValue(GENUS_LABEL).stringValue());
        }
        return geneticInfo;
    }
    
    private String getGeneticResourceType(String uri) {
        SPARQLQueryBuilder query = prepareTypeQuery(uri);
        String type = new String();
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());        
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    if (bindingSet.getValue(RDF_TYPE) != null) {
                        type = bindingSet.getValue(RDF_TYPE).stringValue();
                    } 
                }
            }             
        return type; 
    }

    private SPARQLQueryBuilder prepareTypeQuery(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendSelect("?" + RDF_TYPE);
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendAndFilter("REGEX ( str(?" + URI + "),\".*" + uri + ".*\",\"i\")");
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        
        return query;
    }

}
