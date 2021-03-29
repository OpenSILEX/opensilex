package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.api.EventGetDTO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityNamedDto;
import org.opensilex.core.position.api.ConcernedItemPositionGetDTO;
import org.opensilex.core.position.dal.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
        "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant","description","targets","author","from","to","targets_positions"
})
public class MoveGetDTO extends EventGetDTO {

    @JsonProperty("from")
    private InfrastructureFacilityNamedDto from;

    @JsonProperty("to")
    private InfrastructureFacilityNamedDto to;

    @JsonProperty("targets_positions")
    private List<ConcernedItemPositionGetDTO> concernedItemPositions;

    public MoveGetDTO() {
    }

    public MoveGetDTO(MoveModel model) throws URISyntaxException, JsonProcessingException {

        super.fromModel(model);
        if(model.getFrom() != null){
            from = new InfrastructureFacilityNamedDto(model.getFrom());
        }
        if(model.getTo() != null){
            to = new InfrastructureFacilityNamedDto(model.getTo());
        }

        MoveEventNoSqlModel moveEventNoSqlModel = model.getNoSqlModel();
        if(moveEventNoSqlModel == null){
            return;
        }

        List<ConcernedItemPositionModel> itemPositions = moveEventNoSqlModel.getItemPositions();
        if(CollectionUtils.isEmpty(itemPositions)){
            return;
        }

        this.concernedItemPositions = new ArrayList<>(itemPositions.size());
        for(ConcernedItemPositionModel itemPosition : itemPositions){
            this.concernedItemPositions.add(new ConcernedItemPositionGetDTO(itemPosition));
        }

    }

    public InfrastructureFacilityNamedDto getFrom() {
        return from;
    }

    public void setFrom(InfrastructureFacilityNamedDto from) {
        this.from = from;
    }

    public InfrastructureFacilityNamedDto getTo() {
        return to;
    }

    public void setTo(InfrastructureFacilityNamedDto to) {
        this.to = to;
    }

    public List<ConcernedItemPositionGetDTO> getConcernedItemPositions() {
        return concernedItemPositions;
    }

    public void setConcernedItemPositions(List<ConcernedItemPositionGetDTO> concernedItemPositions) {
        this.concernedItemPositions = concernedItemPositions;
    }

    @ApiModelProperty(value = "Description of the move",example = "Move to greenhouse A")
    public String getDescription() {
        return description;
    }
}
