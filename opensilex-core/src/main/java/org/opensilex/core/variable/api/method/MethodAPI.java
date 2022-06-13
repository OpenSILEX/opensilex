//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.method;

import io.swagger.annotations.*;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensilex.core.variable.api.VariableAPI.*;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(MethodAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class MethodAPI {

    public static final String PATH = "/core/methods";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Add a method")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A method is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A method with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createMethod(
            @ApiParam("Method description") @Valid MethodCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);
            MethodModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Method already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a method")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method retrieved", response = MethodDetailsDTO.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response getMethod(
            @ApiParam(value = "Method URI", example = "http://opensilex.dev/set/variables/method/ImageAnalysis", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);
        MethodModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(new MethodDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }
    
    
    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed methods by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return methods", response = MethodDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Method not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getMethodsByURIs(
            @ApiParam(value = "Methods URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        
        BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);

        try {
            List<MethodDetailsDTO> resultDTOList = dao.getList(uris)
                    .stream()
                    .map(MethodDetailsDTO::new)
                    .collect(Collectors.toList());

            return new PaginatedListResponse<>(resultDTOList).getResponse();

        }catch (SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.NOT_FOUND, "Methods not found", e.getStrUris()).getResponse();
        }
    }
    
    
    @PUT
    @ApiOperation("Update a method")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown method URI", response = ErrorResponse.class)
    })
    public Response updateMethod(
            @ApiParam("Method description") @Valid MethodUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);

        MethodModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a method")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown method URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMethod(
            @ApiParam(value = "Method URI", example = "http://opensilex.dev/set/variables/method/ImageAnalysis", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }



    @GET
    @ApiOperation("Search methods by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return methods", response = MethodGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMethods(
            @ApiParam(value = "Name (regex)", example = "ImageAnalysis") @QueryParam("name") String namePattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<MethodModel> dao = new BaseVariableDAO<>(MethodModel.class, sparql);
        ListWithPagination<MethodModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<MethodGetDTO> resultDTOList = resultList.convert(
                MethodGetDTO.class,
                MethodGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
