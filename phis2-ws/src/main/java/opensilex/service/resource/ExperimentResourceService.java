//******************************************************************************
//                          ExperimentResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DateFormats;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.ExperimentSQLDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.experiment.ExperimentDTO;
import opensilex.service.resource.dto.experiment.ExperimentPostDTO;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Experiment;

/**
 * Experiment resource service.
 * @update [Morgane Vidal] 31 Oct. 2017: refactor trial to experiment
 * @update [Morgane Vidal] 20 Dec. 2018: add PUT services:
 *                          - experiment/{uri}/variables 
 *                          - experiment/{uri}/sensors
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/experiments")
@Path("experiments")
public class ExperimentResourceService extends ResourceService {
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
            @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("uri") @URL String uri,
            @ApiParam(value = "Search by project uri", example = DocumentationAnnotation.EXAMPLE_PROJECT_URI) @QueryParam("projectUri") @URL String projectUri,
            @ApiParam(value = "Search by start date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_START_DATE) @QueryParam("startDate") @Date(DateFormat.YMD) String startDate,
            @ApiParam(value = "Search by end date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_END_DATE) @QueryParam("endDate") @Date(DateFormat.YMD) String endDate,
            @ApiParam(value = "Search by field", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_FIELD) @QueryParam("field") String field,
            @ApiParam(value = "Search by campaign", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CAMPAIGN) @QueryParam("campaign") @Pattern(regexp = DateFormats.YEAR_REGEX, message = "This is not a valid year. Excepted format : YYYY (e.g. 2017)") String campaign,
            @ApiParam(value = "Search by place", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_PLACE) @QueryParam("place") String place,
            @ApiParam(value = "Search by alias", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS) @QueryParam("alias") String alias,
            @ApiParam(value = "Search by keywords", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_KEYWORDS) @QueryParam("keywords") String keywords) {

        ExperimentSQLDAO experimentDao = new ExperimentSQLDAO();

        if (uri != null) {
            experimentDao.uri = uri;
        }
        if (projectUri != null) {
            experimentDao.projectUri = projectUri;
        }
        if (startDate != null) {
            experimentDao.startDate = startDate;
        }
        if (endDate != null) {
            experimentDao.endDate = endDate;
        }
        if (field != null) {
            experimentDao.field = field;
        }
        if (campaign != null) {
            experimentDao.campaign = campaign;
        }
        if (place != null) {
            experimentDao.place = place;
        }
        if (alias != null) {
            experimentDao.alias = alias;
        }
        if (keywords != null) {
            experimentDao.keyword = keywords;
        }

        experimentDao.user = userSession.getUser();
        experimentDao.setPage(page);
        experimentDao.setPageSize(limit);

        return getExperimentsData(experimentDao);
    }

    /**
     * Get an experiment.
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
        @ApiResponse(code = 200, message = "Retrieve an experiment.", response = Experiment.class, responseContainer = "List"),
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
            @ApiParam(
                    value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION, 
                    example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI, 
                    required = true) 
                @PathParam("experiment") 
                @URL @Required String experimentURI,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) 
                @QueryParam("pageSize") 
                @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) 
                @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) 
                @QueryParam("page") 
                @DefaultValue(DefaultBrapiPaginationValues.PAGE) 
                @Min(0) int page) {
        if (experimentURI == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty Experiment URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }

        ExperimentSQLDAO experimentDao = new ExperimentSQLDAO(experimentURI);
        experimentDao.setPageSize(limit);
        experimentDao.setPage(page);
        experimentDao.user = userSession.getUser();

        return getExperimentsData(experimentDao);
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
        AbstractResultForm postResponse = null;

        // If there is at least an experiment in the data sent      
        if (experiments != null
                && !experiments.isEmpty()) {
            try {
                ExperimentSQLDAO experimentDao = new ExperimentSQLDAO();
                if (experimentDao.remoteUserAdress != null) {
                    experimentDao.remoteUserAdress = context.getRemoteAddr();
                }

                experimentDao.user = userSession.getUser();

                // Check and insert the experiments
                POSTResultsReturn result = experimentDao.checkAndInsertExperimentsList(experiments);

                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    //Code 201, experiments created
                    postResponse = new ResponseFormPOST(result.statusList);
                    postResponse.getMetadata().setDatafiles(result.getCreatedResources());
                } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || result.getHttpStatus().equals(Response.Status.OK)
                        || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                }
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty experiment(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Experiment update service.
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
        AbstractResultForm postResponse = null;

        if (experiments != null && !experiments.isEmpty()) {
            ExperimentSQLDAO experimentDao = new ExperimentSQLDAO();
            if (experimentDao.remoteUserAdress != null) {
                experimentDao.remoteUserAdress = context.getRemoteAddr();
            }
            experimentDao.user = userSession.getUser();

            // Check and update the experiments
            POSTResultsReturn result = experimentDao.checkAndUpdateList(experiments);

            if (result.getHttpStatus().equals(Response.Status.OK)) { //200: experiments updated
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty experiment(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Updates the variables linked to an experiment.
     * @example
     * [
     *   "http://www.opensilex.fr/platform/id/variables/v001",
     *   "http://www.opensilex.fr/platform/id/variables/v003"
     * ]
     * @param variables
     * @param uri
     * @param context
     * @return the result
     * @example 
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [
     *              {
     *                  "message": "Resources updated",
     *                  "exception": {
     *                      "type": "Info",
     *                      "href": null,
     *                      "details": "The experiment http://www.opensilex.fr/platform/OSL2015-1 has now 2 linked variables"
     *                  }
     *              }
     *          ],
     *          "datafiles": [
     *              "http://www.opensilex.fr/platform/OSL2015-1"
     *          ]
     *      }
     * }
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
            @ApiParam(value = DocumentationAnnotation.LINK_VARIABLES_DEFINITION) @URL ArrayList<String> variables,
            @ApiParam(
                    value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION, 
                    example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI, 
                    required = true) 
                @PathParam("uri") @Required @URL String uri,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
        if (context.getRemoteAddr() != null) {
            experimentDAO.remoteUserAdress = context.getRemoteAddr();
        }
        
        experimentDAO.user = userSession.getUser();
        
        POSTResultsReturn result = experimentDAO.checkAndUpdateLinkedVariables(uri, variables);
        
        if (result.getHttpStatus().equals(Response.Status.CREATED)) {
            postResponse = new ResponseFormPOST(result.statusList);
            postResponse.getMetadata().setDatafiles(result.getCreatedResources());
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            postResponse = new ResponseFormPOST(result.statusList);
        }
        
        return Response.status(result.getHttpStatus()).entity(postResponse).build();
    }
    
    /**
     * Updates the sensors linked to an experiment.
     * @example
     * [
     *      "http://www.phenome-fppn.fr/opensilex/2018/s18001"
     * ]
     * @param sensors
     * @param uri
     * @param context
     * @return the query result
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [
     *              {
     *                  "message": "Resources updated",
     *                  "exception": {
     *                      "type": "Info",
     *                      "href": null,
     *                      "details": "The experiment http://www.opensilex.fr/platform/OSL2018-1 has now 1 linked sensors"
     *                  }
     *              }
     *          ],
     *          "datafiles": [
     *              "http://www.opensilex.fr/platform/OSL2015-1"
     *          ]
     *      }
     * }
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
            @PathParam("uri") 
            @Required 
            @URL String uri,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
        if (context.getRemoteAddr() != null) {
            experimentDAO.remoteUserAdress = context.getRemoteAddr();
        }
        
        experimentDAO.user = userSession.getUser();
        
        POSTResultsReturn result = experimentDAO.checkAndUpdateLinkedSensors(uri, sensors);
        
        if (result.getHttpStatus().equals(Response.Status.CREATED)) {
            postResponse = new ResponseFormPOST(result.statusList);
            postResponse.getMetadata().setDatafiles(result.getCreatedResources());
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            postResponse = new ResponseFormPOST(result.statusList);
        }
        
        return Response.status(result.getHttpStatus()).entity(postResponse).build();
    }

    /**
     * Gets experiment data.
     * @param experimentSQLDao
     * @return experiments found
     */
    private Response getExperimentsData(ExperimentSQLDAO experimentSQLDao) {
        ArrayList<Experiment> experiments = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Experiment> getResponse;
        Integer experimentsCount = experimentSQLDao.count();

        if (experimentsCount != null && experimentsCount == 0) {
            getResponse = new ResultForm<>(experimentSQLDao.getPageSize(), experimentSQLDao.getPage(), experiments, true, experimentsCount);
            return noResultFound(getResponse, statusList);
        } else {
            experiments = experimentSQLDao.allPaginate();
            
            if (experiments == null || experimentsCount == null) { //sql error
                getResponse = new ResultForm<>(0, 0, experiments, true, experimentsCount);
                return sqlError(getResponse, statusList);
            } else if (experiments.isEmpty()) { // no result found
                getResponse = new ResultForm<>(experimentSQLDao.getPageSize(), experimentSQLDao.getPage(), experiments, false, experimentsCount);
                return noResultFound(getResponse, statusList);
            } else { //results founded
                getResponse = new ResultForm<>(experimentSQLDao.getPageSize(), experimentSQLDao.getPage(), experiments, true, experimentsCount);
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        }
    }
}
