//******************************************************************************
//                                       Actuator.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 17 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.HashMap;

/**
 * Model of an actuator.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Actuator extends Device {
    //variables mesured by the actuator
    protected HashMap<String, String>  variables;
    
    public HashMap<String, String>  getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, String>  variables) {
        this.variables = variables;
    }
}
