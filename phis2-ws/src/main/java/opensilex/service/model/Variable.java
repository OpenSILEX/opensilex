//******************************************************************************
//                                       Variable.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 16 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Variable model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Variable extends RdfResourceDefinition {
    
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
