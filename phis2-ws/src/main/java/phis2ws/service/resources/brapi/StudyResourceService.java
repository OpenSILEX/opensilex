//******************************************************************************
//                                       StudyResourceService.java
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
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
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
import phis2ws.service.view.model.phis.BrapiVariable;
import phis2ws.service.view.model.phis.Call;
import phis2ws.service.view.model.phis.AgronomicalData;
import phis2ws.service.view.model.phis.Dataset;
import phis2ws.service.view.model.phis.ScientificObject;
import phis2ws.service.view.model.phis.StudyDetails;
import phis2ws.service.view.model.phis.Variable;

@Api("/brapi/v1/studies")
@Path("/brapi/v1/studies")

/**
 * Study services :
 * GET Studies/{studyDbId}
 * GET Studies/{studyDbId}/observations
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudyResourceService implements BrapiCall{
    
    final static Logger LOGGER = LoggerFactory.getLogger(StudyResourceService.class);  
    
     /**
     * Overriding BrapiCall method
     * @date 27 Aug 2018
     * @return Calls call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        
        //SILEX:info 
        //Call GET Study/{stuDbId}
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("studies/{studyDbId}", calldatatypes, callMethods, callVersions);
        Call call2 = new Call("studies/{studyDbId}/observations", calldatatypes, callMethods, callVersions);
        Call call3 = new Call("studies/{studyDbId}/observationVariables", calldatatypes, callMethods, callVersions);
        //\SILEX:info        
        calls.add(call1);
        calls.add(call2);
        calls.add(call3);
        
        return calls;
    }
    
    //User session
    @SessionInject
    Session userSession;
    
    /**
     * Retrieve one study information
     * @param studyDbId
     * @return the study information
     * @example
     *  {
         "metadata": {
           "pagination": {
             "pageSize": 0,
             "currentPage": 0,
             "totalCount": 0,
             "totalPages": 0
           },
           "status": null,
           "datafiles": []
         },
         "result": {
           "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
           "studyName": "EXP01",
           "studyTypeDbId": null,
           "studyTypeName": null,
           "studyDescription": "",
           "seasons": [
             "2018"
           ],
           "commonCropName": "",
           "trialDbId": null,
           "trialName": null,
           "startDate": "2018-07-01",
           "endDate": "2019-02-01",
           "active": false,
           "license": null,
           "location": null,
           "contacts": [],
           "dataLinks": [],
           "lastUpdate": null,
           "additionalInfo": null,
           "documentationURL": null
         }
        }
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
    
    /**
     * Retrieve one study observations
     * @param studyDbId
     * @param observationVariableDbIds
     * @param limit
     * @param page
     * @return the study observations
     * @example
     *  {
            "metadata": {
              "pagination": {
                "pageSize": 20,
                "currentPage": 0,
                "totalCount": 1,
                "totalPages": 1
              },
              "status": null,
              "datafiles": []
            },
            "result": {
              "data": [
                {
                  "germplasmDbId": null,
                  "germplasmName": null,
                  "observationDbId": null,
                  "observationLevel": "http://www.opensilex.org/vocabulary/oeso#Plant",
                  "observationTimeStamp": "2019-02-27",
                  "observationUnitDbId": "http://www.phenome-fppn.fr/platform/2019/o19000001",
                  "observationUnitName": "Plant01",
                  "observationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v004",
                  "observationVariableName": "ttt_mmm_uuu",
                  "operator": null,
                  "season": null,
                  "studyDbId": "http://www.opensilex.org/demo/DMO2018-3",
                  "uploadedBy": null,
                  "value": "0.484969"
                }
              ]
            }
        }
     */  
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
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = "observationVariableDbIds") @QueryParam(value = "observationVariableDbIds") List<String> observationVariableDbIds,  
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws SQLException {               
        
        StudyDAO studyDAO = new StudyDAO();
        List<String> variableURIs = new ArrayList();
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }      
        
        studyDAO.limit=1;
        studyDAO.user = userSession.getUser();
        
        if (observationVariableDbIds != null) {
            variableURIs= observationVariableDbIds;
        }
        
        return getStudyObservations(studyDAO, variableURIs, limit, page);
    }
    /**
     * Retrieve all observation variables measured in the study
     * @param studyDbId
     * @param limit
     * @param page
     * @return the study observations
     * @example
        {
          "metadata": {
            "pagination": {
              "pageSize": 1,
              "currentPage": 1,
              "totalCount": 2,
              "totalPages": 2
            },
            "status": null,
            "datafiles": []
          },
          "result": {
            "data": [
              {
                "ObservationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v004",
                "ObservationVariableName": "ttt_mmm_uuu",
                "ontologyReference": null,
                "synonyms": [],
                "contextOfUse": [],
                "growthStage": null,
                "status": null,
                "xref": null,
                "institution": null,
                "scientist": null,
                "submissionTimesTamp": null,
                "language": null,
                "crop": null,
                "trait": {
                  "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t003",
                  "traitName": "ttt",
                  "class": null,
                  "description": null,
                  "synonyms": [],
                  "mainAbbreviation": null,
                  "alternativeAbbreviations": [],
                  "entity": null,
                  "attribute": null,
                  "status": null,
                  "xref": null,
                  "ontologyReference": null
                },
                "method": {
                  "methodDbId": "http://www.phenome-fppn.fr/platform/id/methods/m003",
                  "methodName": "mmm",
                  "class": null,
                  "description": null,
                  "formula": null,
                  "ontologyReference": null,
                  "reference": null
                },
                "scale": {
                  "scaleDbid": "http://www.phenome-fppn.fr/platform/id/units/u004",
                  "scaleName": "uuu",
                  "dataType": "Numerical",
                  "decimalPlaces": null,
                  "ontologyReference": null,
                  "xref": null,
                  "validValues": null
                },
                "defaultValue": null,
                "documentationURL": null
              }
            ]
          }
        }
     */  
    @GET
    @Path("{studyDbId}/observationVariables")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
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
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @PathParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws SQLException {               
        
        StudyDAO studyDAO = new StudyDAO();
        
        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }      
        
        studyDAO.limit=1;
        studyDAO.user = userSession.getUser();
        ArrayList<Status> statusList = new ArrayList<>();  
                
        ArrayList<BrapiObservationDTO> observationsList = getObservationsList(studyDAO, new ArrayList());
        ArrayList<String> variableURIs = new ArrayList();
        ArrayList<BrapiVariable> obsVariablesList = new ArrayList();
        for (BrapiObservationDTO obs:observationsList) {  
            if (!variableURIs.contains(obs.getObservationVariableDbId())){
                variableURIs.add(obs.getObservationVariableDbId());
                VariableDaoSesame varDAO = new VariableDaoSesame();
                varDAO.uri = obs.getObservationVariableDbId();
                BrapiVariable obsVariable = varDAO.getBrapiVarData().get(0);
                obsVariablesList.add(obsVariable);               
            }            
        }
        if (observationsList.isEmpty()) {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, obsVariablesList, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, obsVariablesList, false);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }      
    }
    
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "no result for this query"));
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
            ArrayList<StudyDetails> nostudy = new ArrayList();
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, nostudy, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiSingleResponseForm getSingleResponse = new BrapiSingleResponseForm(study);
            return Response.status(Response.Status.OK).entity(getSingleResponse).build();
        }        
    }
    
    /**
     * Retrieve the observations corresponding to the user query (parameters: one specific study and eventually some variables)
     * @param ArrayList<Dataset> datasets
     * @return observations list 
     */
    private Response getStudyObservations(StudyDAO studyDAO, List<String> variableURIs, int limit, int page) {
        ArrayList<Status> statusList = new ArrayList<>();         
        ArrayList<BrapiObservationDTO> observations = getObservationsList(studyDAO,variableURIs);
                
        if (observations.isEmpty()) {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(0, 0, observations, true);
            return noResultFound(getResponse, statusList);
        } else {
            BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, observations, false);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }    
    }
    
    private ArrayList<BrapiObservationDTO> getObservationsList(StudyDAO studyDAO, List<String> variableURIs) {
        DatasetDAOMongo datasetDAOMongo = new DatasetDAOMongo();
        ArrayList<BrapiObservationDTO> observations = new ArrayList();
        datasetDAOMongo.experiment = studyDAO.studyDbId; 
        
        if (variableURIs.isEmpty()) {        
            ArrayList<Dataset> datasets = datasetDAOMongo.allPaginate();
            observations = getObservationsFromDatasets(datasets);
        } else {
            //in case a variable uri is duplicated, we keep distinct uris
            List<String> uniqueVariableURIs= variableURIs.stream().distinct().collect(Collectors.toList());
            for (String variableURI:uniqueVariableURIs) { 
                datasetDAOMongo.variable = variableURI;
                ArrayList<Dataset> datasets = datasetDAOMongo.allPaginate();
                ArrayList<BrapiObservationDTO> variableURIObservations = getObservationsFromDatasets(datasets);
                observations.addAll(variableURIObservations);              
            }
        }     
        return observations;
    }
    
    /**
     * Create observations with datasets attributes
     * @param ArrayList<Dataset> datasets
     * @return observations list 
     */
    private ArrayList<BrapiObservationDTO> getObservationsFromDatasets(ArrayList<Dataset> datasets) {
        ArrayList<BrapiObservationDTO> observations = new ArrayList();
        for (Dataset dataset:datasets){
            ArrayList<AgronomicalData> dataList = dataset.getData();
            for (AgronomicalData data:dataList){
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
        return observations;
    }

}
