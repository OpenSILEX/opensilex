//**********************************************************************************************
//                                       Variable.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 16 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 16 2017
// Subject: Represents the instance of definition view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

public class Variable extends InstanceDefinition {
    
    /**
     * @attribute trait le trait 
     * @attribute method la méthode 
     * @attribute unit l'unité 
     */
    protected Trait trait;
    protected Method method;
    protected Unit unit;

    public Variable() {
    }
    
    public Variable(String uri) {
        super(uri);
    }

    public Trait getTrait() {
        return trait;
    }

    public void setTrait(Trait trait) {
        this.trait = trait;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}
