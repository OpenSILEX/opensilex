//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.binding.ImportDecisions;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A resource the user describes in the assistant because no file does, such as the factor a sheet of
 * levels belongs to. Rows of the files may then point to it by its name.
 *
 * @author Arnaud Charleroy
 */
public class DeclaredResourceDTO {

    @JsonProperty("id")
    @ApiModelProperty(value = "Its identifier in the import; given by the assistant when absent")
    private String id;

    @NotNull
    @JsonProperty("class_uri")
    @ApiModelProperty(value = "Its class", required = true)
    private URI classUri;

    @Valid
    @NotNull
    @JsonProperty("values")
    @ApiModelProperty(value = "Its values; a property that takes several values comes several times",
            required = true)
    private List<PropertyValueDTO> values = new ArrayList<>();

    public static DeclaredResourceDTO fromModel(ImportDecisions.DeclaredResource resource) {
        DeclaredResourceDTO dto = new DeclaredResourceDTO();
        dto.id = resource.id();
        dto.classUri = resource.classUri();
        resource.values().forEach((property, list) ->
                list.forEach(value -> dto.values.add(new PropertyValueDTO(property, value))));
        return dto;
    }

    /** The resource, under the identifier given here or the one the caller chose for a new one. */
    public ImportDecisions.DeclaredResource toModel(String newId) {
        Map<URI, List<String>> byProperty = new LinkedHashMap<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && value.getProperty() != null)
                    .forEach(value -> byProperty.computeIfAbsent(value.getProperty(), key -> new ArrayList<>())
                            .add(value.getValue()));
        }
        return new ImportDecisions.DeclaredResource(id == null || id.isBlank() ? newId : id, classUri, byProperty);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getClassUri() {
        return classUri;
    }

    public void setClassUri(URI classUri) {
        this.classUri = classUri;
    }

    public List<PropertyValueDTO> getValues() {
        return values;
    }

    public void setValues(List<PropertyValueDTO> values) {
        this.values = values;
    }
}
