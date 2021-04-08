package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.core.event.api.validation.MoveLocationOrPositionNotNullConstraint;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.position.api.ConcernedItemPositionCreationDTO;
import org.opensilex.core.event.dal.move.ConcernedItemPositionModel;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@JsonPropertyOrder({
    "uri", "rdf_type","start", "end", "is_instant","description","targets","relations","from","to","targets_positions"
})
@MoveLocationOrPositionNotNullConstraint
public class MoveCreationDTO extends EventCreationDTO {

    @JsonProperty("from")
    private URI from;

    @JsonProperty("to")
    private URI to;

    @JsonProperty("targets_positions")
    private List<ConcernedItemPositionCreationDTO> concernedItemPositions;

    @ApiModelProperty(example="test:greenHouseA")
    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    @ApiModelProperty(example="test:greenHouseB")
    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }


    public List<ConcernedItemPositionCreationDTO> getConcernedItemPositions() {
        return concernedItemPositions;
    }

    public void setConcernedItemPositions(List<ConcernedItemPositionCreationDTO> concernedItemPositions) {
        this.concernedItemPositions = concernedItemPositions;
    }

    public MoveModel toModel() {

        MoveModel model = toModel(new MoveModel());
        if(from != null && ! from.toString().isEmpty()){
            InfrastructureFacilityModel fromModel = new InfrastructureFacilityModel();
            fromModel.setUri(from);
            model.setFrom(fromModel);
        }
        if(to != null && ! to.toString().isEmpty()){
            InfrastructureFacilityModel toModel = new InfrastructureFacilityModel();
            toModel.setUri(to);
            model.setTo(toModel);
        }

        MoveEventNoSqlModel noSqlModel = toNoSqlModel();
        if(noSqlModel != null){
            model.setNoSqlModel(noSqlModel);
        }
        return model;
    }

    public MoveEventNoSqlModel toNoSqlModel() {

        if (CollectionUtils.isEmpty(concernedItemPositions)) {
            return null;
        }

        List<ConcernedItemPositionModel> itemPositions = concernedItemPositions.stream()
                .map(ConcernedItemPositionCreationDTO::toModel)
                .collect(Collectors.toList());

        MoveEventNoSqlModel moveNoSql = new MoveEventNoSqlModel();
        moveNoSql.setItemPositions(itemPositions);
        return moveNoSql;
    }
}
