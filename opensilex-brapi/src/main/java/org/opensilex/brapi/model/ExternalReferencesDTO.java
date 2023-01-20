package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Bernhard Gschloessl
 */
public class ExternalReferencesDTO {
    private String referenceId; //v2.1 : The external reference ID. Could be a simple string or a URI.
    private String referenceSource; //v2.1 : An identifier for the source system or database of this reference

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceSource() {
        return referenceSource;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }
}
