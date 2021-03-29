package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.position.dal.ConcernedItemPositionModel;
import org.opensilex.core.position.dal.PositionNoSqlModel;
import org.opensilex.server.rest.validation.Required;

import javax.validation.Valid;
import java.net.URI;

@ApiModel
@Valid
public class ConcernedItemPositionCreationDTO {

    @JsonProperty("target")
    private URI concernedItem;

    @JsonProperty("position")
    private PositionCreationDTO position;

    @Required
    @ApiModelProperty(required = true, example = "test:plantA")
    public URI getConcernedItem() {
        return concernedItem;
    }

    public void setConcernedItem(URI concernedItem) {
        this.concernedItem = concernedItem;
    }

    @Required
    public PositionCreationDTO getPosition() {
        return position;
    }

    public void setPosition(PositionCreationDTO position) {
        this.position = position;
    }

    public ConcernedItemPositionModel toModel(){

        ConcernedItemPositionModel model = new ConcernedItemPositionModel();
        model.setConcernedItem(concernedItem);
        model.setPosition(position.toModel());
        return model;
    }

}
