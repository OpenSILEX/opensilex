/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.dal;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author boizetal
 */
public class GermplasmDAO {
    
    protected final SPARQLService sparql;
    
    public GermplasmDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    
    public GermplasmModel create(GermplasmModel instance) throws Exception {
        //checkURIs(instance);
        sparql.create(instance);
        return instance;
    }

    public boolean germplasmLabelExists(String label) throws Exception {
        return sparql.existsByUniquePropertyValue(
                GermplasmModel.class,
                RDFS.label,
                label
        );
    }

    public GermplasmModel create(
            URI uri, 
            String label, 
            URI rdfType, 
            URI fromSpecies, 
            URI fromVariety, 
            URI fromAccession
    ) throws Exception {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setUri(uri);
        germplasm.setLabel(label);
        germplasm.setRdfType(rdfType);
        
        if (fromAccession != null) {
            germplasm.setAccession(fromAccession);
        } else {
            if (fromVariety != null) {
                germplasm.setVariety(fromVariety);
            } else {
                germplasm.setSpecies(fromSpecies);            
            }
        }
        
        check(germplasm);        
        sparql.create(germplasm);
        
        return germplasm;
    }
    
   
    /**
     * Check that all URI(s) which refers to a non {@link org.opensilex.sparql.annotations.SPARQLResource}-compliant model exists.
     *
     * @param model the experiment for which we check if all URI(s) exists
     * @throws SPARQLException          if the SPARQL uri checking query fail
     * @throws IllegalArgumentException if the given model contains a unknown URI
     */
    protected void check(GermplasmModel model) throws SPARQLException, IllegalArgumentException, URISyntaxException {
        // check rdfType
        if (!sparql.isSubClassOf(model.getRdfType(), new URI(Oeso.Germplasm.getURI()))) {
            throw new IllegalArgumentException("wrong rdfType given : " + model.getRdfType().toString());
        }
        
        // check that fromAccession, fromVariety or fromSpecies are given and exist
        boolean missingLink = true;
        if (model.getRdfType() == new URI(Oeso.PlantMaterialLot.getURI())) {
            if (model.getAccession() != null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getAccession())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown accession : " + model.getAccession().toString());
                }                
            }
            if (model.getVariety() != null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getVariety())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown variety : " + model.getVariety().toString());
                }  
            }
            if (model.getSpecies()!= null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getVariety())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown species : " + model.getSpecies().toString());
                }  
            }
            
        } else if (model.getRdfType() == new URI(Oeso.Accession.getURI())) {
            if (model.getVariety() != null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getVariety())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown variety : " + model.getVariety().toString());
                }  
            }
            if (model.getSpecies()!= null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getVariety())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown species : " + model.getSpecies().toString());
                }  
            }
            
        } else {
            if (model.getSpecies()!= null) {
                missingLink = false;
                if (!sparql.uriExists(model.getRdfType(), model.getVariety())) {
                    throw new IllegalArgumentException("Trying to insert a germplasm with an unknown species : " + model.getSpecies().toString());
                }  
            }
        }
        
        if (missingLink) {
            throw new IllegalArgumentException("Missing a link to another germplasm (fromSpecies, fromVariety or fromAccession)");
        }

    }


}
