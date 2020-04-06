//******************************************************************************
//                               VariableDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 14 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Method;
import opensilex.service.model.Trait;
import opensilex.service.model.Unit;
import opensilex.service.model.Variable;

/**
 * Variable DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class VariableDTO extends InstanceDefinitionDTO {

    final static Logger LOGGER = LoggerFactory.getLogger(VariableDTO.class);

    private String trait;
    private String method;
    private String unit;

    @Override
    public Variable createObjectFromDTO() {
        Variable variable = (Variable) super.createObjectFromDTO();
        variable.setTrait(new Trait(trait));
        variable.setMethod(new Method(method));
        variable.setUnit(new Unit(unit));

        return variable;
    }

    @URL
    @Required
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/id/traits/t001")
    public String getTrait() {
        return trait;
    }

    public void setTrait(String trait) {
        this.trait = trait;
    }

    @URL
    @Required
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/id/methods/m001")
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @URL
    @Required
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/id/units/u001")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
