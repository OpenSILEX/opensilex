/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import io.swagger.annotations.*;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchDTO;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.rest.validation.date.DateConstraint;
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
import java.time.LocalDate;
import java.util.ArrayList;
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
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        try {
            ExperimentModel createdXp = dao.create(xpDto.newModel());
            return new ObjectUriResponse(Response.Status.CREATED, createdXp.getUri()).getResponse();

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

    @POST
    @Path("experiments")
    @ApiOperation("Create a list of experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAll(
            @ApiParam("Experiment description") @Valid List<ExperimentCreationDTO> xpDtoList
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);

            List<URI> uris = new ArrayList<>(xpDtoList.size());
            for (ExperimentCreationDTO xpDto : xpDtoList) {
                URI xpUri = dao.create(xpDto.newModel()).getUri();
                uris.add(xpUri);
            }
            return new PaginatedListResponse<>(uris).setStatus(Response.Status.CREATED).getResponse();

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
//    @Path("{uri}/variables")
//    @ApiOperation("Update an experiment with a list of variables")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response updateWithVariableList(
//            @ApiParam("Experiment URI") @PathParam("uri") @NotNull @ValidURI URI uri,
//            @ApiParam("Variables URI list") @URL List<URI> variableUris
//    ) {
//        try {
//            ExperimentDAO dao = new ExperimentDAO(sparql);
//            dao.updateWithVariableList(uri, variableUris);
//            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
//        } catch (Exception e) {
//            return new ErrorResponse(e).getResponse();
//        }
//    }

//    @PUT
//    @Path("{uri}/sensors")
//    @ApiOperation("Update an experiment with a list of sensors")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response updateWithSensorsList(
//            @ApiParam("Experiment URI") @PathParam("uri") @NotNull @ValidURI URI uri,
//            @ApiParam("Sensors URI list") @URL List<URI> sensorsUris
//    ) {
//        try {
//            ExperimentDAO dao = new ExperimentDAO(sparql);
//            dao.updateWithSensorList(uri, sensorsUris);
//            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
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
                return new SingleObjectResponse<>(ExperimentGetDTO.fromModel(model)).getResponse();
            } else {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        "Experiment not found",
                        "Unknown Experiment URI: " + uri.toString()
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Search experiments
     *
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
            @ApiResponse(code = 204, message = "No experiment found", response = ExperimentGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchExperiments(
            @ApiParam(value = "Search by uri") @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by start date", example = "2017-06-15") @QueryParam("startDate") @DateConstraint String startDate,
            @ApiParam(value = "Search by end date", example = "2017-06-15") @QueryParam("endDate") @DateConstraint String endDate,
            @ApiParam(value = "Search by campaign") @QueryParam("campaign") Integer campaign,
            @ApiParam(value = "Search by label") @QueryParam("label") String label,
            @ApiParam(value = "Search by keywords") @QueryParam("keywords") List<String> keywords,
            @ApiParam(value = "Search by related project uri") @QueryParam("projects") List<URI> projects,
            @ApiParam(value = "Search by involved species") @QueryParam("species") List<URI> species,

            @ApiParam(value = "search ended(false) or active projects(true") @QueryParam("isArchived") Boolean isEnded,

            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) {

        try {
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ExperimentSearchDTO searchDTO = new ExperimentSearchDTO().setEnded(isEnded);

            searchDTO.setUri(uri)
                    .setCampaign(campaign)
                    .setLabel(label)
                    .setKeywords(keywords)
                    .setProjects(projects)
                    .setSpecies(species)
                    .setStartDate(startDate != null ? LocalDate.parse(startDate) : null)
                    .setEndDate(endDate != null ? LocalDate.parse(endDate) : null);

            ListWithPagination<ExperimentModel> resultList = xpDao.search(searchDTO, orderByList, page, pageSize);
            if (resultList.getList().isEmpty()) {
                return new ErrorResponse(Response.Status.NOT_FOUND, "No experiment found", "").getResponse();
            }
            // Convert paginated list to DTO
            ListWithPagination<ExperimentGetDTO> resultDTOList = resultList.convert(ExperimentGetDTO.class, ExperimentGetDTO::fromModel);
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
            return new ObjectUriResponse(uri).getResponse();

//        } catch (SPARQLUnknownUriException e) {
//            return new ErrorResponse(Response.Status.BAD_REQUEST, "", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}