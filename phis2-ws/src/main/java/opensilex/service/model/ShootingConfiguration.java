//******************************************************************************
//                          ShootingConfiguration.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Shooting configuration model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ShootingConfiguration {
    
    /**
     * Date of the image shooting.
     */
    private String date;
    
    /**
     * Timestamp of the date attribute.
     */
    private String timestamp;
    
    /**
     * Position of the sensor.
     */
    private String position;
    
    /**
     * Sensor URI.
     */
    private String sensor;

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

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }
}
