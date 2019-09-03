//******************************************************************************
//                           VariableResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 27 Sept. 2018
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.DataDAO;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.dao.StudySQLDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.view.brapi.form.BrapiSingleResponseForm;
import opensilex.service.model.BrapiVariable;
import opensilex.service.model.Call;
import opensilex.service.model.Data;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.Variable;
import opensilex.service.resource.ResourceService;
import opensilex.service.resource.dto.data.BrapiObservationDTO;

/**
 * Variable resource service.
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@Api("/brapi/v1/variables")
@Path("/brapi/v1")
public class VariableResourceService extends ResourceService implements BrapiCall {
    final static Logger LOGGER = LoggerFactory.getLogger(BrapiVariable.class);

    /**
     * Overriding BrapiCall method.
     * @return variable call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        
        //SILEX:info 
        //Calls description
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("variables", calldatatypes, callMethods, callVersions);
        Call call2 = new Call("variables/{variables}", calldatatypes, callMethods, callVersions);
        Call call3 = new Call("observationvariables?studyDbId={studyDbId}", calldatatypes, callMethods, callVersions);
        //\SILEX:info       
        
        calls.add(call1);
        calls.add(call2);
        calls.add(call3);
        
        return calls;
    }
    
    /**
     * Retrieve all the variables available in the system
     * @param limit
     * @param page
     * @return the information of all the variable
     * @throws java.sql.SQLException
     * @example
     *  {
     *    "metadata": {
     *      "pagination": {
     *        "pageSize": 20,
     *        "currentPage": 0,
     *        "totalCount": 1,
     *        "totalPages": 1
     *      },
     *      "status": [],
     *      "datafiles": []
     *    },
     *    "result": {
     *      "data": [
     *        {
     *          "ObservationVariableDbId": "http://www.phenome-fppn.fr/platform/id/variables/v001",
     *          "name": "Leaf-Area_Index_m2.m2",
     *          "ontologyDbId": null,
     *          "ontologyName": null,
     *          "synonyms": null,
     *          "contextOfUse": null,
     *          "growthStage": null,
     *          "status": null,
     *          "xref": null,
     *          "institution": null,
     *          "scientist": null,
     *          "submissionTimesTamp": null,
     *          "language": null,
     *          "crop": null,
     *          "trait": {
     *            "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
     *            "name": "Leaf_Area_Index",
     *            "class": null,
     *            "description": "",
     *            "synonyms": null,
     *            "mainAbbreviation": null,
     *            "alternativeAbbreviations": null,
     *            "entity": null,
     *            "attribute": null,
     *            "status": null,
     *            "xref": null
     *          },
     *          "method": {
     *            "methodDbId": "http://www.phenome-fppn.fr/platform/id/methods/m001",
     *            "name": "LAI_Computation",
     *            "class": null,
     *            "description": "",
     *            "formula": null,
     *            "reference": null
     *          },
     *          "scale": {
     *            "scaleDbid": "http://www.phenome-fppn.fr/platform/id/units/u001",
     *            "name": "m2.m2",
     *            "datatype": null,
     *            "decimalPlaces": null,
     *            "xref": null,
     *            "validValues": null
     *          },
     *          "defaultValue": null
     *        }
     *      ]
     *    }
     *  }
     */
    @Path("/variables")
    @GET
    @ApiOperation(value = DocumentationAnnotation.VARIABLE_CALL_MESSAGE,
                       notes = DocumentationAnnotation.VARIABLE_CALL_MESSAGE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = DocumentationAnnotation.VARIABLE_CALL_MESSAGE, response = BrapiVariable.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})    
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                         dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesList ( 
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
        //SILEX:todo
        //to be discussed : do we keep as inputs the concepts we don't manage ?    
        //@ApiParam(value = "traitClass") @QueryParam("traitClass") String traitClass
        //\SILEX:todo    
        ) throws SQLException {        
        VariableDAO varDAO = new VariableDAO();
        varDAO.setPageSize(limit);
        varDAO.setPage(page);               
                
        ArrayList<Status> statusList = new ArrayList<>();                
        //Get number of variables corresponding to the search params
        Integer totalCount = varDAO.count();
        //Get the variables to return
        ArrayList<BrapiVariable> brapiVariables = varDAO.getBrapiVarData();

        BrapiMultiResponseForm getResponse;
        if (!brapiVariables.isEmpty()) {
            getResponse = new BrapiMultiResponseForm(varDAO.getPageSize(), varDAO.getPage(), brapiVariables, true, totalCount);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            getResponse = new BrapiMultiResponseForm(0, 0, brapiVariables, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * Retrieves the detail of one variable from the URI given.
     * @param variableUri 
     * @return the variable information
     * @throws java.sql.SQLException
     */
    @GET
    @Path("/variables/{observationVariableDbId}")
    @ApiOperation(value = DocumentationAnnotation.VARIABLE_DETAILS_CALL_MESSAGE,
            notes = DocumentationAnnotation.VARIABLE_DETAILS_CALL_MESSAGE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = DocumentationAnnotation.VARIABLE_DETAILS_CALL_MESSAGE, response = BrapiVariable.class , responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})    
    @ApiImplicitParams({
       @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                         dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariableDetails ( 
        @ApiParam(value = DocumentationAnnotation.VARIABLE_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @PathParam("observationVariableDbId") @Required @URL String variableUri
    ) throws SQLException {        
        VariableDAO varDAO = new VariableDAO();
        ArrayList<BrapiVariable> results = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        try {
            BrapiVariable brapiVariable = varDAO.findBrapiVariableById(variableUri);
            BrapiSingleResponseForm getResponse = new BrapiSingleResponseForm(brapiVariable);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } catch (Exception e) {
            statusList.add(new Status(e.getMessage(), "", ""));
            BrapiMultiResponseForm getNoResponse = new BrapiMultiResponseForm(0, 0, results, true);
            return noResultFound(getNoResponse, statusList);
        }
    }    
    
    /**
     * Brapi Call GET studies/observationvariables?studyDbId={studyDbId} V1.3
     * Retrieve all observation variables measured in the study
     * @param studyDbId
     * @param limit
     * @param page
     * @return the study observation variables
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
    @Path("observationvariables")
    @ApiOperation(value = "List all the observation variables measured in the study.", notes = "List all the observation variables measured in the study.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = BrapiVariable.class, responseContainer = "List"),
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

    public Response getObservationVariables (
        @ApiParam(value = "studyDbId", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI ) @QueryParam("studyDbId") @URL @Required String studyDbId,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws SQLException {               

        StudySQLDAO studyDAO = new StudySQLDAO();

        if (studyDbId != null) {
            studyDAO.studyDbIds = new ArrayList();
            studyDAO.studyDbIds.add(studyDbId);
        }      

        studyDAO.setPageSize(1);
        studyDAO.user = userSession.getUser();
        ArrayList<Status> statusList = new ArrayList<>();  

        ArrayList<BrapiObservationDTO> observationsList = getObservationsList(studyDAO, new ArrayList());
        ArrayList<String> variableURIs = new ArrayList();
        ArrayList<BrapiVariable> obsVariablesList = new ArrayList();
        for (BrapiObservationDTO obs:observationsList) {  
            if (!variableURIs.contains(obs.getObservationVariableDbId())){
                variableURIs.add(obs.getObservationVariableDbId());
                VariableDAO varDAO = new VariableDAO();
                try {
                    BrapiVariable obsVariable = varDAO.findBrapiVariableById(obs.getObservationVariableDbId());
                    obsVariablesList.add(obsVariable);  
                } catch (Exception ex) {
                    // Ignore unknown variable id
                }
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
    
    /**
     * Retrieve the observations list corresponding to the user query
     * @param studyDAO the study for which we want to retrieve the linked observations
     * @param variableURIs to filter the observations on a list of variableURIs defined by the user
     * @param limit pagesize
     * @param page the page number
     * @return observations list 
     */
    private ArrayList<BrapiObservationDTO> getObservationsList(StudySQLDAO studyDAO, List<String> variableURIs) {

        ArrayList<BrapiObservationDTO> observations = new ArrayList();  
        ScientificObjectRdf4jDAO objectDAO = new ScientificObjectRdf4jDAO();
        ArrayList<ScientificObject> objectsList = objectDAO.find(null, null, null, null, studyDAO.studyDbIds.get(0), null);
        ArrayList<Variable> variablesList = new ArrayList();

        if (variableURIs.isEmpty()) {  
            VariableDAO variableDaoSesame = new VariableDAO();
            //if variableURIs is empty, we look for all variables observations
            variablesList = variableDaoSesame.getAll(false, false); 

        } else {            
            //in case a variable uri is duplicated, we keep distinct uris
            List<String> uniqueVariableURIs= variableURIs.stream().distinct().collect(Collectors.toList());
            for (String variableURI:uniqueVariableURIs) {
                VariableDAO variableDAO = new VariableDAO();
                try {
                    Variable variable = variableDAO.findById(variableURI);
                    variablesList.add(variable);
                } catch (Exception ex) {
                    // ignore unknown variables
                }

            }                
        }

        for (Variable variable:variablesList) {
            DataDAO dataDAOMongo = new DataDAO();
            dataDAOMongo.variableUri = variable.getUri();
            ArrayList<BrapiObservationDTO> observationsPerVariable = new ArrayList();
            for (ScientificObject object:objectsList) {            
                dataDAOMongo.objectUri = object.getUri();
                ArrayList<Data> dataList = dataDAOMongo.allPaginate();
                ArrayList<BrapiObservationDTO> observationsPerVariableAndObject = getObservationsFromData(dataList,variable,object);
                observationsPerVariable.addAll(observationsPerVariableAndObject);            
            }
            observations.addAll(observationsPerVariable);
        }

        return observations;
    }

    /**
     * Fill the observations attributes with Data, Variable and ScientificObject attributes
     * @param dataList list of data corresponding to the variable and the scientificObject
     * @param variable variable linked to the dataList
     * @param object scientific object linked to the dataList
     * @return observations list 
     */
    private ArrayList<BrapiObservationDTO> getObservationsFromData(ArrayList<Data> dataList, Variable variable, ScientificObject object) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        ArrayList<BrapiObservationDTO> observations = new ArrayList();

        for (Data data:dataList){            
            BrapiObservationDTO observation= new BrapiObservationDTO();
            observation.setObservationUnitDbId(object.getUri());
            observation.setObservationUnitName(object.getLabel());
            observation.setObservationLevel(object.getRdfType());            
            observation.setStudyDbId(object.getExperiment());
            observation.setObservationVariableDbId(variable.getUri());
            observation.setObservationVariableName(variable.getLabel());    
            observation.setObservationDbId(data.getUri());
            observation.setObservationTimeStamp(df.format(data.getDate()));
            observation.setValue(data.getValue().toString());
            observations.add(observation);
        }

        return observations;
    }
       
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "No result"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }        
    
}
