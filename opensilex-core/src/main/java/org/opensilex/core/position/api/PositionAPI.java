package org.opensilex.core.position.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoQueryException;
import com.mongodb.client.FindIterable;
import io.swagger.annotations.*;
import org.geojson.GeoJsonObject;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.MoveEventNoSqlModel;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.ontology.Oeev;
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geoJsonToGeometry;

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

        MoveModel model = moveDAO.getLastMoveAfter(
                uri,
                time != null ? OffsetDateTime.parse(time) : null
        );

        if (model == null) {
            //if an object has move,it's not an exception. Just no move is associated with this object
            return new SingleObjectResponse<>(new PositionGetDTO()).getResponse();
        }
        else {
            PositionModel position = moveDAO.getPosition(uri, model.getUri());

            if (model.getTo() == null && model.getFrom() == null && position == null) {
                throw new NotFoundURIException("No position found", uri);
            }
            return new SingleObjectResponse<>(new PositionGetDTO(model, position)).getResponse();
        }
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
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") @NotNull URI target,
            @ApiParam(value = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDateTime") @ValidOffsetDateTime String startDate,
            @ApiParam(value = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") @ValidOffsetDateTime String endDate,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);

        MoveModel moveEvent = moveDAO.getLastMoveAfter(target, null);

        if (moveEvent != null) {
            var positionHistory = moveDAO.getPositionsHistory(
                    target,
                    null,
                    startDate != null ? OffsetDateTime.parse(startDate) : null,
                    endDate != null ? OffsetDateTime.parse(endDate) : null,
                    orderByList,
                    page,
                    pageSize
            ).convert(PositionGetDTO.class, move -> {
                try {
                    return new PositionGetDTO(move, move.getNoSqlModel().getTargetPositions().get(0).getPosition());
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            });
            return new PaginatedListResponse<>(positionHistory).getResponse();
        }

        return new PaginatedListResponse<>().getResponse();
    }

    @POST
    @Path("geospatializedPosition")
    @ApiOperation("Search the last geospatialized position of a target for an experiment")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return position list", response = PositionGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchGeospatializedPosition(
            @ApiParam(value = "geometry GeoJSON", required = true) @NotNull GeoJsonObject geometry,
            @ApiParam(value = "target RDF Type URI") @QueryParam("base_type") @ValidURI URI targetType,
            @ApiParam(value = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDateTime") @ValidOffsetDateTime String startDate,
            @ApiParam(value = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") @ValidOffsetDateTime String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) @Max(1000) int pageSize
    ) throws Exception {

        MoveEventDAO moveDAO = new MoveEventDAO(sparql, nosql);
        List<MoveEventNoSqlModel> lastTargetPositionList = new ArrayList<>();
        List<MoveEventNoSqlModel> lastPositionListGeo = new ArrayList<>();

        try {
            //create search filter
            EventSearchFilter searchFilter = new EventSearchFilter();
            searchFilter.setStart(startDate != null ? OffsetDateTime.parse(startDate) : null)
                    .setEnd(endDate != null ? OffsetDateTime.parse(endDate) : OffsetDateTime.now())
                    .setBaseType(targetType)
                    .setType(new URI(Oeev.Move.getURI()))
                    .setLang(this.currentUser.getLanguage())
                    .setPageSize(pageSize);


            // search all moves between the start (and end) date of the experiment for an event type (move) and a target type
            ListWithPagination<EventModel> moveList = moveDAO.search(searchFilter);
            //get last move by unique target uri
             Map<List<URI>,Optional<EventModel>> uniqueTargetLastMoveList = moveList.getList().stream()
                                                                                    //group by unique target URI
                                                                                    .collect(Collectors.groupingBy(EventModel::getTargets,
                                                                                    // get the last move by the property end
                                                                                    Collectors.maxBy(Comparator.comparing(u ->u.getEnd().getDateTimeStamp()))));

            //for each unique target uri, get the mongoDB Model move linked (and the target detail?)
            for (Optional<EventModel> uniqueTargetLastMove : uniqueTargetLastMoveList.values()) {

                MoveEventNoSqlModel lastTargetPosition = moveDAO.getMoveEventNoSqlModel(uniqueTargetLastMove.get().getUri());
                if(lastTargetPosition != null){
                    lastTargetPositionList.add(lastTargetPosition);
                }
            }
            // Get filtered positions with coordinates not null and inside the current extend
            FindIterable<MoveEventNoSqlModel> lastPositionFindIterable = moveDAO.getIntersectPosition(lastTargetPositionList, geoJsonToGeometry(geometry));
            //Convert FindIterable to List
            for (MoveEventNoSqlModel results : lastPositionFindIterable) {
                lastPositionListGeo.add(results);
            }
        }catch (MongoQueryException mongoException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
        }
       return new PaginatedListResponse<>(lastPositionListGeo).getResponse();
    }

    @GET
    @Path("count")
    @ApiOperation("Count moves")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of moves associated to a given target", response = Integer.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countMoves(
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI target) throws Exception {

        MoveEventDAO dao = new MoveEventDAO(sparql, nosql);
        int moveCount = dao.countMoves(target);

        return new SingleObjectResponse<>(moveCount).getResponse();
    }
}
