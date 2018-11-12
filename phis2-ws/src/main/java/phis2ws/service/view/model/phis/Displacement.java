//******************************************************************************
//                                       Displacement.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * This is the model for the displacements in time and space 
 * of the different objects participating in experiments. 
 * For instance, plants are often transferred, during a single experiment, 
 * between installations or compartments with different environmental conditions. 
 * Similarly, sensors can be replaced or moved to different positions in the 
 * field or greenhouse.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public abstract class Displacement extends Event{

    public Displacement(String uri, String date) {
        super(uri, date);
    }
}
