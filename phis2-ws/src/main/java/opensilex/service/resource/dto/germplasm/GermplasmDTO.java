//******************************************************************************
//                                GermplasmDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 23 oct. 2019
// Contact: alice.boizet@inra.fr., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

/**
 * GermplasmDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmDTO {
    private String rdfType;
    private String uri;
    private String label;
    private String genus;
    private String species;
    private String variety;
    private String accession;
    private String plantMaterialLot;  


    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
