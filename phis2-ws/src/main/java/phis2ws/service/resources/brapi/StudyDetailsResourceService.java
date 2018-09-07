//******************************************************************************
//                                       StudyDetailsResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.brapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormStudyDetails;
import phis2ws.service.view.model.phis.Call;
import phis2ws.service.view.model.phis.StudyDetails;

@Api("/brapi/v1/studies")
@Path("/brapi/v1/studies")

/**
 * StudyDetails service
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyDetailsResourceService implements BrapiCall{
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudyDetailsResourceService.class);  
    
     /**
     * Overriding BrapiCall method
     * @date 27 Aug 2018
     * @return Calls call information
     */
    @Override
    public Call callInfo() {
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.2");
        Call callscall = new Call("studies/{studyDbId}", calldatatypes, callMethods, callVersions);
        return callscall;
    }
    
    //User session
    @SessionInject
    Session userSession;
    
    @GET
    @Path("{studyDbId}")
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
        @ApiParam(value = "Search by studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId
        ) throws SQLException {               
        
        StudyDAO studyDAO = new StudyDAO();
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }      
        
        studyDAO.limit=1;
        studyDAO.user = userSession.getUser();
        
        return getStudyData(studyDAO);
        }
     
    private Response noResultFound(ResponseFormStudyDetails getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "This study doesn't exist"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    private Response sqlError(ResponseFormStudyDetails getResponse, ArrayList<Status> insertStatusList) {
         insertStatusList.add(new Status("SQL error" ,StatusCodeMsg.ERR, "can't fetch result"));
         getResponse.setStatus(insertStatusList);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }
     
     /**
     * Collect data from a user query
     * @param studyDao StudyDAO
     * @return Response for the user: contains study corresponding to the query 
     */
    private Response getStudyData(StudyDAO studyDAO) throws SQLException{
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormStudyDetails getResponse;    
        StudyDetails study = studyDAO.getStudyInfo();
        if (study.getStudyDbId() == null) {
            getResponse = new ResponseFormStudyDetails(0, 0, study, true);
            return noResultFound(getResponse, statusList);
        } else {
            getResponse = new ResponseFormStudyDetails(0, 0, study, true);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }        
    }
}
