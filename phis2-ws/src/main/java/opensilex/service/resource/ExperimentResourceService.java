//******************************************************************************
//                          ExperimentResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.*;
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DateFormats;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.SensorDAO;
import opensilex.service.dao.SpeciesDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Experiment;
import opensilex.service.model.Variable;
import opensilex.service.resource.dto.experiment.*;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchDTO;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import opensilex.service.result.ResultForm;
import opensilex.service.view.brapi.Status;

/**
 * Experiment resource service.
 *
 * @update [Morgane Vidal] 31 Oct. 2017: refactor trial to experiment
 * @update [Morgane Vidal] 20 Dec. 2018: add PUT services: -
 * experiment/{uri}/variables - experiment/{uri}/sensors
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/experiments")
@Path("experiments")
public class ExperimentResourceService extends ResourceService {

    @Inject
    private SPARQLService sparql;

    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentResourceService.class);

    /**
     * @param uri
     * @param limit
     * @param page
     * @param projectUri
     * @param startDate
     * @param endDate
     * @param field
     * @param campaign
     * @param place
     * @param alias
     * @param keywords
     * @return found experiments
     */
    @GET
    @ApiOperation(value = "Get all experiments corresponding to the searched params given",
            notes = "Retrieve all experiments authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all experiments", response = Experiment.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExperimentsBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by project uri", example = DocumentationAnnotation.EXAMPLE_PROJECT_URI) @QueryParam("projectUri") URI projectUri,
            @ApiParam(value = "Search by start date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_START_DATE) @QueryParam("startDate") @Date(DateFormat.YMD) String startDate,
            @ApiParam(value = "Search by end date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_END_DATE) @QueryParam("endDate") @Date(DateFormat.YMD) String endDate,
            @ApiParam(value = "Search by field", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_FIELD) @QueryParam("field") String field,
            @ApiParam(value = "Search by campaign", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CAMPAIGN) @QueryParam("campaign") @Pattern(regexp = DateFormats.YEAR_REGEX, message = "This is not a valid year. Excepted format : YYYY (e.g. 2017)") String campaign,
            @ApiParam(value = "Search by place", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_PLACE) @QueryParam("place") String place,
            @ApiParam(value = "Search by alias", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS) @QueryParam("alias") String alias,
            @ApiParam(value = "Search by keywords", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_KEYWORDS) @QueryParam("keywords") String keywords) {

        try {

            // create a new search DTO from  the old xp search attributes
            ExperimentSearchDTO searchDTO = new ExperimentSearchDTO();
            searchDTO.setUri(uri)
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .setLabel(alias);

            if (!StringUtils.isEmpty(campaign)) {
                searchDTO.setCampaign(Integer.parseInt(campaign));
            }
            if (!StringUtils.isEmpty(keywords)) {
                searchDTO.setKeywords(Collections.singletonList(keywords));
            }
            if (projectUri != null) {
                searchDTO.setProjects(Collections.singletonList(projectUri));
            }

            // use the new DAO in order to get paginated model List
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ListWithPagination<ExperimentModel> resultList = xpDao.search(searchDTO, Collections.emptyList(), page, limit);
//            if (resultList.getList().isEmpty()) {
//                return new ErrorResponse(Response.Status.NOT_FOUND, "No experiment found", "").getResponse();
//            }

            // convert model list to dto list
            ExperimentModelToExperiment modelToExperiment = new ExperimentModelToExperiment();
            ArrayList<Experiment> xps = new ArrayList<>(resultList.getList().size());
            for (ExperimentModel xpModel : resultList.getList()) {
                xps.add(modelToExperiment.convert(xpModel));
            }

            // return paginated response
            ArrayList<Status> statusList = new ArrayList<>();
            ResultForm<Experiment> getResponse;
            if (xps.isEmpty()) { //Request failure || No result found
                getResponse = new ResultForm<Experiment>(0, 0, xps, true);
                return noResultFound(getResponse, statusList);
            } else { //Results

                getResponse = new ResultForm<>(resultList.getPageSize(), resultList.getPage(), xps, true, resultList.getTotal());
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }

        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Get an experiment.
     *
     * @param experimentURI
     * @param limit
     * @param page
     * @return the experiment corresponding to the URI given
     */
    @GET
    @Path("{experiment}")
    @ApiOperation(value = "Get an experiment",
            notes = "Retrieve an experiment. Need URL encoded experiment URI (Unique resource identifier).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve an experiment.", response = Experiment.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExperimentDetail(
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI, required = true) @PathParam("experiment") URI experimentURI,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {

        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel xpModel = dao.get(experimentURI);

            ArrayList<Status> statusList = new ArrayList<>();
            ResultForm<Experiment> getResponse;

            if (xpModel != null) {

                ExperimentModelToExperiment modelToExperiment = new ExperimentModelToExperiment();

                ArrayList<Experiment> xps = new ArrayList<>();
                xps.add(modelToExperiment.convert(xpModel));

                getResponse = new ResultForm<>(20, 1, xps, true, 1);
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            } else {
                ArrayList<Experiment> xps = new ArrayList<>();
                getResponse = new ResultForm<Experiment>(0, 0, xps, true);
                return noResultFound(getResponse, statusList);
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param experiments
     * @param context
     * @return result of the experiment creation request
     */
    @POST
    @ApiOperation(value = "Post a experiment",
            notes = "Register a new experiment in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Experiment saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postExperiment(
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_POST_DATA_DEFINITION) @Valid ArrayList<ExperimentPostDTO> experiments,
            @Context HttpServletRequest context) {

        if (experiments == null || experiments.isEmpty()) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "No experiments provided", "").getResponse();
        }

        try {
            AuthenticationService authentication = OpenSilex.getInstance().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            UserDAO userDAO = new UserDAO(sparql, authentication);
            ProjectDAO projectDAO = new ProjectDAO(sparql);
            SpeciesDAO speciesDAO = new SpeciesDAO();

            // use DAO(s) in order to validate URI(s) from ExperimentPostDTO
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            PostDtoToExperimentModel postDtoToNewModel = new PostDtoToExperimentModel(speciesDAO, projectDAO, userDAO);

            List<URI> createdXpUris = new ArrayList<>(experiments.size());
            for (ExperimentPostDTO xpDto : experiments) {
                ExperimentModel model = postDtoToNewModel.convert(xpDto);
                xpDao.create(model);
                createdXpUris.add(model.getUri());
            }

            // convert model list to dto list
            // return paginated response
            ArrayList<Status> statusList = new ArrayList<>();
            ResultForm<URI> getResponse;
            if (createdXpUris.isEmpty()) { //Request failure || No result found
                getResponse = new ResultForm<URI>(0, 0, new ArrayList<>(createdXpUris), true);
                return noResultFound(getResponse, statusList);
            } else { //Results

                getResponse = new ResultForm<>(0, 0, new ArrayList<>(createdXpUris), true, 0);
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } catch (IllegalArgumentException | URISyntaxException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Bad request", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Experiment update service.
     *
     * @param experiments
     * @param context
     * @return the update result
     */
    @PUT
    @ApiOperation(value = "Update experiment")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Experiment not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putExperiment(
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_POST_DATA_DEFINITION) @Valid ArrayList<ExperimentDTO> experiments,
            @Context HttpServletRequest context) {

        if (experiments == null || experiments.isEmpty()) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "No experiments provided", "").getResponse();
        }

        try {
            AuthenticationService authentication = OpenSilex.getInstance().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            UserDAO userDAO = new UserDAO(sparql, authentication);
            ProjectDAO projectDAO = new ProjectDAO(sparql);
            SpeciesDAO speciesDAO = new SpeciesDAO();

            // use DAO(s) in order to validate URI(s) from ExperimentPostDTO
            ExperimentDtoToExperimentModel postDtoToNewModel = new ExperimentDtoToExperimentModel(speciesDAO, projectDAO, userDAO);

            List<ExperimentModel> xpModels = new ArrayList<>(experiments.size());
            for (ExperimentDTO xpDto : experiments) {
                xpModels.add(postDtoToNewModel.convert(xpDto));
            }

            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            for (ExperimentModel xpModel : xpModels) {
                xpDao.update(xpModel);
            }
            return new PaginatedListResponse<>(xpModels.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList())).getResponse();

        } catch (IllegalArgumentException | URISyntaxException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Bad request", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Updates the variables linked to an experiment.
     *
     * @param variableUris
     * @param experimentURI
     * @param context
     * @return the result
     * @example [ "http://www.opensilex.fr/platform/id/variables/v001",
     * "http://www.opensilex.fr/platform/id/variables/v003" ]
     * @example { "metadata": { "pagination": null, "status": [ { "message":
     * "Resources updated", "exception": { "type": "Info", "href": null,
     * "details": "The experiment http://www.opensilex.fr/platform/OSL2015-1 has
     * now 2 linked variables" } } ], "datafiles": [
     * "http://www.opensilex.fr/platform/OSL2015-1" ] } }
     */
    @PUT
    @Path("{uri}/variables")
    @ApiOperation(value = "Update the observed variables of an experiment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Measured observed variables of the experiment updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putVariables(
            @ApiParam(value = DocumentationAnnotation.LINK_VARIABLES_DEFINITION) @URL ArrayList<String> variableUris,
            @ApiParam(
                    value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION,
                    example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI,
                    required = true)
            @PathParam("uri") @Required URI experimentURI,
            @Context HttpServletRequest context) {
        try {
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ExperimentModel xpModel = xpDao.get(experimentURI);

            if (xpModel != null) {

                VariableDAO varDao = new VariableDAO();
                for (String variableUri : variableUris) {
                    URI varURI = new URI(variableUri);
                    Variable variable = varDao.findById(variableUri);
                    if (variable == null) {
                        return new ErrorResponse(Response.Status.NOT_FOUND, "Variable not found", "Unknown Variable URI: " + variableUri).getResponse();
                    }
                    xpModel.getVariables().add(varURI);
                }
                xpDao.update(xpModel);

                SensorDAO sensorDAO = new SensorDAO();
                ExperimentModelToExperiment modelToExperiment = new ExperimentModelToExperiment();

                Experiment xp = modelToExperiment.convert(xpModel);
                ArrayList<Experiment> xps = new ArrayList<>();
                xps.add(xp);
                
                ArrayList<Status> statusList = new ArrayList<>();
                ResultForm<Experiment> getResponse;
                if (xps.isEmpty()) { //Request failure || No result found
                    getResponse = new ResultForm<Experiment>(0, 0, new ArrayList<>(), true);
                    return noResultFound(getResponse, statusList);
                } else { //Results

                    getResponse = new ResultForm<>(0, 0, xps, true, 0);
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND, "Experiment not found", "Unknown Experiment URI: " + experimentURI
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Updates the sensors linked to an experiment.
     *
     * @param sensors
     * @param uri
     * @param context
     * @return the query result
     * @example [ "http://www.phenome-fppn.fr/opensilex/2018/s18001" ]
     * @example { "metadata": { "pagination": null, "status": [ { "message":
     * "Resources updated", "exception": { "type": "Info", "href": null,
     * "details": "The experiment http://www.opensilex.fr/platform/OSL2018-1 has
     * now 1 linked sensors" } } ], "datafiles": [
     * "http://www.opensilex.fr/platform/OSL2015-1" ] } }
     */
    @PUT
    @Path("{uri}/sensors")
    @ApiOperation(value = "Update the sensors which participates in an experiment")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "The list of sensors which participates in the experiment updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putSensors(
            @ApiParam(value = DocumentationAnnotation.LINK_SENSORS_DEFINITION) @URL ArrayList<String> sensors,
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI, required = true)
            @PathParam("uri") @Required URI uri,
            @Context HttpServletRequest context) {
        try {
            ExperimentDAO xpDao = new ExperimentDAO(sparql);
            ExperimentModel xpModel = xpDao.get(uri);

            if (xpModel != null) {

                for (String sensor : sensors) {
                    xpModel.getSensors().add(new URI(sensor));
                }
                xpDao.update(xpModel);

                SensorDAO sensorDAO = new SensorDAO();
                ExperimentModelToExperiment modelToExperiment = new ExperimentModelToExperiment();

                Experiment xp = modelToExperiment.convert(xpModel);
                ArrayList<Experiment> xps = new ArrayList<>();
                xps.add(xp);
                
                ArrayList<Status> statusList = new ArrayList<>();
                ResultForm<Experiment> getResponse;
                if (xps.isEmpty()) { //Request failure || No result found
                    getResponse = new ResultForm<Experiment>(0, 0, new ArrayList<>(), true);
                    return noResultFound(getResponse, statusList);
                } else { //Results

                    getResponse = new ResultForm<>(0, 0, xps, true, 0);
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                return new ErrorResponse(
                        Response.Status.NOT_FOUND, "Experiment not found", "Unknown Experiment URI: " + uri
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

}
