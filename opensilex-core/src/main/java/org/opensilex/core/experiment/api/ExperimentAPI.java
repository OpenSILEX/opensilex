/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.server.response.*;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
@Api("Experiments")
@Path("/core/experiment")
public class ExperimentAPI {

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Create an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(
        @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
    ) {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        try {
            ExperimentModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                Response.Status.CONFLICT,
                "Experiment already exists",
                duplicateUriException.getMessage()
            ).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }



    @PUT
    @ApiOperation("Update an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
        @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel model = dto.newModel();
            dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

//    @PUT
//    @ApiOperation("Update an experiment")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response updateWithVariableList(
//        @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
//    ) {
//        try {
//            ExperimentDAO dao = new ExperimentDAO(sparql);
//            ExperimentModel model = dto.newModel();
//            dao.update(model);
//            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
//        } catch (Exception e) {
//            return new ErrorResponse(e).getResponse();
//        }
//    }
//
//    @PUT
//    @ApiOperation("Update an experiment")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response updateWithSensorList(
//        @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
//    ) {
//        try {
//            ExperimentDAO dao = new ExperimentDAO(sparql);
//            ExperimentModel model = dto.newModel();
//            dao.update(model);
//            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
//        } catch (Exception e) {
//            return new ErrorResponse(e).getResponse();
//        }
//    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an experiment by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
        @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel model = dao.get(uri);

            if (model != null) {
                return new SingleObjectResponse<>(
                    ExperimentGetDTO.fromModel(model)
                ).getResponse();
            } else {
                return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Project not found",
                    "Unknown project URI: " + uri.toString()
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Search experiments
     *
     * @param label       Regex pattern for filtering list by alias
     * @param uri         : the experiment URI
     * @param orderByList List of fields to sort as an array of
     *                    fieldName=asc|desc
     * @param page        Page number
     * @param pageSize    Page size
     * @return filtered, ordered and paginated list
     * @see ExperimentDAO
     */
    @GET
    @Path("search")
    @ApiOperation("Search Experiments")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Experiment list", response = ExperimentGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response search(
        @ApiParam(value = "Regex pattern for filtering list by label", example = "Diaphen") @QueryParam("label") String label,
        @ApiParam(value = "Experiment URI", example = "http://example.com/") @QueryParam("uri") @ValidURI URI uri,

        @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
        @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
        @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) {

        try {
            ExperimentDAO xpDao = new ExperimentDAO(sparql);

            ExperimentGetDTO xpGetDTO = new ExperimentGetDTO();
            xpGetDTO.setLabel(label);
            xpGetDTO.setUri(uri);

            ListWithPagination<ExperimentModel> resultList = xpDao.search(
                xpGetDTO,
                orderByList,
                page,
                pageSize
            );

            // Convert paginated list to DTO
            ListWithPagination<ExperimentGetDTO> resultDTOList = resultList.convert(
                ExperimentGetDTO.class,
                ExperimentGetDTO::fromModel
            );
            // Return paginated list of factor DTO
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }

    }

    /**
     * Remove an experiment
     *
     * @param uri the experiment URI
     * @return the URI of the deleted experiment
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
        @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
