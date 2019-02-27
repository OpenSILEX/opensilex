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
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.mongo.DatasetDAOMongo;
import phis2ws.service.dao.phis.StudyDAO;
import phis2ws.service.dao.sesame.ScientificObjectDAOSesame;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.phenotype.BrapiObservationDTO;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.BrapiMultiResponseForm;
import phis2ws.service.view.brapi.form.BrapiSingleResponseForm;
import phis2ws.service.view.model.phis.Call;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.view.model.phis.Dataset;
import phis2ws.service.view.model.phis.ScientificObject;
import phis2ws.service.view.model.phis.StudyDetails;
import phis2ws.service.view.model.phis.Variable;

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

    @GET
    @Path("{studyDbId}/observations")
    @ApiOperation(value = "Get the observations associated to a specific study", notes = "Get the observations associated to a specific study")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = StudyDetails.class, responseContainer = "List"),
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
    
    public Response getObservations (
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId
        ) throws SQLException {               
        
        StudyDAO studyDAO = new StudyDAO();
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }      
        
        studyDAO.limit=1;
        studyDAO.user = userSession.getUser();
        
        return getStudyObservations(studyDAO);
    }
    
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "This study doesn't exist"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    private Response sqlError(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
         insertStatusList.add(new Status("SQL error" ,StatusCodeMsg.ERR, "can't fetch result"));
         getResponse.getMetadata().setStatus(insertStatusList);
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }
     
     /**
     * Collect data from a user query
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
    
    private Response getStudyVariables(StudyDAO studyDAO) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Response getStudyObservations(StudyDAO studyDAO) {
        ArrayList<Status> statusList = new ArrayList<>(); 
        DatasetDAOMongo datasetDAOMongo = new DatasetDAOMongo();
        
        datasetDAOMongo.experiment = studyDAO.studyDbId;
        ArrayList<Dataset> datasets = new ArrayList<>();
        datasets = datasetDAOMongo.allPaginate();
        ArrayList<BrapiObservationDTO> observations = new ArrayList();
        for (Dataset dataset:datasets){
            ArrayList<Data> dataList = dataset.getData();
            for (Data data:dataList){
                BrapiObservationDTO observation= new BrapiObservationDTO(dataset);
                VariableDaoSesame variableDAO = new VariableDaoSesame();
                variableDAO.uri = data.getVariable();
                ArrayList<Variable> variables = variableDAO.allPaginate();
                observation.setObservationVariableDbId(data.getVariable());
                observation.setObservationVariableName(variables.get(0).getLabel());
                observation.setObservationTimeStamp(data.getDate());
                observation.setValue(data.getValue());
                ScientificObjectDAOSesame scientificObjectDAO = new ScientificObjectDAOSesame();
                scientificObjectDAO.uri = data.getAgronomicalObject();
                ArrayList<ScientificObject> scientificObjects = scientificObjectDAO.allPaginate();
                observation.setObservationUnitDbId(data.getAgronomicalObject());
                observation.setObservationUnitName(scientificObjects.get(0).getAlias());
                observation.setObservationLevel(scientificObjects.get(0).getRdfType());
                observations.add(observation);
            }
        }      
        
        if (observations.isEmpty()) {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, observations, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(datasetDAOMongo.getPageSize(), datasetDAOMongo.getPage(), observations, false);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
        
    }
}
