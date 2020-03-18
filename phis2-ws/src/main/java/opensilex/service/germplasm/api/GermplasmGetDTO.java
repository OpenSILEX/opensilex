/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.germplasm.api;

import java.net.URI;

/**
 *
 * @author boizetal
 */
class GermplasmGetDTO {
    
    protected URI uri;
    
    protected String rdfType;
    
    protected String label;
    
    protected String genus;
    
    protected String species;
    
    protected String variety;
    
    protected String accession;
    
    protected String plantMaterialLot;  

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

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

    public String getPlantMaterialLot() {
        return plantMaterialLot;
    }

    public void setPlantMaterialLot(String plantMaterialLot) {
        this.plantMaterialLot = plantMaterialLot;
    }    
    
}
