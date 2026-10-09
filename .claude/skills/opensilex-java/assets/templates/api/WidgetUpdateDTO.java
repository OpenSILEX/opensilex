package org.opensilex.core.widget.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Body of the PUT request: the URI identifies the widget to update, so it becomes mandatory.
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "start_date"})
public class WidgetUpdateDTO extends WidgetCreationDTO {

    @Override
    @ApiModelProperty(value = "Widget URI", example = "http://opensilex.dev/widgets#my-widget", required = true)
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}
