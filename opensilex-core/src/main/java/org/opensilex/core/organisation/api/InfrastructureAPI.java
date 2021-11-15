/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.organisation.dal.InfrastructureDAO;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.ResourceDagDTO;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vidalmor
 */
@Api(InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("/core/organisations")
@ApiCredentialGroup(
        groupId = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
        groupLabelKey = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY
)
public class InfrastructureAPI {

    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_ID = "Organisations";
    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY = "credential-groups.infrastructures";

    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID = "infrastructure-modification";
    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY = "credential.infrastructure.modification";

    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_ID = "infrastructure-delete";
    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY = "credential.infrastructure.delete";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Create an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an organisation", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An organisation with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createInfrastructure(
            @ApiParam("Organisation description") @Valid InfrastructureCreationDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql);
            InfrastructureModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();

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
        @ApiResponse(code = 200, message = "Organisation retrieved", response = InfrastructureGetDTO.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructure(
            @ApiParam(value = "Organisation URI", example = "http://opensilex.dev/organisation/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureModel model = dao.get(uri, currentUser);
        return new SingleObjectResponse<>(InfrastructureGetDTO.getDTOFromModel(model)).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Organisation deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructure(
            @ApiParam(value = "Organisation URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation("Search organisations")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return organisations", response = ResourceDagDTO.class, responseContainer = "List")
    })
    public Response searchInfrastructures(
            @ApiParam(value = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = " organisation URIs") @QueryParam("organisation_uris") List<URI> infraURIs
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        List<InfrastructureModel> organizations = dao.search(pattern, infraURIs, currentUser);
        return new PaginatedListResponse<>(ResourceDagDTO.getDtoListFromModel(organizations)).getResponse();
    }

    @PUT
    @ApiOperation("Update an organisation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated organisation", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Organisation URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructure(
            @ApiParam("Organisation description")
            @Valid InfrastructureUpdateDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        InfrastructureModel infrastructure = dao.update(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, infrastructure.getUri()).getResponse();

        return response;
    }
}
