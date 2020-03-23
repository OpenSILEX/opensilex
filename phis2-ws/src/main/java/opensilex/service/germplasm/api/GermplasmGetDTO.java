//******************************************************************************
//                          GermplasmGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.germplasm.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import opensilex.service.germplasm.dal.GermplasmModel;
import opensilex.service.model.Property;

/**
 * DTO representing JSON for searching germplasm or getting them by uri
 * @author Alice Boizet
 */
class GermplasmGetDTO {
    
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
    
    @ApiModelProperty(value = "Germplasm URI", example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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
    
    @ApiModelProperty(value = "species URI", example = "http://opensilex.dev/opensilex/id/species#zeamays")
    public URI getFromSpecies() {
        return fromSpecies;
    }    
    
    public void setFromSpecies(URI fromSpecies) {
        this.fromSpecies = fromSpecies;
    }

    @ApiModelProperty(value = "variety URI", example = "http://opensilex.dev/opensilex/id/variety#B73")
    public URI getFromVariety() {
        return fromVariety;
    }

    public void setFromVariety(URI fromVariety) {
        this.fromVariety = fromVariety;
    }

    @ApiModelProperty(value = "accession URI", example = "http://opensilex.dev/opensilex/id/accession#B73_INRA")
    public URI getFromAccession() {
        return fromAccession;
    }

    public void setFromAccession(URI fromAccession) {
        this.fromAccession = fromAccession;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model User Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetDTO fromModel(GermplasmModel model) {
        GermplasmGetDTO dto = new GermplasmGetDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getRdfType());
        dto.setLabel(model.getLabel());
        dto.setFromSpecies(model.getSpecies());
        dto.setFromVariety(model.getVariety());
        dto.setFromAccession(model.getAccession());

        return dto;
    }

}
