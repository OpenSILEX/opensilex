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
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
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
import opensilex.service.model.Variable;
import opensilex.service.model.BrapiVariableTrait;
import opensilex.service.model.BrapiMethod;
import opensilex.service.model.BrapiScale;

/**
 * Variable resource service.
 * @See https://brapi.docs.apiary.io/#reference/observation-variables
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@Api("/brapi/v1/variables")
@Path("/brapi/v1/variables")
public class VariableResourceService implements BrapiCall {
    final static Logger LOGGER = LoggerFactory.getLogger(BrapiVariable.class);

    /**
     * Overriding BrapiCall method.
     * @return variable call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        
        //SILEX:info 
        //Call GET Variable list
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("variables", calldatatypes, callMethods, callVersions);
        //\SILEX:info 
        
        //SILEX:info 
        //Call GET Variable details
        ArrayList<String> calldatatypes2 = new ArrayList<>();
        calldatatypes2.add("json");
        ArrayList<String> callMethods2 = new ArrayList<>();
        callMethods2.add("GET");
        ArrayList<String> callVersions2 = new ArrayList<>();
        callVersions2.add("1.3");
        Call call2 = new Call("variables/{variables}", calldatatypes2, callMethods2, callVersions2);
        //\SILEX:info 
        
        calls.add(call1);
        calls.add(call2);
        
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
                
        return getVariablesData(varDAO);
    }
    
    /**
     * Retrieves the detail of one variable from the URI given.
     * @param variableUri 
     * @return the variable information
     * @throws java.sql.SQLException
     */
    @GET
    @Path("{observationVariableDbId}")
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
        varDAO.uri = variableUri;
        return getOneVariableData(varDAO);
    }    
    
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "No result"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }        
    
    /**
     * Gets a variables data list.
     * @param varDAO
     * @return the response with the variables data list 
     */
    private Response getVariablesData(VariableDAO varDAO) {
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<BrapiVariable> brapiVariables = getBrapiVarData(varDAO);
        BrapiMultiResponseForm getResponse;
        if (!brapiVariables.isEmpty()) {
            getResponse = new BrapiMultiResponseForm(varDAO.getPageSize(), varDAO.getPage(), brapiVariables, false);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            getResponse = new BrapiMultiResponseForm(0, 0, brapiVariables, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * Gets a variable data.
     * @param varDAO
     * @return the response with one variable data
     */
    private Response getOneVariableData(VariableDAO varDAO) {
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<BrapiVariable> brapiVariables = getBrapiVarData(varDAO);
        BrapiSingleResponseForm getResponse;
        if (!brapiVariables.isEmpty()){
            BrapiVariable variable = brapiVariables.get(0);
            getResponse = new BrapiSingleResponseForm(variable);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            BrapiMultiResponseForm getNoResponse = new BrapiMultiResponseForm(0, 0, brapiVariables, true);
            return noResultFound(getNoResponse, statusList);
        }
    }    
    
    /**
     * Gets the list of BrAPI variables from the the DAO corresponding to the search query
     * @param varDAO
     * @return the list of BrAPI variables
     */
    private ArrayList<BrapiVariable> getBrapiVarData(VariableDAO varDAO) {
        ArrayList<Variable> variablesList = varDAO.allPaginate();
        ArrayList<BrapiVariable> varList = new ArrayList();
        for (Variable var:variablesList) {
            BrapiVariable brapiVar = new BrapiVariable();
            brapiVar.setObservationVariableDbId(var.getUri());
            brapiVar.setObservationVariableName(var.getLabel());
            brapiVar.setContextOfUse(new ArrayList());
            brapiVar.setSynonyms(new ArrayList());
                        
            //trait 
            BrapiVariableTrait trait = new BrapiVariableTrait();
            trait.setTraitDbId(var.getTrait().getUri());
            trait.setTraitName(var.getTrait().getLabel());
            trait.setDescription(var.getTrait().getComment());
            trait.setAlternativeAbbreviations(new ArrayList());
            trait.setSynonyms(new ArrayList());
            brapiVar.setTrait(trait);
            
            //method
            BrapiMethod method = new BrapiMethod();
            method.setMethodDbId(var.getMethod().getUri());
            method.setMethodName(var.getMethod().getLabel());
            method.setDescription(var.getMethod().getComment());
            brapiVar.setMethod(method);
            
            //scale
            BrapiScale scale = new BrapiScale();
            scale.setScaleDbid(var.getUnit().getUri());
            scale.setScaleName(var.getUnit().getLabel());
            scale.setDataType("Numerical");
            brapiVar.setScale(scale);
            
            varList.add(brapiVar); 
        }
        return varList;        
    }
}
