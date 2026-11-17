//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * A value of a property, as the user typed it: a literal, or the name or URI of what a link points to.
 *
 * @author Arnaud Charleroy
 */
public class PropertyValueDTO {

    @NotNull
    @JsonProperty("property")
    @ApiModelProperty(value = "The property", required = true)
    private URI property;

    @NotNull
    @JsonProperty("value")
    @ApiModelProperty(value = "The value, or the name or URI of what it points to", required = true)
    private String value;

    public PropertyValueDTO() {
    }

    public PropertyValueDTO(URI property, String value) {
        this.property = property;
        this.value = value;
    }

    public URI getProperty() {
        return property;
    }

    public void setProperty(URI property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
