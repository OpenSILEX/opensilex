//******************************************************************************
//                              Sensor.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 Mar. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.HashMap;

/**
 * Sensor model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Sensor extends Device {
    /**
     * Variables measured by the sensor.
     */
    private HashMap<String, String>  variables;
    
    public HashMap<String, String>  getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, String>  variables) {
        this.variables = variables;
    }
}
