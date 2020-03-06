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
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_READ_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_READ_LABEL_KEY;
import org.opensilex.core.variable.dal.UnitDAO;
import org.opensilex.core.variable.dal.UnitModel;
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

//@Api(CREDENTIAL_VARIABLE_GROUP_ID)
//@Path("/core/variable/unit")
public class UnitAPI {

    @Inject
    public UnitAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    @POST
    @ApiOperation("Create a unit")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUnit(
            @ApiParam("Unit description") @Valid UnitCreationDTO dto
    ) throws Exception {
        UnitDAO dao = new UnitDAO(sparql);
        try {
            UnitModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Unit already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update a unit")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnit(
            @ApiParam(value = "Unit URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Unit description") @Valid UnitUpdateDTO dto
    ) throws Exception {
        UnitDAO dao = new UnitDAO(sparql);

        UnitModel model = dao.get(uri);
        if (model != null) {
            dao.update(dto.defineModel(model));
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Unit not found",
                    "Unknown unit URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a unit")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUnit(
            @ApiParam(value = "Unit URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        UnitDAO dao = new UnitDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a unit")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnit(
            @ApiParam(value = "Unit URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        UnitDAO dao = new UnitDAO(sparql);
        UnitModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    UnitGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Unit not found",
                    "Unknown method URI: " + uri.toString()
            ).getResponse();
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search entities corresponding to given criteria")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_VARIABLE_GROUP_ID,
            groupLabelKey = CREDENTIAL_VARIABLE_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUnits(
            @ApiParam(value = "Name regex pattern") @QueryParam("name") String namePattern,
            @ApiParam(value = "Comment regex pattern") @QueryParam("comment") String commentPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        UnitDAO dao = new UnitDAO(sparql);
        ListWithPagination<UnitModel> resultList = dao.search(
                namePattern,
                commentPattern,
                orderByList,
                page,
                pageSize
        );
        ListWithPagination<UnitGetDTO> resultDTOList = resultList.convert(
                UnitGetDTO.class,
                UnitGetDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
