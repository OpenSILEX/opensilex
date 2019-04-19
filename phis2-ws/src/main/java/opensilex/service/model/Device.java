//******************************************************************************
//                                       Device.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Model of the devices
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Device {
    //device uri
    protected String uri;
    //device type
    protected String rdfType;
    //device alias
    protected String label;
    //device brand
    protected String brand;
    //serial number of the device
    protected String serialNumber;
    //first date of device service
    protected String inServiceDate;
    //purchase date of the device
    protected String dateOfPurchase;
    //date of the last calibration of the device
    protected String dateOfLastCalibration;
    //email of the person in charge of the device
    protected String personInCharge;
    
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

    public String getDateOfLastCalibration() {
        return dateOfLastCalibration;
    }

    public void setDateOfLastCalibration(String dateOfLastCalibration) {
        this.dateOfLastCalibration = dateOfLastCalibration;
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
