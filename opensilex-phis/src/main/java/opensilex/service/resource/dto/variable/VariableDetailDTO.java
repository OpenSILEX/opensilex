//******************************************************************************
//                                VariableDetailDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 3 juin 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.variable;

import java.util.ArrayList;
import opensilex.service.model.OntologyReference;
import opensilex.service.model.Variable;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VariableDetailDTO extends RdfResourceDefinitionDTO {
    protected TraitDTO trait;
    protected MethodDTO method;
    protected UnitDTO unit;
    protected ArrayList<OntologyReference> ontologiesReferences;
    
    public VariableDetailDTO(Variable variable) {
        super(variable);
        
        trait = new TraitDTO(variable.getTrait());
        method = new MethodDTO(variable.getMethod());
        unit = new UnitDTO(variable.getUnit());
        ontologiesReferences = variable.getOntologiesReferences();
    }

    public TraitDTO getTrait() {
        return trait;
    }

    public void setTrait(TraitDTO trait) {
        this.trait = trait;
    }

    public MethodDTO getMethod() {
        return method;
    }

    public void setMethod(MethodDTO method) {
        this.method = method;
    }

    public UnitDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitDTO unit) {
        this.unit = unit;
    }
}
