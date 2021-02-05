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
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;

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
    @ApiOperation("Create an infrastructure")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an infrastructure", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An infrastructure with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createInfrastructure(
            @ApiParam("Infrastructure description") @Valid InfrastructureCreationDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql);
            InfrastructureModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an experiment by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Infrastructure retrieved", response = InfrastructureGetDTO.class),
        @ApiResponse(code = 404, message = "Infrastructure URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructure(
            @ApiParam(value = "Infrastructure URI", example = "http://opensilex.dev/infrastructures/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureModel model = dao.get(uri, currentUser);
        return new SingleObjectResponse<>(InfrastructureGetDTO.getDTOFromModel(model)).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an infrastructure")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Infrastructure deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Infrastructure URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructure(
            @ApiParam(value = "Infrastructure URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation("Search infrastructures tree")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of infrastructure tree", response = ResourceTreeDTO.class, responseContainer = "List")
    })
    public Response searchInfrastructuresTree(
            @ApiParam(value = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "List of infrasctures URI limitations in the result") @QueryParam("infraURIs") List<URI> infraURIs
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        SPARQLTreeListModel<InfrastructureModel> tree = dao.searchTree(pattern, infraURIs, currentUser);

        boolean enableSelection = (pattern != null && !pattern.isEmpty());
        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(enableSelection, tree)).getResponse();
    }

    @PUT
    @ApiOperation("Update an infrastructure")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated infrastructure", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Infrastructure URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructure(
            @ApiParam("Infrastructure description")
            @Valid InfrastructureUpdateDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        InfrastructureModel infrastructure = dao.update(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, infrastructure.getUri()).getResponse();

        return response;
    }


    @POST
    @Path("team/create")
    @ApiOperation("Create an infrastructure team")
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
        @ApiResponse(code = 201, message = "Create an infrastructure team", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An infrastructure team with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createInfrastructureTeam(
            @ApiParam("Infrastructure team description") @Valid InfrastructureTeamDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql);
            InfrastructureTeamModel model = dao.createTeam(dto.newModel(), currentUser);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure team already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("team/get/{uri}")
    @ApiOperation("Get an infrastructure team by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Infrastructure team retrieved", response = InfrastructureTeamDTO.class),
        @ApiResponse(code = 404, message = "Infrastructure team URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructureTeam(
            @ApiParam(value = "Infrastructure team URI", example = "http://opensilex.dev/infrastructures/facility/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureTeamModel model = dao.getTeam(uri, currentUser);
        return new SingleObjectResponse<>(InfrastructureTeamDTO.getDTOFromModel(model)).getResponse();
    }

    @DELETE
    @Path("team/delete/{uri}")
    @ApiOperation("Delete an infrastructure team")
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
        @ApiResponse(code = 200, message = "Infrastructure team deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Infrastructure team URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructureTeam(
            @ApiParam(value = "Infrastructure team URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.deleteTeam(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @PUT
    @Path("team/update")
    @ApiOperation("Update an infrastructure team")
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
        @ApiResponse(code = 200, message = "Return updated infrastructure uri", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Infrastructure team URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructureTeam(
            @ApiParam("Infrastructure team description")
            @Valid InfrastructureTeamDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureTeamModel team = dao.updateTeam(dto.newModel(), currentUser);
        Response response = new ObjectUriResponse(Response.Status.OK, team.getUri()).getResponse();
        return response;
    }
}
