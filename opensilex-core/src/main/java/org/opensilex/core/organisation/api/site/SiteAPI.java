package org.opensilex.core.organisation.api.site;

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
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.api.OrganizationAPI;
import org.opensilex.core.organisation.bll.SiteLogic;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opensilex.core.organisation.api.OrganizationAPI.*;

/**
 * @author Valentin RIGOLLE
 */
@Tag(name = CREDENTIAL_GROUP_ORGANIZATION_ID)
@Path("core/sites")
@ApiCredentialGroup(
        groupId = OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID,
        groupLabelKey = OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_LABEL_KEY
)
public class SiteAPI {

    public static final String SITE_MUST_HAVE_PARENT_EXCEPTION = "A site must have at least one parent organization";
    public static final String SITE_MUST_HAVE_PARENT_KEY = "component.organization.exception.siteMustHaveParent";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBServiceV2 nosql;

    @CurrentUser
    AccountModel currentUser;

    @GET
    @Operation(summary = "Search all sites")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sites retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SiteGetListDTO.class))))
    })
    public Response searchSites(
            @Parameter(description = "Regex pattern for filtering sites by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @Parameter(description = "List of organizations of the sites to filter") @QueryParam("organizations") List<URI> organizations,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number") @QueryParam("page") int page,
            @Parameter(description = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        SiteLogic siteLogic = new SiteLogic(sparql, nosql);

        SiteSearchFilter filter = (SiteSearchFilter) new SiteSearchFilter()
                .setNamePattern(pattern)
                .setUser(currentUser)
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);
        if (!organizations.isEmpty()) {
            filter.setOrganizations(organizations);
        }
        ListWithPagination<SiteModel> siteModels = siteLogic.search(filter);

        ListWithPagination<SiteGetListDTO> siteDtos = siteModels.convert(SiteGetListDTO.class, SiteGetListDTO::getNewDTOFromModel);

        return new PaginatedListResponse<>(siteDtos).getResponse();
    }

    @GET
    @Path("{uri}")
    @Operation(summary = "Get a site")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Site retrieved", content = @Content(schema = @Schema(implementation = SiteGetDTO.class)))
    })
    public Response getSite(
            @Parameter(description = "Site URI") @PathParam("uri") @NotNull URI siteUri
    ) throws Exception {
        SiteLogic siteLogic = new SiteLogic(sparql, nosql);

        SiteModel model = siteLogic.get(siteUri, currentUser);
        LocationObservationModel locationModel= siteLogic.getSiteLocationObservationModel(model);

        SiteGetDTO siteDto = new SiteGetDTO();
        siteDto.fromModelWithGeospatialInfo(model, locationModel.getLocation());

        //@TODO : refactoring publisher check for all models (from API to Logic) - 'getPublisher' in superClass 'AbstractLogic' or 'AccountLogic' ??
        if (Objects.nonNull(model.getPublisher())){
            siteDto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }

        return new SingleObjectResponse<>(siteDto).getResponse();
    }

    @GET
    @Path("by_uris")
    @Operation(summary = "Get a list of sites")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sites retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = NamedResourceDTO.class))))
    })
    public Response getSitesByURI(
            @Parameter(description = "Site URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty @ValidURI List<URI> uris
    ) throws Exception {
        SiteLogic siteLogic = new SiteLogic(sparql, nosql);
        List<SiteModel> siteModels = siteLogic.getList(uris, currentUser);

        List<NamedResourceDTO> dtoList = siteModels.stream()
                .map(NamedResourceDTO::getDTOFromModel)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @POST
    @Operation(summary = "Create a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Site created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "A site with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createSite(
            @Parameter(description = "Site creation object") @Valid SiteCreationDTO siteCreationDto
    ) throws Exception {

        try {
            SiteLogic siteLogic = new SiteLogic(sparql, nosql);
            SiteModel created = siteCreationDto.newModel();

            created = siteLogic.create(created, currentUser);

            return new CreatedUriResponse(created.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "A facility with the same URI already exists",
                    e.getMessage()).getResponse();
        } catch (BadRequestException e) {
            throw new DisplayableBadRequestException(SITE_MUST_HAVE_PARENT_EXCEPTION, SITE_MUST_HAVE_PARENT_KEY);
        }
    }

    @PUT
    @Operation(summary = "Update a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Site updated", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Site URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateSite(
            @Parameter(description = "Site update object") @Valid SiteUpdateDTO siteUpdateDTO
    ) throws Exception {
        try {
            SiteLogic siteLogic = new SiteLogic(sparql, nosql);

            SiteModel siteModel = siteUpdateDTO.newModel();
            siteLogic.update(siteModel, currentUser);

            return new ObjectUriResponse(Response.Status.OK, siteModel.getUri()).getResponse();
        } catch (BadRequestException e) {
            throw new DisplayableBadRequestException(SITE_MUST_HAVE_PARENT_EXCEPTION, SITE_MUST_HAVE_PARENT_KEY);
        }
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Site deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Site URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteSite(
            @Parameter(description = "Site URI", required = true) @PathParam("uri") @NotNull @Valid URI siteUri
    ) throws Exception {
        SiteLogic siteLogic = new SiteLogic(sparql, nosql);

        siteLogic.delete(siteUri, currentUser);

        return new ObjectUriResponse(Response.Status.OK, siteUri).getResponse();
    }

    @GET
    @Path("/with_location")
    @Operation(summary = "Get only the list of sites with a location")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sites retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SiteGetWithGeometryDTO.class))))
    })
    public Response getSitesWithLocation() throws Exception {
        SiteLogic siteLogic = new SiteLogic(sparql, nosql);
        List<SiteGetWithGeometryDTO> siteDTOList = new ArrayList<>();

        Map<SiteModel, LocationObservationModel> sitesAndLocationsMap = siteLogic.getSitesWithPosition(currentUser);

        sitesAndLocationsMap.forEach((k, v) -> {
            SiteGetWithGeometryDTO siteDTO = new SiteGetWithGeometryDTO();
            siteDTO.fromModel(k, v);
            siteDTOList.add(siteDTO);
        });

        return new PaginatedListResponse<>(siteDTOList).getResponse();
    }
}
