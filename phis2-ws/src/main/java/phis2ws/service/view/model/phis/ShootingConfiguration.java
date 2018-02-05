//**********************************************************************************************
//                                       ShootingConfiguration.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: Represents the shooting configuration view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

/**
 * the shooting configuration view
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ShootingConfiguration {
    
    //date of the image shooting
    private String date;
    //timestamp of the date attribute
    private String timestamp;
    //position of the sensor
    private String position;

    public ShootingConfiguration() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
