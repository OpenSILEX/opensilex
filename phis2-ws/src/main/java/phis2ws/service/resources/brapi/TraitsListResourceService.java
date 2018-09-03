//******************************************************************************
//                                       TraitsListResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 août 2018
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
    
    @GET
    @ApiOperation(value = "Retrieve the list of all traits available in the system",
                       notes = "Retrieve the list of all traits available in the system")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all experiments", response = BrapiTrait.class, responseContainer = "List"),
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
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page 
        ) throws SQLException {        
        BrapiTraitDAO traitDAO = new BrapiTraitDAO();
        traitDAO.setPageSize(limit);
        traitDAO.setPage(page);           
        return getTraitsData(traitDAO);
    }
    
    @GET
    @Path("{traitDbId}")
    @ApiOperation(value = "Retrieve trait details by id",
            notes = "Retrieve trait details by id")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve trait details", response = BrapiTrait.class, responseContainer = "List"),
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
    
    public Response getTraitDetails ( 
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page, 
        @ApiParam(value = DocumentationAnnotation.TRAIT_URI_DEFINITION, required = true, example=DocumentationAnnotation.EXAMPLE_TRAIT_URI) @QueryParam("traitDbId") @DefaultValue(DefaultBrapiPaginationValues.PAGE) String traitDbId
    ) throws SQLException {        
        BrapiTraitDAO traitDAO = new BrapiTraitDAO(traitDbId);
        traitDAO.setPageSize(limit);
        traitDAO.setPage(page);           
        return getTraitsData(traitDAO);
    }
    
    
    private Response noResultFound(ResponseFormBrapiTraits getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
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
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
