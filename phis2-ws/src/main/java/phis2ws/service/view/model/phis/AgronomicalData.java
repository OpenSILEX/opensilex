//**********************************************************************************************
//                                       AgronomicalData.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 14 2017
// Subject: Represents the data view 
//***********************************************************************************************
package phis2ws.service.view.model.phis;

public class AgronomicalData {
    private String agronomicalObject;
    private String date;
    private String value;
    private String variable; 
    private String sensor;
    private String incertitude;
    
    public AgronomicalData() {
        
    }
    
    public AgronomicalData(AgronomicalData data) {
        agronomicalObject = data.getAgronomicalObject();
        date = data.getDate();
        value = data.getValue();
    }

    public String getAgronomicalObject() {
        return agronomicalObject;
    }

    public void setAgronomicalObject(String agronomicalObject) {
        this.agronomicalObject = agronomicalObject;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public String getIncertitude() {
        return incertitude;
    }

    public void setIncertitude(String incertitude) {
        this.incertitude = incertitude;
    }
}
