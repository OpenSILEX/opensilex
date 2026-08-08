//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.method;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.dal.MethodModel;

import java.net.URI;


/**
 *
 * @author vidalmor
 */
public class MethodDetailsDTO extends BaseVariableDetailsDTO<MethodModel> {

    public MethodDetailsDTO(MethodModel model) {
        super(model);
    }

    public MethodDetailsDTO() {
    }

    @Override
    @Schema(example = "http://opensilex.dev/set/variables/method/ImageAnalysis")
    public URI getUri() {
        return uri;
    }

    @Override
    @Schema(example = "ImageAnalysis")
    public String getName() {
        return name;
    }

    @Override
    @Schema(example = "Based on a software")
    public String getDescription() {
        return description;
    }

    @Override
    public MethodModel toModel() {
        MethodModel model = new MethodModel();
        setBasePropertiesToModel(model);
        return model;
    }

}
