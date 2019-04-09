//******************************************************************************
//                           TraitResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 17 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.TraitDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.TraitDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Trait;

/**
 * Trait resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/traits")
@Path("traits")
public class TraitResourceService extends ResourceService {
    
    /**
     * Trait POST service.
     * @param traits
     * @param context
     * @return POST result
     */
    @POST
    @ApiOperation(value = "Post trait(s)",
                  notes = "Register new trait(s) in the data base")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Trait(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postTrait(@ApiParam(value = DocumentationAnnotation.TRAIT_POST_DATA_DEFINITION) @Valid ArrayList<TraitDTO> traits,
                              @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (traits != null && !traits.isEmpty()) {
            TraitDAO traitDao = new TraitDAO();
            if (context.getRemoteAddr() != null) {
                traitDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            traitDao.user = userSession.getUser();
            
            POSTResultsReturn result = traitDao.checkAndInsert(traits);
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                //Code 201, traits insérés
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty traits(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Trait PUT service.
     * @param traits
     * @param context
     * @return PUT result
     */
    @PUT
    @ApiOperation(value = "Update trait")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Trait updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Trait not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putTrait(
        @ApiParam(value = DocumentationAnnotation.TRAIT_POST_DATA_DEFINITION) @Valid ArrayList<TraitDTO> traits,
        @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        if (traits != null && !traits.isEmpty()) {
            TraitDAO traitDao = new TraitDAO();
            if (context.getRemoteAddr() != null) {
                traitDao.remoteUserAdress = context.getRemoteAddr();
            }
            
            traitDao.user = userSession.getUser();
            
            POSTResultsReturn result = traitDao.checkAndUpdate(traits);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) {
                //Code 200, traits modifiés
                postResponse = new ResponseFormPOST(result.statusList);
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty traits(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Gets trait data.
     * @param traitDao
     * @return the traits found
     * SILEX:todo
     * Add other filters than URI and label
     * \SILEX:todo
     */
    private Response getTraitsData(TraitDAO traitDao) {
        ArrayList<Trait> traits;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<Trait> getResponse;
        
        traits = traitDao.allPaginate();
        
        if (traits == null) {
            getResponse = new ResultForm<>(0, 0, traits, true);
            return noResultFound(getResponse, statusList);
        } else if (!traits.isEmpty()) {
            getResponse = new ResultForm<>(traitDao.getPageSize(), traitDao.getPage(), traits, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<>(0, 0, traits, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * Trait GET method.
     * @param limit
     * @param page
     * @param uri
     * @param label
     * @return GET result
     */
    @GET
    @ApiOperation(value = "Get all Traits corresponding to the searched params given",
                  notes = "Retrieve all traits authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all traits", response = Trait.class, responseContainer = "List"),
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
    public Response getTraitsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_TRAIT_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by label", example = DocumentationAnnotation.EXAMPLE_TRAIT_LABEL) @QueryParam("label") String label
    ) {
        TraitDAO traitDao = new TraitDAO();
        
        if (uri != null) {
            traitDao.uri = uri;
        }
        if (label != null) {
            traitDao.label = label;
        }
        
        traitDao.user = userSession.getUser();
        traitDao.setPage(page);
        traitDao.setPageSize(limit);
        
        return getTraitsData(traitDao);
    }
    
    /**
     * Trait GET service.
     * @param trait
     * @param limit
     * @param page
     * @return the trait found
     */
    @GET
    @Path("{trait}")
    @ApiOperation(value = "Get a trait", 
                  notes = "Retrieve a trait. Need URL encoded trait URI (Unique resource identifier).")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve a trait.", response = Trait.class, responseContainer = "List"),
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
    public Response getTraitDetails(
        @ApiParam(value = DocumentationAnnotation.TRAIT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_TRAIT_URI, required = true) @PathParam("trait") @Required @URL String trait,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        
        if (trait == null) {
            final Status status = new Status(StatusCodeMsg.ACCESS_ERROR, StatusCodeMsg.ERR, "Empty trait URI");
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(status)).build();
        }
        
        TraitDAO traitDao = new TraitDAO();
        traitDao.uri = trait;
        traitDao.setPageSize(limit);
        traitDao.setPage(page);
        traitDao.user = userSession.getUser();
        
        return getTraitsData(traitDao);
    }
}
