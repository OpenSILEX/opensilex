/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import io.swagger.annotations.*;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.ResourceDagDTOBuilder;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author vidalmor
 */
@Api(OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID)
@Path("/core/organisations")
@ApiCredentialGroup(
        groupId = OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID,
        groupLabelKey = OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_LABEL_KEY
)
public class OrganizationAPI {

    public static final String CREDENTIAL_GROUP_ORGANIZATION_ID = "Organizations";
    public static final String CREDENTIAL_GROUP_ORGANIZATION_LABEL_KEY = "credential-groups.organizations";

    public static final String CREDENTIAL_ORGANIZATION_MODIFICATION_ID = "organization-modification";
    public static final String CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_ORGANIZATION_DELETE_ID = "organization-delete";
    public static final String CREDENTIAL_ORGANIZATION_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @ApiOperation("Create an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an organisation", response = URI.class),
        @ApiResponse(code = 409, message = "An organisation with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createOrganization(
            @ApiParam("Organisation description") @Valid OrganizationCreationDTO dto
    ) throws Exception {
        try {
            OrganizationDAO dao = new OrganizationDAO(sparql);
            OrganizationModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            return new CreatedUriResponse(model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Organisation already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an organisation ")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Organisation retrieved", response = OrganizationGetDTO.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response getOrganization(
            @ApiParam(value = "Organisation URI", example = "http://opensilex.dev/organisation/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);
        OrganizationModel model = dao.get(uri, currentUser);
        OrganizationGetDTO dto = OrganizationGetDTO.getDTOFromModel(model);
        if (Objects.nonNull(model.getPublisher())){
            dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
        }
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Organisation deleted", response = URI.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response deleteOrganization(
            @ApiParam(value = "Organisation URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation("Search organisations")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return organisations", response = OrganizationDagDTO.class, responseContainer = "List")
    })
    public Response searchOrganizations(
            @ApiParam(value = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = " organisation URIs") @QueryParam("organisation_uris") List<URI> restrictedOrganizationUris,
            @ApiParam(value = "Regex pattern for filtering list by types", example = ".*") @QueryParam("type") URI type,
            @ApiParam(value = "Organization every result will be direct child of") @QueryParam("parent_organization_uri") URI parentOrganizationUri,
            @ApiParam(value = "Facility for filtering") @QueryParam("facility_uri") URI facilityUri
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);

        List<OrganizationModel> organizations = dao.search(new OrganizationSearchFilter()
                .setNameFilter(pattern)
                .setTypeUriFilter(type)
                .setDirectChildURI(parentOrganizationUri)
                .setFacilityURI(facilityUri)
                .setRestrictedOrganizations(restrictedOrganizationUris.isEmpty() ? null : restrictedOrganizationUris)
                .setUser(currentUser));
        ResourceDagDTOBuilder<OrganizationModel> dtoBuilder = new OrganizationDagDTOBuilder(organizations);
        return new PaginatedListResponse<>(dtoBuilder.build()).getResponse();
    }

    @PUT
    @ApiOperation("Update an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated organisation", response = URI.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response updateOrganization(
            @ApiParam("Organisation description")
            @Valid OrganizationUpdateDTO dto
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);

        OrganizationModel organization = dao.update(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, organization.getUri()).getResponse();

        return response;
    }
}
