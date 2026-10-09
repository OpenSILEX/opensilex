package org.opensilex.core.widget.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;

/**
 * Body of the POST (and base of the PUT) request: same fields as {@link WidgetDTO}, with the mandatory ones declared.
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "start_date"})
public class WidgetCreationDTO extends WidgetDTO {

    @Override
    @ApiModelProperty(value = "Widget name", example = "My widget", required = true)
    @Required
    public String getName() {
        return name;
    }
}
