//******************************************************************************
//                          MoblieModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.mobile.dal.FormDAO;
import org.opensilex.mobile.dal.FormModel;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * <pre>
 * Mobile API which provides:
 *
 * - Create form
 * - Delete form
 * - Get all forms, or get forms corresponding to a list of uri's
 * - Update form
 * </pre>
 *
 * @author Maximilian Hart
 */
@Api(MobileAPI.CREDENTIAL_MOBILE_GROUP_ID)
@Path("/mobile")
public class MobileAPI { 
    public static final String CREDENTIAL_MOBILE_GROUP_ID = "Mobile";
    
    @Inject
    private MongoDBService nosql;

    /**
     * Create a form
     *
     * @param dto the Form to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Form {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @POST
    @Path("forms")
    @ApiOperation("Add a form")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Add a form", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
    })
    public Response createForm(
            @ApiParam("Form to save") @NotNull @Valid FormCreationDTO dto
    ) throws Exception {
        FormDAO dao = new FormDAO(nosql);
        FormModel createdForm = dao.create(dto.newModel());
        return new ObjectUriResponse(Response.Status.CREATED, createdForm.getUri()).getResponse();
    }
    
    /**
     * Get forms
     *
     * @param uris List of form uris to get, leave null to get all
     * @param orderByList How to order the returned forms
     * @param page Which page to get (see pagination system)
     * @param pageSize How many forms per page
     * @return a {@link Response} with a paginated list if successful
     * @throws java.lang.Exception if fail
     */
    @GET
    @ApiOperation("Search forms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return form list", response = FormGetDTO.class, responseContainer = "List")
    })
    public Response searchFormList(
            @ApiParam(value = "Search by uris") @QueryParam("uris") List<URI> uris,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        FormDAO dao = new FormDAO(nosql);


        ListWithPagination<FormModel> resultList = dao.search(
                uris,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<FormGetDTO> resultDTOList = resultList.convert(FormGetDTO.class, FormGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    /**
     * Delete form
     *
     * @param uri , uri of the form to delete
     * @return a {@link Response} 
     * @throws java.lang.Exception if fail
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete form")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Form deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Form URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteForm(
            @ApiParam(value = "Form URI", required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        try {
            FormDAO dao = new FormDAO(nosql);
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
    }
    
    /**
     * Update form
     *
     * @param dto , form containing modifications to update
     * @return a {@link Response} 
     * @throws java.lang.Exception if fail
     */
    @PUT
    @ApiProtected
    @ApiOperation("Update form")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Form description") @Valid FormUpdateDTO dto
    ) throws Exception {

        FormDAO dao = new FormDAO(nosql);

        try {
            FormModel model = dto.newModel();
            dao.update(model);
            return new SingleObjectResponse<>(FormGetDTO.fromModel(model)).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", dto.getUri());
        
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        }
    }
}
