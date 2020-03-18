/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.dal;

import java.util.ArrayList;
import opensilex.service.model.Property;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;
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
        graph = "set/germplasm",
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
    
    @SPARQLProperty(
        ontology = RDF.class,
        property = "type",
        required = true
    )
    String rdfType;
    public static final String RDF_TYPE_VAR = "rdfType";
    
    ArrayList<Property> properties;
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }      
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromGenus"
    )
    String genus;
    public static final String GENUS_URI_SPARQL_VAR = "genus";

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromSpecies"
    )
    String species;
    public static final String SPECIES_URI_SPARQL_VAR = "species";
    
    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromVariety"
    )
    String variety;
    public static final String VARIETY_URI_SPARQL_VAR = "variety";
    
    @SPARQLProperty(
        ontology = Oeso.class,
        property = "fromAccession"
    )
    String accession;
    public static final String ACCESSION_URI_SPARQL_VAR = "accession";    

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }
    
    @Override
    public String[] getUriSegments(GermplasmModel instance) {
        return new String[]{
                instance.getLabel()
        };
    }
    
}
