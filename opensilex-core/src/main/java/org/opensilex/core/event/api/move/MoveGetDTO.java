package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.api.EventGetDTO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.organisation.api.facility.FacilityNamedDTO;
import org.opensilex.core.position.api.TargetPositionGetDTO;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.event.dal.move.MoveNosqlModel;

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
    private FacilityNamedDTO from;

    @JsonProperty("to")
    private FacilityNamedDTO to;

    @JsonProperty("targets_positions")
    private List<TargetPositionGetDTO> targetsPositions;

    public MoveGetDTO() {
    }

    public MoveGetDTO(MoveModel model) throws URISyntaxException, JsonProcessingException {

        super.fromModel(model);
        if(model.getFrom() != null){
            from = new FacilityNamedDTO(model.getFrom());
        }
        if(model.getTo() != null){
            to = new FacilityNamedDTO(model.getTo());
        }

        MoveNosqlModel moveEventNoSqlModel = model.getNoSqlModel();
        if(moveEventNoSqlModel == null){
            return;
        }

        List<TargetPositionModel> itemPositions = moveEventNoSqlModel.getTargetPositions();
        if(CollectionUtils.isEmpty(itemPositions)){
            return;
        }

        this.targetsPositions = new ArrayList<>(itemPositions.size());
        for(TargetPositionModel itemPosition : itemPositions){
            this.targetsPositions.add(new TargetPositionGetDTO(itemPosition));
        }

    }

    public FacilityNamedDTO getFrom() {
        return from;
    }

    public void setFrom(FacilityNamedDTO from) {
        this.from = from;
    }

    public FacilityNamedDTO getTo() {
        return to;
    }

    public void setTo(FacilityNamedDTO to) {
        this.to = to;
    }

    public List<TargetPositionGetDTO> getTargetsPositions() {
        return targetsPositions;
    }

    public void setTargetsPositions(List<TargetPositionGetDTO> targetsPositions) {
        this.targetsPositions = targetsPositions;
    }

    @ApiModelProperty(value = "Description of the move",example = "Move to greenhouse A")
    public String getDescription() {
        return description;
    }
}
