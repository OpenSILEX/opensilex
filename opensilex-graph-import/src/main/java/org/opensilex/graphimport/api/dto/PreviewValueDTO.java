//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.ValueComparison;

import java.net.URI;
import java.util.List;

/**
 * A value of the file, compared with the resource it goes to, with the cell it comes from.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewValueDTO {

    @JsonProperty("property")
    @ApiModelProperty(value = "The property")
    private URI property;

    @JsonProperty("property_label")
    @ApiModelProperty(value = "The property's label in the user's language")
    private String propertyLabel;

    @JsonProperty("cell")
    @ApiModelProperty(value = "Where it comes from")
    private CellRefDTO cell;

    @JsonProperty("raw")
    @ApiModelProperty(value = "The cell as written")
    private String raw;

    @JsonProperty("value")
    @ApiModelProperty(value = "The literal once read, as it would be stored; absent for a link")
    private String value;

    @JsonProperty("language")
    @ApiModelProperty(value = "The language of the literal")
    private String language;

    @JsonProperty("target")
    @ApiModelProperty(value = "For a link, the resource it points to; absent for a row that would be created")
    private URI target;

    @JsonProperty("state")
    @ApiModelProperty(value = "NEW, PRESENT, CONFLICT, AMBIGUOUS or UNUSABLE", example = "NEW")
    private String state;

    @JsonProperty("held")
    @ApiModelProperty(value = "What the resource holds instead, for a value in conflict")
    private List<String> held;

    static PreviewValueDTO fromModel(ValueComparison model, String propertyLabel) {
        PreviewValueDTO dto = new PreviewValueDTO();
        dto.property = model.property();
        dto.propertyLabel = propertyLabel;
        dto.cell = CellRefDTO.fromModel(model.value().cell());
        dto.raw = model.value().raw();
        if (!model.value().isLink()) {
            dto.value = model.value().literal().lexical();
            dto.language = model.value().language();
        }
        dto.target = model.target();
        dto.state = model.state().name();
        dto.held = model.held();
        return dto;
    }

    public URI getProperty() {
        return property;
    }

    public String getPropertyLabel() {
        return propertyLabel;
    }

    public CellRefDTO getCell() {
        return cell;
    }

    public String getRaw() {
        return raw;
    }

    public String getValue() {
        return value;
    }

    public String getLanguage() {
        return language;
    }

    public URI getTarget() {
        return target;
    }

    public String getState() {
        return state;
    }

    public List<String> getHeld() {
        return held;
    }
}
