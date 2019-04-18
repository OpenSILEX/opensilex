//******************************************************************************
//                         TraitsResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug. 2018
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
import opensilex.service.dao.TraitDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.resource.dto.trait.BrapiTraitDTO;
import opensilex.service.view.brapi.form.BrapiSingleResponseForm;
import opensilex.service.model.Call;
import opensilex.service.model.Trait;


/**
 * Traits resource service.
 * @See https://brapi.docs.apiary.io/#reference/traits
 * @update [Alice Boizet] 24 Sept. 2018: add Get Trait Details.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@Api("/brapi/v1/traits")
@Path("/brapi/v1/traits")
public class TraitsResourceService implements BrapiCall {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitsResourceService.class);
    
    /**
     * Overriding BrapiCall method
     * @return traits call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        
        //SILEX:info 
        //Call GET Trait list
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call1 = new Call("traits", calldatatypes, callMethods, callVersions);
        //\SILEX:info 
        
        //SILEX:info 
        //Call GET Trait details
        ArrayList<String> calldatatypes2 = new ArrayList<>();
        calldatatypes2.add("json");
        ArrayList<String> callMethods2 = new ArrayList<>();
        callMethods2.add("GET");
        ArrayList<String> callVersions2 = new ArrayList<>();
        callVersions2.add("1.3");
        Call call2 = new Call("traits/{traitDbId}", calldatatypes2, callMethods2, callVersions2);
        //\SILEX:info 
        
        calls.add(call1);
        calls.add(call2);
        
        return calls;
    }
    
    /**
     * Retrieves the list of traits.
     * @param limit
     * @param page 
     * @return list of the traits corresponding to the search params given
     * @throws java.sql.SQLException
     * @example
     * {
     * "metadata": {
     *    "pagination": {
     *      "pageSize": 20,
     *      "currentPage": 0,
     *      "totalCount": 2,
     *      "totalPages": 1
     *    },
     *    "status": [],
     *    "datafiles": []
     *  },
     *  "result": {
     *    "data": [
     *      {
     *        "defaultValue": null,
     *        "description": "",
     *        "traitName": "Leaf_Area_Index",
     *        "observationVariables": [
     *          "http://www.phenome-fppn.fr/platform/id/variables/v001"
     *        ],
     *        "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
     *        "traitId": null
     *      },
     *      {
     *        "defaultValue": null,
     *        "description": "",
     *        "traitName": "NDVI",
     *        "observationVariables": [
     *          "http://www.phenome-fppn.fr/platform/id/variables/v002"
     *        ],
     *        "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t002",
     *        "traitId": null
     *      }
     *    ]
     *  }
     *}
     */
    @GET
    @ApiOperation(value = DocumentationAnnotation.TRAIT_CALL_MESSAGE,
                       notes = DocumentationAnnotation.TRAIT_CALL_MESSAGE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = DocumentationAnnotation.TRAIT_CALL_MESSAGE, response = BrapiTraitDTO.class, responseContainer = "List"),
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
    public Response getTraitsList ( 
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0)int page 
        ) throws SQLException {        
        TraitDAO traitDAO = new TraitDAO();
        traitDAO.setPageSize(limit);
        traitDAO.setPage(page);           
        return getTraitsData(traitDAO);
    }
    
    /**
     * Retrieve the detail of one trait from the URI given..
     * @param traitUri
     * @return the trait information
     * @throws java.sql.SQLException
     * @example 
     * {
     *  "metadata": {
     *    "pagination": {
     *     "pageSize": 0,
     *     "currentPage": 0,
     *      "totalCount": 0,
     *      "totalPages": 0
     *    },
     *    "status": [],
     *    "datafiles": []
     *  },
     *  "result": {
     *    "data": {
     *      "defaultValue": null,
     *      "description": null,
     *      "traitName": "myTrait",
     *      "observationVariables": null,
     *      "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t003",
     *      "traitId": null
     *    }
     *  }
     *}
     */
    @GET
    @Path("{traitDbId}")
    @ApiOperation(value = DocumentationAnnotation.TRAIT_DETAILS_CALL_MESSAGE,
            notes = DocumentationAnnotation.TRAIT_DETAILS_CALL_MESSAGE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = DocumentationAnnotation.TRAIT_DETAILS_CALL_MESSAGE, response = BrapiTraitDTO.class, responseContainer = "List"),
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
    public Response getTraitDetails ( 
        @ApiParam(value = DocumentationAnnotation.TRAIT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_TRAIT_URI) @PathParam("traitDbId") @Required @URL String traitUri
    ) throws SQLException {        
        TraitDAO traitDAO = new TraitDAO(traitUri);           
        return getOneTraitData(traitDAO);
    }    
    
    /**
     * Returns a generic response when no result are found.
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(BrapiMultiResponseForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "No result"));
        getResponse.getMetadata().setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
        
    /**
     * Return a list of variable data.
     * @param traitDAO
     * @return the response with the traits data list 
     */
    private Response getTraitsData(TraitDAO traitDAO) {
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<BrapiTraitDTO> brapiTraits = getBrapiTraitsData(traitDAO);
        BrapiMultiResponseForm getResponse;
        if (!brapiTraits.isEmpty()) {
            getResponse = new BrapiMultiResponseForm(traitDAO.getPageSize(), traitDAO.getPage(), brapiTraits, false);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            getResponse = new BrapiMultiResponseForm(0, 0, brapiTraits, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * Returns a trait data.
     * @param traitDAO
     * @return the response with one trait data
     */
    private Response getOneTraitData(TraitDAO traitDAO) {
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<BrapiTraitDTO> brapiTraits = getBrapiTraitsData(traitDAO);
        BrapiSingleResponseForm getResponse;
        if (!brapiTraits.isEmpty()){
            BrapiTraitDTO trait = brapiTraits.get(0);
            getResponse = new BrapiSingleResponseForm(trait);
            getResponse.getMetadata().setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } else {
            BrapiMultiResponseForm getNoResponse = new BrapiMultiResponseForm(0, 0, brapiTraits, true);
            return noResultFound(getNoResponse, statusList);
        }
    } 
    
    /**
     * Gets list of BrAPI traits from the TraitDAO corresponding to the search query
     * @param BrapiTraitDAO
     * @return the traits available in the system
     */
    private ArrayList<BrapiTraitDTO> getBrapiTraitsData(TraitDAO traitDAO) {
        ArrayList<Trait> traits = traitDAO.allPaginate();  
        ArrayList<BrapiTraitDTO> brapiTraits = new ArrayList();          
                
        if (!traits.isEmpty()) {
            for (Trait trait:traits) {
                BrapiTraitDTO brapiTrait = new BrapiTraitDTO(trait);
                brapiTrait.setObservationVariables(traitDAO.getVariableFromTrait(trait));
                brapiTraits.add(brapiTrait);
            }
        }
        return brapiTraits;
    }
}