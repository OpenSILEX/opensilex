package org.opensilex.core.variable.api.dimension;

import io.swagger.annotations.*;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.authentication.*;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.SPARQLResourceModel;
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
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(DimensionAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class DimensionAPI {

    public static final String PATH = "/core/dimensions";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Add a dimension")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "An dimension is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An dimension with the same URI already exists", response = ErrorResponse.class),
    })
    public Response createDimension(
            @ApiParam("Dimension description") @Valid DimensionCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);
            DimensionModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Dimension already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a dimension")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Dimension retrieved", response = DimensionDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown dimension URI", response = ErrorResponse.class)
    })
    public Response getDimension(
            @ApiParam(value = "Dimension URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);
        DimensionModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(new DimensionDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed dimensions by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return dimensions", response = DimensionDetailsDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Dimension not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getDimensionsByURIs(
            @ApiParam(value = "Dimensions URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {

        BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);

        try {
            List<DimensionDetailsDTO> resultDTOList = dao.getList(uris)
                    .stream()
                    .map(DimensionDetailsDTO::new)
                    .collect(Collectors.toList());

            return new PaginatedListResponse<>(resultDTOList).getResponse();

        } catch (SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.NOT_FOUND, "Dimensions not found", e.getStrUris()).getResponse();
        }

    }

    @GET
    @ApiOperation("Search dimensions by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Dimension list", response = DimensionGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchDimensions(
            @ApiParam(value = "Name (regex)") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);
        ListWithPagination<DimensionModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<DimensionGetDTO> resultDTOList = resultList.convert(
                DimensionGetDTO.class,
                DimensionGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @PUT
    @ApiOperation("Update a dimension")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Dimension updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown dimension URI", response = ErrorResponse.class)
    })
    public Response updateDimension(
            @ApiParam("Dimension description") @Valid DimensionUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);

        DimensionModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an dimension")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Dimension deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown dimension URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDimension(
            @ApiParam(value = "Dimension URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        if (this.isContainedInMultidimensional(uri)) {
            throw new ForbiddenURIAccessException(uri, "Dimension can't be deleted while linked to a multidimensional variable");
        }

        BaseVariableDAO<DimensionModel> dao = new BaseVariableDAO<>(DimensionModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * It returns true if the given URI is contained in a multidimensional variable
     *
     * @param uri the URI of the dimension to be deleted
     * @return A boolean.
     */
    private boolean isContainedInMultidimensional(URI uri) throws Exception {
        return sparql.search(
                VariableModel.class,
                null,
                (SelectBuilder select) -> {
                    select.addWhere(makeVar(SPARQLResourceModel.URI_FIELD), Oeso.hasDimensions, SPARQLDeserializers.nodeURI(uri));
                }
        ).size() > 0;
    }

}