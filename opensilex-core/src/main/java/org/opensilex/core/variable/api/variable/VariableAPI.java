//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.variable;

import io.swagger.annotations.*;
import org.opensilex.core.variable.dal.variable.VariableDAO;
import org.opensilex.core.variable.dal.variable.VariableModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path("/core/variable")
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariableAPI {

    public static final String CREDENTIAL_VARIABLE_GROUP_ID = "Variables";
    public static final String CREDENTIAL_VARIABLE_GROUP_LABEL_KEY = "credential-groups.variables";

    public static final String CREDENTIAL_VARIABLE_MODIFICATION_ID = "variable-modification";
    public static final String CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY = "credential.variable.modification";

    public static final String CREDENTIAL_VARIABLE_DELETE_ID = "variable-delete";
    public static final String CREDENTIAL_VARIABLE_DELETE_LABEL_KEY = "credential.variable.delete";

    public static final String CREDENTIAL_VARIABLE_READ_ID = "variable-read";
    public static final String CREDENTIAL_VARIABLE_READ_LABEL_KEY = "credential.variable.read";

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVariable(
            @ApiParam("Variable description") @Valid VariableCreationDTO variableDTO
    ) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        try {

            // then get variable model and update with inserted trait
            VariableModel variable = variableDTO.newModel();
            dao.create(variable);
            sparql.commitTransaction();

            return new ObjectUriResponse(Response.Status.CREATED, variable.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Variable already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        } catch (Exception e){
            sparql.rollbackTransaction();
            return new ErrorResponse(Response.Status.BAD_REQUEST,"Variable create error",e.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariable(
            @ApiParam(value = "Variable URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Variable description") @Valid VariableUpdateDTO variableDTO
    ) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);

        VariableModel variable = dao.get(uri);
        if (variable != null) {
            dao.update(variableDTO.defineModel(variable));
            return new ObjectUriResponse(Response.Status.OK, variable.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Variable not found",
                    "Unknown variable URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariable(
            @ApiParam(value = "Variable URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a variable")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariable(
            @ApiParam(value = "Variable URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        VariableModel variable = dao.get(uri);

        if (variable != null) {
            return new SingleObjectResponse<>(
                    VariableGetDTO.fromModel(variable)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Variable not found",
                    "Unknown variable site URI: " + uri.toString()
            ).getResponse();
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search variables corresponding to given criteria")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Variable list", response = VariableGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariables(
            @ApiParam(value = "Name regex pattern") @QueryParam("name") String namePattern,
            @ApiParam(value = "Comment regex pattern") @QueryParam("comment") String commentPattern,
            @ApiParam(value = "Filter by entity URI") @QueryParam("entity") URI entity,
            @ApiParam(value = "Filter by quality URI") @QueryParam("quality") URI quality,
            @ApiParam(value = "Filter by method URI") @QueryParam("method") URI method,
            @ApiParam(value = "Filter by unit URI") @QueryParam("unit") URI unit,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        VariableDAO dao = new VariableDAO(sparql);
        ListWithPagination<VariableModel> resultList = dao.search(
                namePattern,
                commentPattern,
                entity,
                quality,
                method,
                unit,
                orderByList,
                page,
                pageSize
        );
        ListWithPagination<VariableGetDTO> resultDTOList = resultList.convert(
                VariableGetDTO.class,
                VariableGetDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}

