package org.opensilex.core.organisation.api.site;

import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.organisation.api.InfrastructureAPI;
import org.opensilex.core.organisation.dal.SiteModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensilex.core.organisation.api.InfrastructureAPI.*;

/**
 * @author Valentin RIGOLLE
 */
@Api(CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("core/sites")
@ApiCredentialGroup(
        groupId = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
        groupLabelKey = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY
)
public class SiteAPI {

    public static final String SITE_MUST_HAVE_PARENT_EXCEPTION = "A site must have at least one parent organization";
    public static final String SITE_MUST_HAVE_PARENT_KEY = "component.organization.exception.siteMustHaveParent";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    UserModel currentUser;

    @GET
    @ApiOperation("Search all sites")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Sites retrieved", response = SiteGetDTO.class, responseContainer = "List")
    })
    public Response searchSites(
            @ApiParam(value = "Regex pattern for filtering sites by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of organizations of the sites to filter") @QueryParam("organizations") List<URI> organizations,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("page_size") int pageSize
    ) throws Exception {
        SiteDAO siteDAO = new SiteDAO(sparql, nosql);

        ListWithPagination<SiteModel> siteModels = siteDAO.searchSites(currentUser, pattern, organizations, orderByList,
                page, pageSize);

        List<SiteGetDTO> siteDtos = siteModels.getList().stream()
                .map(siteModel -> {
                    SiteGetDTO siteDto = new SiteGetDTO();
                    siteDto.fromModel(siteModel);
                    return siteDto;
                }).collect(Collectors.toList());

        return new PaginatedListResponse<>(siteDtos).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a site")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Site retrieved", response = SiteGetDTO.class)
    })
    public Response getSite(
            @ApiParam(value = "Site URI") @PathParam("uri") @NotNull URI siteUri
    ) throws Exception {
        SiteDAO siteDAO = new SiteDAO(sparql, nosql);

        SiteGetDTO siteDto = new SiteGetDTO();
        siteDto.fromModelWithGeospatialInfo(
                siteDAO.getSite(siteUri, currentUser),
                siteDAO.getSiteGeospatialModel(siteUri));

        return new SingleObjectResponse<>(siteDto).getResponse();

    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get a list of sites")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Sites retrieved", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response getSitesByURI(
            @ApiParam(value = "Site URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty @ValidURI List<URI> uris
    ) throws Exception {
        SiteDAO siteDAO = new SiteDAO(sparql, nosql);

        List<SiteModel> siteModels = siteDAO.getSitesByURI(uris, currentUser);

        List<NamedResourceDTO> dtoList = siteModels.stream()
                .map(NamedResourceDTO::getDTOFromModel)
                .collect(Collectors.toList());

        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @POST
    @ApiOperation("Create a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Site created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A site with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createSite(
            @ApiParam("Site creation object") @Valid SiteCreationDTO siteCreationDto
    ) throws Exception {
        if (CollectionUtils.isEmpty(siteCreationDto.getOrganizations())) {
            throw new DisplayableBadRequestException(SITE_MUST_HAVE_PARENT_EXCEPTION, SITE_MUST_HAVE_PARENT_KEY);
        }

        try {
            SiteDAO siteDAO = new SiteDAO(sparql, nosql);

            SiteModel created = siteDAO.createSite(siteCreationDto.newModel(), currentUser);

            return new ObjectUriResponse(Response.Status.CREATED, created.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "A facility with the same URI already exists",
                    e.getMessage()).getResponse();
        }
    }

    @PUT
    @ApiOperation("Update a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Site updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Site URI not found", response = ErrorResponse.class)
    })
    public Response updateSite(
            @ApiParam("Site update object") @Valid SiteUpdateDTO siteUpdateDTO
    ) throws Exception {
        if (CollectionUtils.isEmpty(siteUpdateDTO.getOrganizations())) {
            throw new DisplayableBadRequestException(SITE_MUST_HAVE_PARENT_EXCEPTION, SITE_MUST_HAVE_PARENT_KEY);
        }

        SiteDAO siteDAO = new SiteDAO(sparql, nosql);

        SiteModel siteModel = siteUpdateDTO.newModel();

        SiteModel updated = siteDAO.updateSite(siteModel, currentUser);

        return new ObjectUriResponse(Response.Status.OK, updated.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a site")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Site deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Site URI not found", response = ErrorResponse.class)
    })
    public Response deleteSite(
            @ApiParam(value = "Site URI", required = true) @PathParam("uri") @NotNull @Valid URI siteUri
    ) throws Exception {
        SiteDAO siteDAO = new SiteDAO(sparql, nosql);
        siteDAO.deleteSite(siteUri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, siteUri).getResponse();
    }
}
