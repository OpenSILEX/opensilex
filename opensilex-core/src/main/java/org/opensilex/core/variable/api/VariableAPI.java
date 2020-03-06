//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

//@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path("/core/variable")
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
    public VariableAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    @POST
    @ApiOperation("Create a variable")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
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
            VariableModel variable = variableDTO.newModel();
            dao.create(variable);
            return new ObjectUriResponse(Response.Status.CREATED, variable.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Variable already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update a variable")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
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
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
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
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
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
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
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
