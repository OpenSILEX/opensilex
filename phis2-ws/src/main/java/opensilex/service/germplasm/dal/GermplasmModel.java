/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.dal;

import java.net.URI;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author boizetal
 */

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Germplasm",
        graph = "germplasm",
        prefix = "germplasm"
)
public class GermplasmModel extends SPARQLResourceModel implements ClassURIGenerator<GermplasmModel>{
    
    @SPARQLProperty(
        ontology = RDFS.class,
        property = "label",
        required = true
    )
    String label;
    public static final String LABEL_VAR = "label";
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromSpecies"
    )
    GermplasmModel species;
    public static final String SPECIES_URI_SPARQL_VAR = "species";
    
    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromVariety"
    )
    GermplasmModel variety;
    public static final String VARIETY_URI_SPARQL_VAR = "variety";
    
    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromAccession"
    )
    GermplasmModel accession;
    public static final String ACCESSION_URI_SPARQL_VAR = "accession";    

    public GermplasmModel getSpecies() {
        return species;
    }

    public void setSpecies(GermplasmModel species) {
        this.species = species;
    }

    public GermplasmModel getVariety() {
        return variety;
    }

    public void setVariety(GermplasmModel variety) {
        this.variety = variety;
    }

    public GermplasmModel getAccession() {
        return accession;
    }

    public void setAccession(GermplasmModel accession) {
        this.accession = accession;
    }
    
    @Override
    public String[] getUriSegments(GermplasmModel instance) {
        String germplasmType = instance.getType().getFragment();
        
        return new String[]{                
                germplasmType + "_" + instance.getLabel()
        };
    }
    
    
    
}
