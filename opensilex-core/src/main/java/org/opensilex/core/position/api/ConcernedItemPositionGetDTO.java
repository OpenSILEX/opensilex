package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.event.dal.move.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.PositionModel;

import java.net.URI;
import java.net.URISyntaxException;

public class ConcernedItemPositionGetDTO {

    @JsonProperty("target")
    private URI concernedItem;

    @JsonProperty("position")
    private PositionGetDetailDTO position;

    public ConcernedItemPositionGetDTO(ConcernedItemPositionModel model) throws URISyntaxException, JsonProcessingException {

        this.concernedItem = model.getConcernedItem();

        PositionModel position = model.getPosition();
        if(position != null){
            this.position = new PositionGetDetailDTO(position);
        }
    }

    public ConcernedItemPositionGetDTO(){

    }

    @ApiModelProperty(example="test:plantA")
    public URI getConcernedItem() {
        return concernedItem;
    }

    public void setConcernedItem(URI concernedItem) {
        this.concernedItem = concernedItem;
    }

    public PositionGetDetailDTO getPosition() {
        return position;
    }

    public void setPosition(PositionGetDetailDTO position) {
        this.position = position;
    }
}
