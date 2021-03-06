//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.method;

import java.net.URI;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.MethodModel;

/**
 *
 * @author vidalmor
 */
public class MethodCreationDTO  extends BaseVariableCreationDTO<MethodModel> {

    @Override
    protected MethodModel newModelInstance() {
        return new MethodModel();
    }

    @ApiModelProperty(example = "ImageAnalysis", required = true)
    public String getName() {
        return name;
    }

    @ApiModelProperty(example = "Based on a software")
    public String getDescription() {
        return description;
    }


    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/ImageAnalysis")
    public URI getUri() {
        return uri;
    }

}
