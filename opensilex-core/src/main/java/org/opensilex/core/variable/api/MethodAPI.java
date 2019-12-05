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
import org.opensilex.core.variable.dal.MethodDAO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.MultipleObjectsResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;




@Api("Variables")
@Path("/core/variable/method")
public class MethodAPI implements RestApplicationAPI {

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create a method")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
            @ApiParam("Method description") @Valid MethodCreationDTO dto
    ) throws Exception {
        MethodDAO dao = new MethodDAO(sparql);
        try {
            MethodModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Method already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update a method")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @ApiParam(value = "Method URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Method description") @Valid MethodUpdateDTO dto
    ) throws Exception {
        MethodDAO dao = new MethodDAO(sparql);

        MethodModel model = dao.get(uri);
        if (model != null) {
            dao.update(dto.defineModel(model));
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Method not found",
                    "Unknown method URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a method")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
            @ApiParam(value = "Method URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MethodDAO dao = new MethodDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a method")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @ApiParam(value = "Method URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MethodDAO dao = new MethodDAO(sparql);
        MethodModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    MethodGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Method not found",
                    "Unknown method URI: " + uri.toString()
            ).getResponse();
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search entities corresponding to given criteria")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(
            @ApiParam(value = "Name regex pattern") @QueryParam("name") String namePattern,
            @ApiParam(value = "Comment regex pattern") @QueryParam("comment") String commentPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        MethodDAO dao = new MethodDAO(sparql);
        ListWithPagination<MethodModel> resultList = dao.search(
                namePattern,
                commentPattern,
                orderByList, 
                page, 
                pageSize
        );
        ListWithPagination<MethodGetDTO> resultDTOList = resultList.convert(
                MethodGetDTO.class,
                MethodGetDTO::fromModel
        );
        return new MultipleObjectsResponse<>(resultDTOList).getResponse();
    }
}