/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

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
@Tag(name = OrganizationAPI.CREDENTIAL_GROUP_ORGANIZATION_ID)
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
    @Operation(summary = "Create an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Create an organisation", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "409", description = "An organisation with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })

    public Response createOrganization(
            @Parameter(description = "Organisation description") @Valid OrganizationCreationDTO dto
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
    @Operation(summary = "Get an organisation ")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organisation retrieved", content = @Content(schema = @Schema(implementation = OrganizationGetDTO.class))),
        @ApiResponse(responseCode = "404", description = "Organisation URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getOrganization(
            @Parameter(description = "Organisation URI", example = "http://opensilex.dev/organisation/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
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
    @Operation(summary = "Delete an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organisation deleted", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "404", description = "Organisation URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteOrganization(
            @Parameter(description = "Organisation URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Operation(summary = "Search organisations")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return organisations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationDagDTO.class))))
    })
    public Response searchOrganizations(
            @Parameter(description = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @Parameter(description = " organisation URIs") @QueryParam("organisation_uris") List<URI> restrictedOrganizationUris,
            @Parameter(description = "Regex pattern for filtering list by types", example = ".*") @QueryParam("type") URI type,
            @Parameter(description = "Organization every result will be direct child of") @QueryParam("parent_organization_uri") URI parentOrganizationUri,
            @Parameter(description = "Facility for filtering") @QueryParam("facility_uri") URI facilityUri
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
    @Operation(summary = "Update an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ORGANIZATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ORGANIZATION_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return updated organisation", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "404", description = "Organisation URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateOrganization(
            @Parameter(description = "Organisation description")
            @Valid OrganizationUpdateDTO dto
    ) throws Exception {
        OrganizationDAO dao = new OrganizationDAO(sparql);

        OrganizationModel organization = dao.update(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, organization.getUri()).getResponse();

        return response;
    }
}
