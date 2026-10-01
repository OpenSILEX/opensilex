//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.objects.TypeProperties.TypeProperty;

import java.net.URI;

/**
 * A property a column can be mapped to: one the platform's importer accepts for the type.
 *
 * @author Arnaud Charleroy
 */
public class TypePropertyDTO {

    private URI uri;
    private String name;

    @JsonProperty("is_object")
    private boolean object;

    private URI range;

    @JsonProperty("is_required")
    private boolean required;

    @JsonProperty("is_list")
    private boolean list;

    public static TypePropertyDTO fromModel(TypeProperty model) {
        TypePropertyDTO dto = new TypePropertyDTO();
        dto.uri = model.uri();
        dto.name = model.name();
        dto.object = model.object();
        dto.range = model.range();
        dto.required = model.required();
        dto.list = model.list();
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "Whether its value is a resource, named in the file and found by its name")
    public boolean isObject() {
        return object;
    }

    @ApiModelProperty(value = "The class of the resources it points to, or the datatype of its values")
    public URI getRange() {
        return range;
    }

    public boolean isRequired() {
        return required;
    }

    @ApiModelProperty(value = "Whether it holds several values: several columns may feed it")
    public boolean isList() {
        return list;
    }
}
