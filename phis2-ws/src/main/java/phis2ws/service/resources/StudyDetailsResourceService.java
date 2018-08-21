//******************************************************************************
//                                       StudyDetailsResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.SQLException;
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
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.StudyDAO;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormStudyDetails;
import phis2ws.service.view.model.phis.StudyDetails;

/**
 *
 * @author boizetal
 */

@Api("/brapi/v1/studies")
@Path("/brapi/v1/studies")
public class StudyDetailsResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(StudiesSearchResourceService.class);  
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
    
    @GET
    @ApiOperation(value = "Retrieve study details", notes = "Retrieve study details")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve study details", response = StudyDetails.class, responseContainer = "List"),
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
    
     public Response getStudyDetails (
        @ApiParam(value = "Search by studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @QueryParam("studyDbId") String studyDbId
        ) throws SQLException {
               
        
        //a modifier
        StudyDAO studyDAO = new StudyDAO();
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }      
        
        studyDAO.limit=1;
        

        studyDAO.user = userSession.getUser();
        
        return getStudyData(studyDAO);
        }
     
    private Response noResultFound(ResponseFormStudyDetails getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the experiments"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    private Response sqlError(ResponseFormStudyDetails getResponse, ArrayList<Status> insertStatusList) {
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
    private Response getStudyData(StudyDAO studyDAO) throws SQLException{
        ArrayList<StudyDetails> studiesList = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormStudyDetails getResponse;
        Integer studiesCount = studyDAO.count();
        
        if (studiesCount != null && studiesCount == 0) {
            getResponse = new ResponseFormStudyDetails(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, true);
            return noResultFound(getResponse, statusList);
        } else {
            studiesList = studyDAO.getStudyInfo();
            if (studiesList == null) {
              getResponse = new ResponseFormStudyDetails(0, 0, studiesList, true);
              return sqlError(getResponse, statusList);
            } else if (!studiesList.isEmpty() && studiesCount != null) {
                getResponse = new ResponseFormStudyDetails(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, false);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResponseFormStudyDetails(0, 0, studiesList, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
