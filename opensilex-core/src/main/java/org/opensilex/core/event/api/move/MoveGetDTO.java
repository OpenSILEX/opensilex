package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.api.EventGetDTO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityNamedDTO;
import org.opensilex.core.position.api.ConcernedItemPositionGetDTO;
import org.opensilex.core.event.dal.move.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant","description","targets","author","from","to","targets_positions"
})
public class MoveEventGetDTO extends EventGetDTO {

    @JsonProperty("from")
    private InfrastructureFacilityNamedDTO from;

    @JsonProperty("to")
    private InfrastructureFacilityNamedDTO to;

    @JsonProperty("targets_positions")
    private List<ConcernedItemPositionGetDTO> concernedItemPositions;

    public MoveEventGetDTO() {
    }

    public MoveEventGetDTO(MoveModel model) throws URISyntaxException, JsonProcessingException {

        super.fromModel(model);
        if(model.getFrom() != null){
            from = new InfrastructureFacilityNamedDTO(model.getFrom());
        }
        if(model.getTo() != null){
            to = new InfrastructureFacilityNamedDTO(model.getTo());
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

    public InfrastructureFacilityNamedDTO getFrom() {
        return from;
    }

    public void setFrom(InfrastructureFacilityNamedDTO from) {
        this.from = from;
    }

    public InfrastructureFacilityNamedDTO getTo() {
        return to;
    }

    public void setTo(InfrastructureFacilityNamedDTO to) {
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
