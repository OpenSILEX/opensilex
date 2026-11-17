//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.PreviewColumn;

import java.net.URI;

/**
 * What a column of a sheet gives the graph.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewColumnDTO {

    @JsonProperty("index")
    @ApiModelProperty(value = "Its position, from 0", example = "0")
    private int index;

    @JsonProperty("header")
    @ApiModelProperty(value = "Its header as written")
    private String header;

    @JsonProperty("role")
    @ApiModelProperty(value = "IDENTIFIER, TYPE, VALUE (literals), LINK, or LEFT_OUT when it gives nothing", example = "VALUE")
    private String role;

    @JsonProperty("property")
    @ApiModelProperty(value = "The property it holds; absent when it holds none")
    private URI property;

    @JsonProperty("property_label")
    @ApiModelProperty(value = "The property's label in the user's language")
    private String propertyLabel;

    static PreviewColumnDTO fromModel(PreviewColumn model) {
        PreviewColumnDTO dto = new PreviewColumnDTO();
        dto.index = model.index();
        dto.header = model.header();
        dto.role = model.role().name();
        dto.property = model.property();
        dto.propertyLabel = model.propertyLabel();
        return dto;
    }

    public int getIndex() {
        return index;
    }

    public String getHeader() {
        return header;
    }

    public String getRole() {
        return role;
    }

    public URI getProperty() {
        return property;
    }

    public String getPropertyLabel() {
        return propertyLabel;
    }
}
