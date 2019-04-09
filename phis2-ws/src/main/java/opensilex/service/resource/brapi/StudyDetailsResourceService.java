//******************************************************************************
//                          StudyDetailsResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 Aug. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

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
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.authentication.Session;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.StudyDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionInject;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.view.brapi.form.BrapiSingleResponseForm;
import opensilex.service.model.Call;
import opensilex.service.model.StudyDetails;

@Api("/brapi/v1/studies")
@Path("/brapi/v1/studies")

/**
 * Study details resource service.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyDetailsResourceService implements BrapiCall{
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudyDetailsResourceService.class);  
    
     /**
     * Overriding BrapiCall method.
     * @return Calls call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call = new Call("studies/{studyDbId}", calldatatypes, callMethods, callVersions);
        calls.add(call);
        return calls;
    }
    
    //User session
    @SessionInject
    Session userSession;
    
    /**
     * Study details GET service.
     * @param studyDbId
     * @return the study details found
     * @throws SQLException 
     */
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
    
    /**
     * Get study details.
     */
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
     
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "This study doesn't exist"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
     
     /**
     * Collects data from a user query.
     * @param studyDao StudyDAO
     * @return Response for the user: contains study corresponding to the query 
     */
    private Response getStudyData(StudyDAO studyDAO) throws SQLException{
        ArrayList<Status> statusList = new ArrayList<>(); 
        StudyDetails study = studyDAO.getStudyInfo();
        if (study.getStudyDbId() == null) {
            //quick fix to manage the case where the studyDbId doesn't exist in the base
            ArrayList<StudyDetails> nostudy= new ArrayList();
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, nostudy, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiSingleResponseForm getSingleResponse = new BrapiSingleResponseForm(study);
            return Response.status(Response.Status.OK).entity(getSingleResponse).build();
        }        
    }
}
