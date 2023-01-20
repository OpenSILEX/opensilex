package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Bernhard Gschloessl
 */
public class GrowthFacilityDTO {
    private String PUI; //v2.1 : MIAPPE V1.1 (DM-27) Type of growth facility
    private String description; //v2.1 : MIAPPE V1.1 (DM-26) Description of growth facility

    public String getPUI() {
        return PUI;
    }

    public void setPUI(String PUI) {
        this.PUI = PUI;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
