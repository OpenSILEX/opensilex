/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.net.URI;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;

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
    ) throws Exception {
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
        }
    }
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @ApiParam(value = "Experiment URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
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
    }
}
