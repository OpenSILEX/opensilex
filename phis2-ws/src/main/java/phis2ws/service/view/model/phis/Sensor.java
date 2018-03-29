//******************************************************************************
//                                       Sensor.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 mars 2018
// Subject: Represents sensors view
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represents a sensor view
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Sensor {
    
    //sensor uri
    private String uri;
    //sensor type
    private String rdfType;
    //sensor alias
    private String label;
    //sensor brand
    private String brand;
    //first date of sensor service
    private String inServiceDate;
    //purchase date of the sensor
    private String dateOfPurchase;
    //date of the last calibration of the sensor
    private String dateOfLastCalibration;

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
    
}
