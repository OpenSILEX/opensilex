package org.opensilex.core.widget.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.widget.dal.WidgetModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.time.LocalDate;

/**
 * Fields shared by the read, creation and update views. The conversion to and from the model lives in the DTO, never
 * in the DAO. Every field added to {@link WidgetModel} must be added to {@link #toModel} and {@link #fromModel}:
 * otherwise an update silently erases it.
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "description", "start_date"})
public class WidgetDTO extends NamedResourceDTO<WidgetModel> {

    protected String description;

    @JsonProperty("start_date")
    protected LocalDate startDate;

    @Override
    @ApiModelProperty(value = "Widget URI", example = "http://opensilex.dev/widgets#my-widget")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(value = "Widget name", example = "My widget")
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "Widget description", example = "A widget used for demonstration")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Widget start date", example = "2026-01-31")
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @Override
    public WidgetModel newModelInstance() {
        return new WidgetModel();
    }

    @Override
    public void toModel(WidgetModel model) {
        super.toModel(model);
        model.setDescription(getDescription());
        model.setStartDate(getStartDate());
    }

    @Override
    public void fromModel(WidgetModel model) {
        super.fromModel(model);
        setDescription(model.getDescription());
        setStartDate(model.getStartDate());
    }

    public static WidgetDTO getDTOFromModel(WidgetModel model) {
        WidgetDTO dto = new WidgetDTO();
        dto.fromModel(model);
        return dto;
    }
}
