package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.position.dal.ConcernedItemPositionModel;
import org.opensilex.core.position.dal.PositionNoSqlModel;

import java.net.URI;
import java.net.URISyntaxException;

public class ConcernedItemPositionGetDTO {

    @JsonProperty("target")
    private URI concernedItem;

    @JsonProperty("position")
    private PositionNoSqlGetDto position;

    public ConcernedItemPositionGetDTO(ConcernedItemPositionModel model) throws URISyntaxException, JsonProcessingException {

        this.concernedItem = model.getConcernedItem();

        PositionNoSqlModel position = model.getPosition();
        if(position != null){
            this.position = new PositionNoSqlGetDto(position);
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

    public PositionNoSqlGetDto getPosition() {
        return position;
    }

    public void setPosition(PositionNoSqlGetDto position) {
        this.position = position;
    }
}
