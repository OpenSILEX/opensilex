/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.germplasm.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * DTO representing JSON for searching germplasm
 * @author Alice Boizet
 */
public class GermplasmSearchDTO {
    /**
     * Germplasm URI
     */
    protected URI uri;
    
    /**
     * Germplasm Type : Species, Variety, Accession or subclass of PlantMaterialLot
     */
    protected URI rdfType;
    
    /**
     * Germplasm label
     */
    protected String label;
    
    /**
     * Germplasm species URI
     */
    protected URI fromSpecies;
    
    /**
     * Germplasm Variety URI
     */
    protected URI fromVariety;
    
    /**
     * Germplasm Accession URI
     */
    protected URI fromAccession;

    /**
     * institute where the accession has been created
     */
    protected String institute;
    
    /**
     * productionYear
     */
    protected Integer productionYear;
    
    /**
     * experiment
     */
    protected URI experiment;
    
    @ValidURI
    @ApiModelProperty(value = "Germplasm URI", example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    @ValidURI
    @ApiModelProperty(value = "Germplasm type", example = "http://www.opensilex.org/vocabulary/oeso#SeedLot")
    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    @ApiModelProperty(value = "Germplasm label", example = "SL_001")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ValidURI
    @ApiModelProperty(value = "species URI", example = "http://opensilex.dev/opensilex/id/species#zeamays")
    public URI getFromSpecies() {
        return fromSpecies;
    }

    public void setFromSpecies(URI fromSpecies) {
        this.fromSpecies = fromSpecies;
    }

    @ValidURI
    @ApiModelProperty(value = "variety URI", example = "http://opensilex.dev/opensilex/id/variety#B73")
    public URI getFromVariety() {
        return fromVariety;
    }

    public void setFromVariety(URI fromVariety) {
        this.fromVariety = fromVariety;
    }

    @ValidURI
    @ApiModelProperty(value = "accession URI", example = "http://opensilex.dev/opensilex/id/accession#B73_INRA")
    public URI getFromAccession() {
        return fromAccession;
    }

    public void setFromAccession(URI fromAccession) {
        this.fromAccession = fromAccession;
    }

    @ApiModelProperty(value = "Institute where the accession has been created", example = "INRA")
    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    @ApiModelProperty(value = "Year when the resource has been produced", example = "2020")
    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    } 

    @ApiModelProperty(value = "experiment in which the germplasm has been used", example = "dev-experiment:expA01")
    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
    
}
