//**********************************************************************************************
//                                       ExperimentResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 31 2017 : Passage de trial à experiment
// Subject: Represents the experiment data service
//***********************************************************************************************

package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
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
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.ExperimentDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.brapi.form.ResponseFormExperiment;
import phis2ws.service.view.model.phis.Experiment;

@Api("/experiments")
@Path("experiments")
public class ExperimentResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentResourceService.class);
    
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
    
    
    /**
     * @param uri
     * @param limit
     * @param page
     * @param startDate
     * @param endDate
     * @param field
     * @param campaign
     * @param place
     * @param alias
     * @param keywords
     * @return liste des expérimentations correspondant aux différents critères de recherche 
     *                                                              (ou toutes les expérimentations si pas de critères)
     *         Le retour (dans "data") est de la forme : 
     *          [
     *              { description de l'expérimentation1 },
     *              { description de l'expérimentation2 },
     *          ]
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
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        @ApiParam(value = "Search by uri", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("uri") String uri,
        @ApiParam(value = "Search by start date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_START_DATE) @QueryParam("startDate") String startDate,
        @ApiParam(value = "Search by end date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_END_DATE) @QueryParam("endDate") String endDate,
        @ApiParam(value = "Search by field", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_FIELD) @QueryParam("field") String field,
        @ApiParam(value = "Search by campaign", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CAMPAIGN) @QueryParam("campaign") String campaign,
        @ApiParam(value = "Search by place", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_PLACE) @QueryParam("place") String place,
        @ApiParam(value = "Search by alias", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS) @QueryParam("alias") String alias,
        @ApiParam(value = "Search by keywords", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_KEYWORDS) @QueryParam("keywords") String keywords) {
        
        ExperimentDao experimentDao = new ExperimentDao();
        
        if (uri != null) {
            experimentDao.uri = uri;
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
     * 
     * @param experimentURI
     * @param limit
     * @param page
     * @return l'expérimentation correspondant à l'uri donnée si elle existe
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
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @PathParam("experiment") String experimentURI, 
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page) {
        if (experimentURI == null) {
            final Status status = new Status("Access error", StatusCodeMsg.ERR, "Empty Experiment URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        ExperimentDao experimentDao = new ExperimentDao(experimentURI);
        experimentDao.setPageSize(limit);
        experimentDao.setPage(page);
        experimentDao.user = userSession.getUser();
        
        return getExperimentsData(experimentDao);
    }    
    
    /**
     * 
     * @param experiments
     * @param context
     * @return le résultat de la requête de création de l'expérimentation
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
            @ApiParam(value = DocumentationAnnotation.EXPERIMENT_POST_DATA_DEFINITION) ArrayList<ExperimentDTO> experiments,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        //Si dans les données envoyées il y a au moins une expérimentation      
        
        if (experiments != null 
                && !experiments.isEmpty()) {
            try {
                ExperimentDao experimentDao = new ExperimentDao();
                if (experimentDao.remoteUserAdress != null) {
                    experimentDao.remoteUserAdress = context.getRemoteAddr();
                } 
                
                experimentDao.user = userSession.getUser();
                
                //Vérification des expérimentations et insertion dans la BD
                POSTResultsReturn result = experimentDao.checkAndInsertList(experiments);
                
                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    //Code 201, experiments insérés
                    postResponse = new ResponseFormPOST(result.statusList);
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
    @ApiParam(value = DocumentationAnnotation.EXPERIMENT_POST_DATA_DEFINITION) ArrayList<ExperimentDTO> experiments,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (experiments != null && !experiments.isEmpty()) {
            ExperimentDao experimentDao = new ExperimentDao();
            if (experimentDao.remoteUserAdress != null) {
                experimentDao.remoteUserAdress = context.getRemoteAddr();
            }
            experimentDao.user = userSession.getUser();
            
            //Vérification des données et update de la BD
            POSTResultsReturn result = experimentDao.checkAndUpdateList(experiments);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200 users modifiés
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
    
    private Response noResultFound(ResponseFormExperiment getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the experiments"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    private Response sqlError(ResponseFormExperiment getResponse, ArrayList<Status> insertStatusList) {
         insertStatusList.add(new Status("SQL error" ,StatusCodeMsg.ERR, "can't fetch result"));
         getResponse.setStatus(insertStatusList);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }
    
    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche d'expérimentations)
     * @param experimentDao ExperimentDao
     * @return la réponse pour l'utilisateur. 
     *          Contient la liste des expérimentations correspondant à la recherche
     */
    private Response getExperimentsData(ExperimentDao experimentDao) {
        ArrayList<Experiment> experiments = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormExperiment getResponse;
        Integer experimentsCount = experimentDao.count();
        
        if (experimentsCount != null && experimentsCount == 0) {
            getResponse = new ResponseFormExperiment(experimentDao.getPageSize(), experimentDao.getPage(), experiments, true);
            return noResultFound(getResponse, statusList);
        } else {
            experiments = experimentDao.allPaginate();
            if (experiments == null) {
              getResponse = new ResponseFormExperiment(0, 0, experiments, true);
              return sqlError(getResponse, statusList);
            } else if (!experiments.isEmpty() && experimentsCount != null) {
                getResponse = new ResponseFormExperiment(experimentDao.getPageSize(), experimentDao.getPage(), experiments, false);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResponseFormExperiment(0, 0, experiments, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
