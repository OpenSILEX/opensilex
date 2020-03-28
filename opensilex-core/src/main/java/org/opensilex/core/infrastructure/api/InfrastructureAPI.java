/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.infrastructure.dal.InfrastructureDAO;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.sparql.dto.ResourceTreeDTO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.rest.sparql.response.ResourceTreeResponse;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.tree.ResourceTree;

/**
 *
 * @author vidalmor
 */
@Api(InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("/core/infrastructure")
public class InfrastructureAPI {

    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_ID = "Infrastructures";
    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY = "credential-groups.infrastructures";

    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID = "infrastructure-modification";
    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY = "credential.infrastructure.modification";

    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_ID = "infrastructure-delete";
    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY = "credential.infrastructure.delete";

    public static final String CREDENTIAL_INFRASTRUCTURE_READ_ID = "infrastructure-read";
    public static final String CREDENTIAL_INFRASTRUCTURE_READ_LABEL_KEY = "credential.infrastructure.read";

    private final SPARQLService sparql;

    /**
     * Inject SPARQL service
     */
    @Inject
    public InfrastructureAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @POST
    @Path("create")
    @ApiOperation("Create an infrastructure")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
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
            InfrastructureModel model = dao.create(dto.newModel());
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Infrastructure already exists", e.getMessage()).getResponse();
        }
    }

    /**
     * @param xpUri the Experiment URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the {@link ExperimentGetDTO}
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get an experiment by URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
            credentialId = CREDENTIAL_INFRASTRUCTURE_READ_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Infrastructure retrieved", response = InfrastructureGetDTO.class),
        @ApiResponse(code = 204, message = "No experiment found", response = ErrorResponse.class)
    })
    public Response getInfrastructure(
            @ApiParam(value = "Infrastructure URI", example = "http://opensilex.dev/infrastructures/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(InfrastructureGetDTO.fromModel(model)).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NO_CONTENT, "Infrastructure not found",
                    "Unknown infrastructure URI: " + uri.toString()
            ).getResponse();
        }
    }

    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an infrastructure")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
            credentialId = CREDENTIAL_INFRASTRUCTURE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteInfrastructure(
            @ApiParam(value = "Infrastructure URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Search infrstructure tree
     *
     * @param pattern
     * @param securityContext
     * @return
     * @throws Exception
     */
    @GET
    @Path("search")
    @ApiOperation("Search infrastructures tree")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
            credentialId = CREDENTIAL_INFRASTRUCTURE_READ_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return list of infrastructure tree", response = ResourceTreeDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchInfrastructuresTree(
            @ApiParam(value = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @Context SecurityContext securityContext
    ) throws Exception {
        UserModel user = (UserModel) securityContext.getUserPrincipal();
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        ResourceTree<InfrastructureModel> tree = dao.searchTree(
                pattern,
                user
        );

        boolean enableSelection = (pattern != null && !pattern.isEmpty());
        return new ResourceTreeResponse(ResourceTreeDTO.fromResourceTree(tree, enableSelection)).getResponse();
    }

    @PUT
    @Path("update")
    @ApiOperation("Update an infrastructure")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated infrastructure", response = String.class),
        @ApiResponse(code = 400, message = "Invalid parameters")
    })
    public Response updateInfrastructure(
            @ApiParam("Infrastructure description")
            @Valid InfrastructureUpdateDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        Response response;
        if (sparql.uriExists(InfrastructureModel.class, dto.getUri())) {
            InfrastructureModel infrastructure = dao.update(dto.newModel());

            response = new ObjectUriResponse(Response.Status.OK, infrastructure.getUri()).getResponse();
        } else {
            response = new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Infrastructure not found",
                    "Unknown infrastructure URI: " + dto.getUri()
            ).getResponse();
        }

        return response;
    }
}
