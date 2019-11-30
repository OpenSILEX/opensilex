//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;

import io.swagger.annotations.*;
import java.net.*;
import java.util.*;
import javax.inject.*;
import javax.validation.*;
import javax.validation.constraints.*;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import org.opensilex.core.variable.dal.*;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.*;
import org.opensilex.server.security.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.exceptions.*;
import org.opensilex.sparql.utils.*;
import org.opensilex.utils.*;



@Api("Variables")
@Path("/core/variable/entity")
public class EntityAPI implements RestApplicationAPI {

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create an entity")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
            @ApiParam("Entity description") @Valid EntityCreationDTO dto
    ) throws Exception {
        EntityDAO dao = new EntityDAO(sparql);
        try {
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
    @ApiProtected()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Entity description") @Valid EntityUpdateDTO dto
    ) throws Exception {
        EntityDAO dao = new EntityDAO(sparql);

        EntityModel model = dao.get(uri);
        if (model != null) {
            dao.update(dto.updateModel(model));
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        EntityDAO dao = new EntityDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an entity")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @ApiParam(value = "Entity URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        EntityDAO dao = new EntityDAO(sparql);
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
    @Path("find")
    @ApiOperation("Find entities corresponding to given criteria")
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
        EntityDAO dao = new EntityDAO(sparql);
        ListWithPagination<EntityModel> resultList = dao.find(
                namePattern,
                commentPattern,
                orderByList, 
                page, 
                pageSize
        );
        ListWithPagination<EntityGetDTO> resultDTOList = resultList.convert(
                EntityGetDTO.class,
                EntityGetDTO::fromModel
        );
        return new MultipleObjectsResponse<>(resultDTOList).getResponse();
    }
}