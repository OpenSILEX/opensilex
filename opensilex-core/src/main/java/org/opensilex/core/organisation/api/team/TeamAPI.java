/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.team;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opensilex.core.organisation.api.InfrastructureAPI;
import org.opensilex.core.organisation.dal.InfrastructureDAO;
import org.opensilex.core.organisation.dal.InfrastructureTeamModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import static org.opensilex.security.group.api.GroupAPI.CREDENTIAL_GROUP_DELETE_ID;
import static org.opensilex.security.group.api.GroupAPI.CREDENTIAL_GROUP_DELETE_LABEL_KEY;
import static org.opensilex.security.group.api.GroupAPI.CREDENTIAL_GROUP_GROUP_ID;
import static org.opensilex.security.group.api.GroupAPI.CREDENTIAL_GROUP_MODIFICATION_ID;
import static org.opensilex.security.group.api.GroupAPI.CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;

import static org.opensilex.core.organisation.api.InfrastructureAPI.*;

/**
 *
 * @author vidalmor
 */
@Api(CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("/core/teams")
@ApiCredentialGroup(
        groupId = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
        groupLabelKey = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY
)
public class TeamAPI {

   
    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;


    @POST
    @ApiOperation("Create a team")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_GROUP_ID,
            groupLabelKey = CREDENTIAL_GROUP_GROUP_ID,
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "a team is created", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A team with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createInfrastructureTeam(
            @ApiParam("Team description") @Valid InfrastructureTeamDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql);
            InfrastructureTeamModel model = dao.createTeam(dto.newModel(), currentUser);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Team already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a team")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Team retrieved", response = InfrastructureTeamDTO.class),
        @ApiResponse(code = 404, message = "Team URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructureTeam(
            @ApiParam(value = "Team URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureTeamModel model = dao.getTeam(uri, currentUser);
        return new SingleObjectResponse<>(InfrastructureTeamDTO.getDTOFromModel(model)).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a team")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_GROUP_ID,
            groupLabelKey = CREDENTIAL_GROUP_GROUP_ID,
            credentialId = CREDENTIAL_GROUP_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GROUP_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Team deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Team URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructureTeam(
            @ApiParam(value = "Team URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.deleteTeam(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @PUT
    @ApiOperation("Update a team")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_GROUP_ID,
            groupLabelKey = CREDENTIAL_GROUP_GROUP_ID,
            credentialId = CREDENTIAL_GROUP_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GROUP_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated team uri", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Team URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructureTeam(
            @ApiParam("Team description")
            @Valid InfrastructureTeamDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureTeamModel team = dao.updateTeam(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, team.getUri()).getResponse();
        return response;
    }
}
