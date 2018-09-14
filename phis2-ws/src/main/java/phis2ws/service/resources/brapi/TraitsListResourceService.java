//******************************************************************************
//                                       TraitsListResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug, 2018
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
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.sesame.BrapiTraitDAO;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormBrapiTraits;
import phis2ws.service.view.model.phis.BrapiTrait;
import phis2ws.service.view.model.phis.Call;

@Api("/brapi/v1/traits")
@Path("/brapi/v1/traits")

/**
 * Traits service
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class TraitsListResourceService implements BrapiCall {
    final static Logger LOGGER = LoggerFactory.getLogger(TraitsListResourceService.class);
    
    /**
    * Overriding BrapiCall method
    * @date 28 Aug 2018
    * @return traits call information
    */
    @Override
    public Call callInfo() {
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.2");
        Call call = new Call("traits", calldatatypes, callMethods, callVersions);
        return call;
    }
    
    /**
     * retrieve the list of traits 
     * 
     * @param limit
     * @param page
     * 
     * @return list of the traits corresponding to the search params given
     * e.g
     * {
        "metadata": {
          "pagination": {
            "pageSize": 20,
            "currentPage": 0,
            "totalCount": 2,
            "totalPages": 1
          },
          "status": [],
          "datafiles": []
        },
        "result": {
          "data": [
            {
              "defaultValue": null,
              "description": "",
              "name": "Leaf_Area_Index",
              "observationVariables": [
                "http://www.phenome-fppn.fr/platform/id/variables/v001"
              ],
              "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
              "traitId": null
            },
            {
              "defaultValue": null,
              "description": "",
              "name": "NDVI",
              "observationVariables": [
                "http://www.phenome-fppn.fr/platform/id/variables/v002"
              ],
              "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t002",
              "traitId": null
            }
          ]
        }
      }
     */
    
    @GET
    @ApiOperation(value = DocumentationAnnotation.TRAIT_CALL_MESSAGE,
                       notes = DocumentationAnnotation.TRAIT_CALL_MESSAGE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = DocumentationAnnotation.TRAIT_CALL_MESSAGE, response = BrapiTrait.class, responseContainer = "List"),
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
    public Response getTraitsList ( 
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0)int page 
        ) throws SQLException {        
        BrapiTraitDAO traitDAO = new BrapiTraitDAO();
        traitDAO.setPageSize(limit);
        traitDAO.setPage(page);           
        return getTraitsData(traitDAO);
    }
    
    /**
     * Return a generic response when no result are found
     * @param getResponse
     * @param insertStatusList
     * @return the response "no result found" for the service
     */
    private Response noResultFound(ResponseFormBrapiTraits getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No result", StatusCodeMsg.INFO, "No results"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * Search Traits corresponding to search params given by a user
     * @param BrapiTraitDAO
     * @return the traits available in the system
     */
    private Response getTraitsData(BrapiTraitDAO traitDAO) {
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormBrapiTraits getResponse;
        ArrayList<BrapiTrait> traits = traitDAO.allPaginate();
        if (traits == null) {
            getResponse = new ResponseFormBrapiTraits(0, 0, traits, true);
            return noResultFound(getResponse, statusList);
        } else if (!traits.isEmpty()) {
            getResponse = new ResponseFormBrapiTraits(traitDAO.getPageSize(), traitDAO.getPage(), traits, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormBrapiTraits(0, 0, traits, true);
            return noResultFound(getResponse, statusList);
        }
    }
}
