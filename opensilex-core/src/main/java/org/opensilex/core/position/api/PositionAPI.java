package org.opensilex.core.position.api;

import com.mongodb.MongoQueryException;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.apache.commons.collections.CollectionUtils;
import org.geojson.GeoJsonObject;
import org.opensilex.core.event.api.move.MoveGetDTO;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geoJsonToGeometry;

/**
 * @author Renaud COLIN
 */
@Tag(name = PositionAPI.CREDENTIAL_POSITION_GROUP_ID)
@Path(PositionAPI.PATH)
@ApiCredentialGroup(
        groupId = PositionAPI.CREDENTIAL_POSITION_GROUP_ID,
        groupLabelKey = PositionAPI.CREDENTIAL_POSITION_GROUP_LABEL_KEY
)
public class PositionAPI {

    public static final String PATH = "/core/positions";
    public static final String CREDENTIAL_POSITION_GROUP_ID = "Positions";
    public static final String CREDENTIAL_POSITION_GROUP_LABEL_KEY = "credential-groups.positions";
    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel currentUser;

    @GET
    @Path("{uri}")
    @Operation(summary = "Get the position of an object")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Position retrieved", content = @Content(schema = @Schema(implementation = PositionGetDTO.class))),
        @ApiResponse(responseCode = "404", description = "No position found for this object", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPosition(
            @Parameter(description = "Object URI", example = "http://opensilex.dev/plant/plant5841", required = true) @PathParam("uri") @NotNull URI uri,
            @Parameter(description = "Time : match position at the given time", example = "2019-09-08T12:00:00+01:00") @QueryParam("time") @ValidOffsetDateTime String time
    ) throws Exception {
        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);

        MoveModel moveModel = moveLogic.getLastMoveAfter(uri, time != null ? OffsetDateTime.parse(time) : null);

        if (moveModel == null) {
            //if an object has no move,it's not an exception. Just no move is associated with this object
            return new SingleObjectResponse<>(new PositionGetDTO()).getResponse();
        }
        else {
            LocationObservationModel location = moveLogic.getPosition(moveModel);

            if (location == null) {
                throw new NotFoundURIException("No position found", uri);
            }
            return new SingleObjectResponse<>(new PositionGetDTO(moveModel, location)).getResponse();
        }
    }

    @GET
    @Path("history")
    @Operation(summary = "Search history of position of an object")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return position list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MoveGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPositionHistory(
            @Parameter(description = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") @NotNull URI target,
            @Parameter(description = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDateTime") @ValidOffsetDateTime String startDate,
            @Parameter(description = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") @ValidOffsetDateTime String endDate,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number") @QueryParam("page") int page,
            @Parameter(description = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);

        try {
            var positionHistory = moveLogic.getPositionsHistory(
                    target,
                    null,
                    startDate != null ? OffsetDateTime.parse(startDate) : null,
                    endDate != null ? OffsetDateTime.parse(endDate) : null,
                    orderByList,
                    page,
                    pageSize
            ).convert(MoveGetDTO.class, MoveGetDTO::new);
            return new PaginatedListResponse<>(positionHistory).getResponse();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("geospatializedPosition")
    @Operation(summary = "Search the last geospatialized position of a target for an experiment")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return position list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PositionGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchGeospatializedPosition(
            @Parameter(description = "geometry GeoJSON", required = true) @NotNull GeoJsonObject geometry,
            @Parameter(description = "target RDF Type URI") @QueryParam("base_type") @ValidURI URI targetType,
            @Parameter(description = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDateTime") @ValidOffsetDateTime String startDate,
            @Parameter(description = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") @ValidOffsetDateTime String endDate,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @Min(0) @Max(1000) int pageSize
    ) throws Exception {
        MoveLogic moveLogic = new MoveLogic(sparql, nosql, currentUser);
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2(), sparql);

        try {
            //create search filter
            MoveSearchFilter searchFilter = new MoveSearchFilter();
            searchFilter.setStart(startDate != null ? OffsetDateTime.parse(startDate) : null)
                    .setEnd(endDate != null ? OffsetDateTime.parse(endDate) : OffsetDateTime.now())
                    .setBaseType(targetType)
                    .setType(new URI(Oeev.Move.getURI()))
                    .setLang(this.currentUser.getLanguage())
                    .setPageSize(pageSize);

            // search all moves between the start (and end) date of the experiment for an event type (move) and a target type
            ListWithPagination<MoveModel> moveList = moveLogic.search(searchFilter);

            //Leave if no moves were found to prevent unexpected errors
            if(CollectionUtils.isEmpty(moveList.getList())){
                return new PaginatedListResponse<>(Collections.emptyList()).getResponse();
            }
            //get last move by unique target uri
             Map<List<URI>,Optional<MoveModel>> uniqueTargetLastMoveList = moveList.getList().stream()
                                                                                    //group by unique target URI
                                                                                    .collect(Collectors.groupingBy(EventModel::getTargets,
                                                                                    // get the last move by the property end
                                                                                    Collectors.maxBy(Comparator.comparing(u ->u.getEnd().getDateTimeStamp()))));

            //for each unique target uri, get the mongoDB Model location linked (inside the current extend)
             StringUriMap<LocationObservationModel> targetLocationMap = new StringUriMap<>(locationObservationLogic.getLocationObservationsWithGeospatializedPositionPerTargetFromTargetUris(
                    uniqueTargetLastMoveList.keySet().stream().flatMap(Collection::stream).collect(Collectors.toList()),
                    endDate != null ? Instant.parse(endDate) : null,
                    geoJsonToGeometry(geometry)));

            List<PositionGetDTO> positionList = new ArrayList<>();

            uniqueTargetLastMoveList.forEach((targetList, move) -> {
                    if(Objects.nonNull(targetLocationMap.get(targetList.get(0)))){
                        PositionGetDTO positionGetDTO = new PositionGetDTO(move.get(), targetLocationMap.get(targetList.get(0)));
                        positionList.add(positionGetDTO);
                    }
            });

            return new PaginatedListResponse<>(positionList).getResponse();
        }catch (MongoQueryException mongoException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
        }
    }

    @GET
    @Path("count")
    @Operation(summary = "Count moves")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the number of moves associated to a given target", content = @Content(schema = @Schema(implementation = Integer.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countMoves(
            @Parameter(description = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI target) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);
        int moveCount = dao.countForTarget(target);

        return new SingleObjectResponse<>(moveCount).getResponse();
    }
}
