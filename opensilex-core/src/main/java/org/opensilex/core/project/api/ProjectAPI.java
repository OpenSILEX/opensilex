//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.api;

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
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.rest.security.ApiProtected;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;




@Api("Projects")
@Path("/core/project")
public class ProjectAPI {

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create a project")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
            @ApiParam("Project description") @Valid ProjectCreationDTO dto
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        try {
            ProjectModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Project already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
    @ApiOperation("Update a project")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @ApiParam(value = "Project URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Project description") @Valid ProjectUpdateDTO dto
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);

        ProjectModel model = dao.get(uri);
        if (model != null) {
            dao.update(dto.updateModel(model));
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Project not found",
                    "Unknown project URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a project")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
            @ApiParam(value = "Project URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a project")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @ApiParam(value = "Project URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        ProjectModel model = dao.get(uri);
        
        if (model != null) {
            return new SingleObjectResponse<>(
                    ProjectGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Project not found",
                    "Unknown project URI: " + uri.toString()
            ).getResponse();
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search entities corresponding to given criteria")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        ListWithPagination<ProjectModel> resultList = dao.search(
                orderByList, 
                page, 
                pageSize
        );
        ListWithPagination<ProjectGetDTO> resultDTOList = resultList.convert(
                ProjectGetDTO.class,
                ProjectGetDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}