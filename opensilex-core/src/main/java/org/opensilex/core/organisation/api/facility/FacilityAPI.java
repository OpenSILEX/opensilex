/*
 * *****************************************************************************
 *                         FacilityAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 28/05/2024 16:03
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import io.swagger.annotations.*;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.displayable.DisplayableResponseException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.organisation.api.OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID;

/**
 * @author vidalmor
 */
@Api(CREDENTIAL_GROUP_ORGANIZATION_ID)
@Path(FacilityAPI.PATH)
@ApiCredentialGroup(
        groupId = FacilityAPI.CREDENTIAL_GROUP_FACILITY_ID,
        groupLabelKey = FacilityAPI.CREDENTIAL_GROUP_FACILITY_LABEL_KEY
)
public class FacilityAPI {

    public static final String PATH = "/core/facilities";
    public static final String CREDENTIAL_GROUP_FACILITY_ID = "Facilities";
    public static final String CREDENTIAL_GROUP_FACILITY_LABEL_KEY = "credential-groups.facilities";

    public static final String CREDENTIAL_FACILITY_MODIFICATION_ID = "facility-modification";
    public static final String CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_FACILITY_DELETE_ID = "facility-delete";
    public static final String CREDENTIAL_FACILITY_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBServiceV2 nosql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @ApiOperation("Create a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a facility", response = URI.class),
            @ApiResponse(code = 409, message = "A facility with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createFacility(
            @ApiParam("Facility description") @Valid FacilityCreationDTO dto
    ) throws Exception {
        try {
            FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);
            FacilityModel facility = dto.newModel();

            List<LocationObservationModel> locations = new ArrayList<>();

            if (!dto.getLocations().isEmpty()) {
                locations = dto.getLocations().stream().map(LocationObservationDTO::newModel).collect(Collectors.toList());
            }

            facility = facilityLogic.create(
                    facility,
                    locations.isEmpty() ? null : locations,
                    currentUser
            );

            return new CreatedUriResponse(facility.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Facility already exists", e.getMessage()).getResponse();
        } catch (NotAllowedException notAllowedException) {
            return new DisplayableResponseException(
                    "Error on dates: A facility can't be at 2 different locations in the same time",
                    Response.Status.FORBIDDEN,
                    "Error on dates",
                    null,
                    null
            ).getResponse();
        }
    }

    @GET
    @Path("all_facilities")
    @ApiOperation("Get all facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Facility retrieved", response = NamedResourceDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getAllFacilities() throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);

        FacilitySearchFilter searchFilter = new FacilitySearchFilter();
        searchFilter.setPageSize(0);

        List<FacilityModel> facilities = facilityLogic.minimalSearch(searchFilter.setUser(currentUser)).getList();

        List<NamedResourceDTO> dtoList = facilities.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a facility")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Facility retrieved", response = FacilityGetDTO.class),
            @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getFacility(
            @ApiParam(value = "facility URI", example = "http://opensilex.dev/organisations/facility/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);
        FacilityModel model = facilityLogic.get(uri, currentUser);
        FacilityGetDTO facilityGetDTO = FacilityGetDTO.getDTOFromModel(
                model,
                true
        );

        if (Objects.nonNull(model.getPublisher())) {
            facilityGetDTO.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }

        if (!Objects.isNull(model.getLocationObservationCollection())) {
            LocationObservationModel lastLocationObservation = facilityLogic.getLastFacilityLocationModel(model);
            if (Objects.nonNull(lastLocationObservation)) {
                LocationObservationDTO locationDto = LocationObservationDTO.getDTOFromModel(lastLocationObservation);
                facilityGetDTO.setLastPosition(locationDto);
            }
        }

        return new SingleObjectResponse<>(facilityGetDTO).getResponse();
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get facilities by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return facilities", response = FacilityNamedDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Facility not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getFacilitiesByURI(
            @ApiParam(value = "Facilities URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty @ValidURI List<URI> uris) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);

        List<FacilityModel> facilities = facilityLogic.getList(uris, currentUser);

        if (facilities.isEmpty()) {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Facilities not found", "Unknown facilities URIs").getResponse();
        }

        List<FacilityNamedDTO> dtoList = facilities.stream()
                .map(FacilityNamedDTO::new)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @ApiOperation("Search facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return facilities", response = FacilityGetDTO.class, responseContainer = "List")
    })
    public Response searchFacilities(
            @ApiParam(value = "Regex pattern for filtering facilities by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of organizations hosted by the facilities to filter") @QueryParam("organizations") List<URI> organizations,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);
        FacilitySearchFilter filter = createSearchFilter(pattern, organizations, page, pageSize, orderByList);

        ListWithPagination<FacilityModel> facilities = facilityLogic.search(filter);

        List<FacilityGetDTO> dtoList = facilities.getList().stream()
                .map(facilityModel -> FacilityGetDTO.getDTOFromModel(facilityModel, true))
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("minimal_search")
    @ApiOperation("Search facilities returning minimal embedded information for better performance")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return facilities", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response minimalSearchFacilities(
            @ApiParam(value = "Regex pattern for filtering facilities by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of organizations hosted by the facilities to filter") @QueryParam("organizations") List<URI> organizations,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);
        FacilitySearchFilter filter = createSearchFilter(pattern, organizations, page, pageSize, orderByList);

        ListWithPagination<FacilityModel> facilities = facilityLogic.minimalSearch(filter);

        List<NamedResourceDTO> dtoList = facilities.getList().stream()
                .map(NamedResourceDTO::getDTOFromModel)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Facility deleted", response = URI.class),
            @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response deleteFacility(
            @ApiParam(value = "Facility URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);

        facilityLogic.delete(uri, currentUser);

        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @PUT
    @ApiOperation("Update a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACILITY_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACILITY_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return updated facility", response = URI.class),
            @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response updateFacility(
            @ApiParam("Facility description")
            @Valid FacilityUpdateDTO dto
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);

        FacilityModel facility = dto.newModel();

        List<LocationObservationModel> locations = new ArrayList<>();

        if (Objects.nonNull(dto.getLocations())) {
            locations = dto.getLocations().stream().map(LocationObservationDTO::newModel).collect(Collectors.toList());
        }

        try {
            facility = facilityLogic.update(
                    facility,
                    locations.isEmpty() ? null : locations,
                    currentUser
            );

            return new ObjectUriResponse(Response.Status.OK, facility.getUri()).getResponse();

        } catch (NotAllowedException notAllowedException) {
            return new DisplayableResponseException(
                    "Error on dates: A facility can't be at 2 different locations in the same time",
                    Response.Status.FORBIDDEN,
                    "Error on dates",
                    null,
                    null
            ).getResponse();
        }
    }

    @GET
    @Path("/with_location")
    @ApiOperation("Get only a list of facilities with a position (address/spatial coordinates")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Facilities retrieved", response = FacilityGetWithGeometryDTO.class, responseContainer = "List")
    })
    public Response getFacilitiesWithGeometry(
            @ApiParam(value = "End date : match position affected before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("endDateTime") String endDate
    ) throws Exception {
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql);
        List<FacilityGetWithGeometryDTO> facilityDTOList = new ArrayList<>();

        Map<FacilityModel, LocationObservationModel> facilitesAndLocationsMap = facilityLogic.getFacilitiesWithPosition(
                Objects.nonNull(endDate) ? Instant.parse(endDate) : null,
                currentUser
        );

        facilitesAndLocationsMap.forEach((k, v) -> {
            FacilityGetWithGeometryDTO facilityDTO = new FacilityGetWithGeometryDTO();
            facilityDTO.fromModel(k, v);
            facilityDTOList.add(facilityDTO);
        });

        return new PaginatedListResponse<>(facilityDTOList).getResponse();
    }

    private FacilitySearchFilter createSearchFilter(String pattern, List<URI> organizations, int page, int pageSize, List<OrderBy> orderByList) {
        FacilitySearchFilter filter = (FacilitySearchFilter) new FacilitySearchFilter()
                .setUser(currentUser)
                .setPattern(pattern)
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);
        if (!organizations.isEmpty()) {
            filter.setOrganizations(organizations);
        }
        return filter;
    }
}
