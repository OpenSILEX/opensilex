//******************************************************************************
//                                       StudiesSearchResourceService.java
//
// Author(s): boizetal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 30 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  30 juil. 2018
// Subject:
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.phis.StudyDAO;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormExperiment;
import phis2ws.service.view.brapi.form.ResponseFormStudy;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.StudiesSearch;
//import phis2ws.service.view.model.phis.Call;

/**
 *
 * @author boizetal
 */


@Api("/brapi/v1/studies-search")
@Path("/brapi/v1/studies-search")
public class StudiesSearchResourceService {   
    final static Logger LOGGER = LoggerFactory.getLogger(StudiesSearchResourceService.class);  
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
    
    @GET
    @ApiOperation(value = "Check the available brapi calls",
                       notes = "Check the available brapi calls")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve brapi calls", response = Experiment.class, responseContainer = "List"),
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
    
    
    
    public Response getStudies (
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        @ApiParam(value = "Search by studyDbId", example = "") @QueryParam("studyDbId") String studyDbId,
        @ApiParam(value = "Search by trialDbId", example = "") @QueryParam("trialDbId") String trialDbId,
        @ApiParam(value = "Search by program", example = "") @QueryParam("programDbId") String programDbId,
        @ApiParam(value = "Search by crop name from the Program", example = "") @QueryParam("commonCropName") String commonCropName,
        @ApiParam(value = "Search by location", example = "") @QueryParam("locationDbId") String locationDbId,
        //@ApiParam(value = "Search by field", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_FIELD) @QueryParam("field") String field,
        @ApiParam(value = "Search by year", example = "") @QueryParam("seasonDbId") String seasonDbId,
        @ApiParam(value = "Search by studyType", example = "") @QueryParam("studyType") String studyType,  
        @ApiParam(value = "Search by germplasmDbIds", example = "") @QueryParam("germplasmDbIds") List<String> germplasmDbIds,   
//        @ApiParam(value = "Search by observationVariableDbIds", example = "") @QueryParam("observationVariableDbIds") ArrayList<String> observationVariableDbIds,   
        @ApiParam(value = "Search by active", example = "") @QueryParam("active") boolean active,
        @ApiParam(value = "sort by", example = "studyDbId") @QueryParam("sortBy") String sortBy,
        @ApiParam(value = "sort order", example = "asc") @QueryParam("sortOrder") String sortOrder
        ){
               
        
        //a modifier
        StudyDAO studyDAO = new StudyDAO();
        
        if (studyDbId != null) {
            studyDAO.studyType = studyType;
        }      
        
        if (trialDbId != null) {
            studyDAO.trialDbId = trialDbId;
        }
        
        if (programDbId != null) {
            studyDAO.programDbId = programDbId;
        }
        
        if (commonCropName != null) {
            studyDAO.commonCropName = commonCropName;
        }
        
        if (locationDbId != null) {
            studyDAO.locationDbId = locationDbId;
        }
        
        if (seasonDbId != null) {
            studyDAO.seasonDbId = seasonDbId;
        }
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }
        
        if (germplasmDbIds != null) {
            studyDAO.germplasmDbIds = germplasmDbIds;
        }
//        
//        if (observationVariableDbIds != null) {
//            studyDAO.observationVariableDbIds = observationVariableDbIds;
//        }
     
        if (sortBy != null) {
            studyDAO.sortBy = sortBy;
        }

        if (sortOrder != null) {
            studyDAO.sortOrder = sortOrder;
        }
      
        studyDAO.setPageSize(limit);
        studyDAO.setPage(page);
        studyDAO.user = userSession.getUser();
        
        return getStudiesData(studyDAO);
        }  

    
    private Response noResultFound(ResponseFormStudy getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the experiments"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    private Response sqlError(ResponseFormStudy getResponse, ArrayList<Status> insertStatusList) {
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
    private Response getStudiesData(StudyDAO studyDAO) {
        ArrayList<StudiesSearch> studiesList = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormStudy getResponse;
        Integer studiesCount = studyDAO.count();
        
        if (studiesCount != null && studiesCount == 0) {
            getResponse = new ResponseFormStudy(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, true);
            return noResultFound(getResponse, statusList);
        } else {
            studiesList = studyDAO.allPaginate();
            if (studiesList == null) {
              getResponse = new ResponseFormStudy(0, 0, studiesList, true);
              return sqlError(getResponse, statusList);
            } else if (!studiesList.isEmpty() && studiesCount != null) {
                getResponse = new ResponseFormStudy(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, false);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResponseFormStudy(0, 0, studiesList, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
