//******************************************************************************
//                         FactorResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Dec. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.FactorDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.factor.FactorDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Factor;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Factor resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/factors")
@Path("/factors")
public class FactorResourceService extends ResourceService {
    
    /**
     * Generates a factorDTO list from a given list of factor.
     * @param factor
     * @return the list of factor DTOs
     */
    private ArrayList<FactorDTO> factorToFactorDTO(ArrayList<Factor> factor) {
        ArrayList<FactorDTO> factorDTO = new ArrayList<>();
        
        factor.forEach((factorEntity) -> {
            factorDTO.add(new FactorDTO(factorEntity));
        });
        
        return factorDTO;
    }
    
    /**
     * Gets the factor corresponding to the search parameters given.
     * @param pageSize
     * @param page
     * @param uri
     * @param label
     * @param language
     * @return the result
     * @example 
     * {
     *    "metadata": {
     *      "pagination": {
     *          "pageSize": 2,
     *          "currentPage": 0,
     *          "totalCount": 18,
     *          "totalPages": 9
     *      },
     *      "status": [],
     *      "datafiles": []
     *    },
     *    "result": {
     *      "data": [
     *          {
     *              "uri": "http://www.phenome-fppn.fr/id/factor/zeamays",
     *              "label": "Water Exposure",
     *              "comment" : "Change the frequency on water addition"
     *          },
     *          {
     *              "uri": "http://www.phenome-fppn.fr/id/factor/gossypiumhirsutum",
     *              "label": "Shading management",
     *              "comment" : "Apply different shading process on entities"
     *          }
     *      ]
     *    }
     * }
     */
    @GET
    @ApiOperation(value = "Get all factor corresponding to the search params given",
                  notes = "Retrieve all factor authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all factor", response = FactorDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactorsByCriteria(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by factor uri", example = DocumentationAnnotation.EXAMPLE_FACTOR_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by factor label", example = DocumentationAnnotation.EXAMPLE_FACTOR_LABEL) @QueryParam("label") String label,
        @ApiParam(value = "Select language", example = DocumentationAnnotation.EXAMPLE_LANGUAGE) @QueryParam("language") String language) {
        //1. Initialize factor filter
        Factor filter = new Factor();
        filter.setUri(uri);
        filter.setLabel(label);
        
        if (language == null) {
            language = DEFAULT_LANGUAGE;
        }
        
        FactorDAO factorDAO = new FactorDAO();
        factorDAO.setPage(page);
        factorDAO.setPageSize(pageSize);
        
        //2. Get number of factor result
        int totalCount = factorDAO.countWithFilter(filter.getUri(),filter.getLabel(),language);
        //3. Get factors result
        ArrayList<Factor> searchResult = factorDAO.findAll(filter.getUri(),filter.getLabel(),language);
        
        //4. Send result
        ResultForm<FactorDTO> getResponse;
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<FactorDTO> factorToReturn = new ArrayList<>();       
        if (searchResult == null || searchResult.isEmpty()) {
            //No result found
            getResponse = new ResultForm<>(0, 0, factorToReturn, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            factorToReturn = factorToFactorDTO(searchResult);
            //Return the result list
            getResponse = new ResultForm<>(pageSize, page, factorToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
    
    
    
     /**
     * Factor POST service.
     * @param factor
     * @param context
     * @return the POST result
     */
    @POST
    @ApiOperation(value = "Post factor(s)",
                  notes = "Register new factor(s) in the data base")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Factor(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                          dataType = "string", paramType = "header",
                          value = DocumentationAnnotation.ACCES_TOKEN,
                          example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postUnit(@ApiParam(value = DocumentationAnnotation.FACTOR_POST_DATA_DEFINITION, required = true) @Valid ArrayList<FactorDTO> factor,
                              @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (factor != null && !factor.isEmpty()) {
            FactorDAO factorDao = new FactorDAO();
            if (context.getRemoteAddr() != null) {
                factorDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            factorDao.user = userSession.getUser();
            
            POSTResultsReturn result = factorDao.checkAndInsert(factor);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201: factor inserted
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty factor(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
}
