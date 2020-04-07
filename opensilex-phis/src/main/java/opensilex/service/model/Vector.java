//******************************************************************************
//                                Vector.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 6 Apr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Vector model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Vector {
    
    /**
     * URI.
     */
    private String uri;
    
    /**
     * Type.
     */
    private String rdfType;
    
    /**
     * Alias.
     */
    private String label;
    
    /**
     * Brand.
     */
    private String brand;
    
    /**
     * Serial number.
     */
    private String serialNumber;
    
    /**
     * Dismissing date.
     */
    private String inServiceDate;
    
    /**
     * Purchase date.
     */
    private String dateOfPurchase;
    
    /**
     * Email of the person in charge.
     */
    private String personInCharge;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getInServiceDate() {
        return inServiceDate;
    }

    public void setInServiceDate(String inServiceDate) {
        this.inServiceDate = inServiceDate;
    }

    public String getDateOfPurchase() {
        return dateOfPurchase;
    }

    public void setDateOfPurchase(String dateOfPurchase) {
        this.dateOfPurchase = dateOfPurchase;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getPersonInCharge() {
        return personInCharge;
    }

    public void setPersonInCharge(String personInCharge) {
        this.personInCharge = personInCharge;
    }
}
