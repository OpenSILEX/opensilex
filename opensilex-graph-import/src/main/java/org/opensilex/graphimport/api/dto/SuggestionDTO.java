//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.graph.Suggestion;

import java.net.URI;

/**
 * A close name, offered as "did you mean …?".
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestionDTO {

    @JsonProperty("uri")
    @ApiModelProperty(value = "The existing instance; absent for a row of the file")
    private URI uri;

    @JsonProperty("in_file")
    @ApiModelProperty(value = "True for a row of the file, false for an existing instance")
    private boolean inFile;

    @JsonProperty("name")
    @ApiModelProperty(value = "The name it answers to")
    private String name;

    @JsonProperty("similarity")
    @ApiModelProperty(value = "How close the names are, from 0 to 1", example = "0.86")
    private double similarity;

    static SuggestionDTO fromModel(Suggestion model) {
        SuggestionDTO dto = new SuggestionDTO();
        dto.uri = model.uri();
        dto.inFile = model.nodeKey() != null;
        dto.name = model.name();
        dto.similarity = model.similarity();
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public boolean isInFile() {
        return inFile;
    }

    public String getName() {
        return name;
    }

    public double getSimilarity() {
        return similarity;
    }
}
