//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.characteristic;

import java.net.URI;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.dal.CharacteristicModel;

/**
 *
 * @author vidalmor
 */
public class CharacteristicCreationDTO extends BaseVariableCreationDTO<CharacteristicModel> {

    @Override
    protected CharacteristicModel newModelInstance() {
        return new CharacteristicModel();
    }

    @Schema(example = "Height", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getName() {
        return name;
    }

    @Schema(example = "Describe the height")
    public String getDescription() {
        return description;
    }

    @Schema(example = "http://opensilex.dev/set/variables/characteristic/Height")
    public URI getUri() {
        return uri;
    }

}
