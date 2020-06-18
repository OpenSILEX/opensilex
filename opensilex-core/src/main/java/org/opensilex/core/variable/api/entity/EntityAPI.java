//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.*;

import java.net.URI;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_READ_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_READ_LABEL_KEY;

import org.opensilex.core.variable.api.method.MethodGetDTO;
import org.opensilex.core.variable.api.variable.VariableAPI;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.core.variable.dal.variable.BaseVariableDAO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path("/core/variable/entity")
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class EntityAPI {

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a Entity", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A Entity with the same URI already exists", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )
    public Response createEntity(
            @ApiParam("Entity description") @Valid EntityCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
            EntityModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Entity already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Entity URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )
    public Response updateEntity(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Entity description") @Valid EntityUpdateDTO dto
    ) throws Exception {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class,sparql);

            EntityModel model = dao.get(uri);
            if (model != null) {
                dao.update(dto.defineModel(model));
                return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
            } else {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Entity not found",
                        "Unknown entity URI: " + uri
                ).getResponse();
            }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEntity(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit retrieved", response = MethodGetDTO.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response getEntity(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
            EntityModel model = dao.get(uri);

            if (model != null) {
                return new SingleObjectResponse<>(
                        EntityGetDTO.fromModel(model)
                ).getResponse();
            } else {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Entity not found",
                        "Unknown entity URI: " + uri.toString()
                ).getResponse();
            }
    }

    @GET
    @Path("search")
    @ApiOperation("Search entities corresponding to given criteria")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Entity list", response = EntityGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchEntities(
            @ApiParam(value = "Name regex pattern") @QueryParam("name") String namePattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
            ListWithPagination<EntityModel> resultList = dao.search(
                    namePattern,
                    orderByList,
                    page,
                    pageSize
            );
            ListWithPagination<EntityGetDTO> resultDTOList = resultList.convert(
                    EntityGetDTO.class,
                    EntityGetDTO::fromModel
            );
            return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
