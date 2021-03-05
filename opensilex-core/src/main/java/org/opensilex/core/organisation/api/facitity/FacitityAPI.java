/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;

import static org.opensilex.core.organisation.api.InfrastructureAPI.*;
import org.opensilex.server.response.PaginatedListResponse;

/**
 *
 * @author vidalmor
 */
@Api(CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("core/facilities")
@ApiCredentialGroup(
        groupId = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
        groupLabelKey = InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY
)
public class FacitityAPI {

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Create a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a facility", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A facility with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createInfrastructureFacility(
            @ApiParam("Facility description") @Valid InfrastructureFacilityCreationDTO dto
    ) throws Exception {
        try {
            InfrastructureDAO dao = new InfrastructureDAO(sparql);
            InfrastructureFacilityModel model = dao.createFacility(dto.newModel(), currentUser);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Facility already exists", e.getMessage()).getResponse();
        }
    }

    @GET
    @Path("all")
    @ApiOperation("Get all facilities")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility retrieved", response = InfrastructureFacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getAllFacilities() throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        List<InfrastructureFacilityModel> facilities = dao.getAllFacilities(currentUser);

        List<InfrastructureFacilityGetDTO> resultDTOList = new ArrayList<>(facilities.size());
        facilities.forEach(result -> {
            resultDTOList.add(InfrastructureFacilityGetDTO.getDTOFromModel(result));
        });

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a facility")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility retrieved", response = InfrastructureFacilityGetDTO.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response getInfrastructureFacility(
            @ApiParam(value = "facility URI", example = "http://opensilex.dev/organisations/facility/phenoarch", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        InfrastructureFacilityModel model = dao.getFacility(uri, currentUser);
        return new SingleObjectResponse<>(InfrastructureFacilityGetDTO.getDTOFromModel(model)).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Facility deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response deleteInfrastructureFacility(
            @ApiParam(value = "Facility URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        dao.deleteFacility(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @PUT
    @ApiOperation("Update a facility")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return updated facility", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Facility URI not found", response = ErrorResponse.class)
    })
    public Response updateInfrastructureFacility(
            @ApiParam("Facility description")
            @Valid InfrastructureFacilityUpdateDTO dto
    ) throws Exception {
        InfrastructureDAO dao = new InfrastructureDAO(sparql);

        Response response;
        InfrastructureFacilityModel infrastructure = dao.updateFacility(dto.newModel(), currentUser);

        response = new ObjectUriResponse(Response.Status.OK, infrastructure.getUri()).getResponse();

        return response;
    }

}
