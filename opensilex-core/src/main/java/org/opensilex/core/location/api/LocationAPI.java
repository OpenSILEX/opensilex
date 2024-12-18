/*
 * *****************************************************************************
 *                         LocationAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 09/10/2024 15:53
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.api;

import io.swagger.annotations.*;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api(LocationAPI.CREDENTIAL_LOCATION_GROUP_ID)
@Path(LocationAPI.PATH)
@ApiCredentialGroup(
        groupId = LocationAPI.CREDENTIAL_LOCATION_GROUP_ID,
        groupLabelKey = LocationAPI.CREDENTIAL_LOCATION_GROUP_LABEL_KEY
)
public class LocationAPI {

    public static final String PATH = "/core/locations";
    public static final String CREDENTIAL_LOCATION_GROUP_ID = "Locations";
    public static final String CREDENTIAL_LOCATION_GROUP_LABEL_KEY = "credential-groups.locations";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBServiceV2 nosql;

    @CurrentUser
    AccountModel currentUser;

    @GET
    @Path("history")
    @ApiOperation("Search location history of an object")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return location list", response = LocationObservationDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchLocationHistory(
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") @NotNull URI featureOfInterest,
            @ApiParam(value = "Start date : match position affected after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("startDate") @ValidOffsetDateTime String startDate,
            @ApiParam(value = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDate") @ValidOffsetDateTime String endDate,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) {
        LocationObservationCollectionLogic observationCollectionLogic = new LocationObservationCollectionLogic(sparql);
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql);
        List<LocationObservationDTO> results = new ArrayList<>();

        URI collectionURI = observationCollectionLogic.getLocationObservationCollectionURI(featureOfInterest);

        if (Objects.nonNull(collectionURI)) {
            ListWithPagination<LocationObservationModel> locationHistory = locationObservationLogic.getLocationsHistory(
                    collectionURI,
                    startDate != null ? Instant.parse(startDate) : null,
                    endDate != null ? Instant.parse(endDate) : null,
                    orderByList,
                    page,
                    pageSize
            );

            results = locationHistory.getList().stream()
                    .map(LocationObservationDTO::getDTOFromModel)
                    .collect(Collectors.toList());
        }
        return new PaginatedListResponse<>(results).getResponse();
    }

    @GET
    @Path("count")
    @ApiOperation("Count locations")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of locations associated to a given target", response = Integer.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countLocations(
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI featureOfInterest) {
        LocationObservationCollectionLogic observationCollectionLogic = new LocationObservationCollectionLogic(sparql);
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql);

        URI collectionURI = observationCollectionLogic.getLocationObservationCollectionURI(featureOfInterest);
        int locationsCount = locationObservationLogic.countLocationsForURI(collectionURI);

        return new SingleObjectResponse<>(locationsCount).getResponse();
    }
}
