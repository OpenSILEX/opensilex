//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.profile.ImportProfile;

/**
 * A known family of import files.
 *
 * @author Arnaud Charleroy
 */
public class ImportProfileDTO {

    private String id;
    private String label;

    public static ImportProfileDTO fromModel(ImportProfile model) {
        ImportProfileDTO dto = new ImportProfileDTO();
        dto.id = model.getId();
        dto.label = model.getLabel();
        return dto;
    }

    @ApiModelProperty(example = "vitis-explorer")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "VitisExplorer grapevine observations")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
