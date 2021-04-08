package org.opensilex.core.position.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.*;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;

/**
 * @author Renaud COLIN
 */
@Api(PositionAPI.CREDENTIAL_POSITION_GROUP_ID)
@Path(PositionAPI.PATH)
@ApiCredentialGroup(
        groupId = PositionAPI.CREDENTIAL_POSITION_GROUP_ID,
        groupLabelKey = PositionAPI.CREDENTIAL_POSITION_GROUP_LABEL_KEY
)
public class PositionAPI {

    public static final String PATH = "/core/positions";
    public static final String CREDENTIAL_POSITION_GROUP_ID = "Positions";
    public static final String CREDENTIAL_POSITION_GROUP_LABEL_KEY = "credential-groups.positions";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    UserModel currentUser;

    @GET
    @Path("{uri}")
    @ApiOperation("Get the position of an object")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Position retrieved", response = PositionGetDTO.class),
        @ApiResponse(code = 404, message = "No position found for this object", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPosition(
            @ApiParam(value = "Object URI", example = "http://opensilex.dev/plant/plant5841", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Time : match position at the given time", example = "2019-09-08T12:00:00+01:00") @QueryParam("time") @ValidOffsetDateTime String time
    ) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);

        MoveModel model = moveDAO.getMoveEvent(
                uri,
                time != null ? OffsetDateTime.parse(time) : null
        );

        if (model == null) {
            throw new NotFoundURIException("No position found", uri);
        }

        PositionModel position = moveDAO.getPosition(uri, model.getUri());

        if (position == null) {
            throw new NotFoundURIException("No position found", uri);
        }
        return new SingleObjectResponse<>(new PositionGetDTO(model, position)).getResponse();

    }

    @GET
    @Path("history")
    @ApiOperation("Search history of position of an object")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return position list", response = PositionGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPositionHistory(
            @ApiParam(value = "Concerned item URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("concernedItemUri") @NotNull URI concernedItem,
            @ApiParam(value = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDateTime") @ValidOffsetDateTime String startDate,
            @ApiParam(value = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") @ValidOffsetDateTime String endDate,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);

        MoveModel moveEvent = moveDAO.getMoveEvent(concernedItem, null);

        List<PositionGetDTO> resultDTOList = new ArrayList<>();
        if (moveEvent != null) {
            LinkedHashMap<MoveModel, PositionModel> positionHistory = moveDAO.getPositionsHistory(
                    concernedItem,
                    null,
                    startDate != null ? OffsetDateTime.parse(startDate) : null,
                    endDate != null ? OffsetDateTime.parse(endDate) : null,
                    orderByList,
                    page,
                    pageSize
            );

            positionHistory.forEach((move, position) -> {
                try {
                    resultDTOList.add(new PositionGetDTO(move, position));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
