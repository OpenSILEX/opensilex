//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.Map;

/**
 * What the user changes in the description of one sheet. A field left out is left as it was; an
 * empty string for a kind or a role puts the default back.
 *
 * @author Arnaud Charleroy
 */
public class SheetDescriptionUpdateDTO {

    @NotEmpty
    private String sheet;
    private String kind;
    private Map<String, String> roles;
    @JsonProperty("rdf_type")
    private URI rdfType;
    private Map<String, String> properties;
    private String factor;

    @ApiModelProperty(value = "The sheet", required = true, example = "essai")
    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    @ApiModelProperty(value = "The kind of the sheet; empty to go back to the default", example = "OBJECTS")
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @ApiModelProperty(value = "The factor a sheet of levels lists the levels of, when no column names it; empty to "
            + "clear it", example = "Traitement")
    public String getFactor() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor = factor;
    }

    @ApiModelProperty(value = "The role of the columns that change, by header; empty to go back to the default")
    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    @ApiModelProperty(value = "The type of the objects or facilities of the sheet")
    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    @ApiModelProperty(value = "The property each column is written to, by header; empty to write none")
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
