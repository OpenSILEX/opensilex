//******************************************************************************
//                                       ObservationVariable.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 23 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 *
 * @author boizetal
 */
public class ObservationVariable {
    
    private String observationVariableDbId;
    private String name;
    private String ontologyDbId;
    private String ontologyName;
    private Trait trait;
    private Method method;
    private Unit Scale;
    private String DefaultValue;
}
