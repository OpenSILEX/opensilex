package org.opensilex.core.widget.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.core.widget.dal.WidgetDAO;
import org.opensilex.core.widget.dal.WidgetModel;
import org.opensilex.core.widget.dal.WidgetSearchFilter;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

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
import java.net.URI;
import java.util.List;

/**
 * REST resource of the widget concept.
 * <p>
 * Annotation order on endpoints (dominant in the code base): HTTP verb, {@code @Path}, {@code @ApiOperation},
 * {@code @ApiProtected}, {@code @ApiCredential} (write operations only), {@code @Consumes}, {@code @Produces},
 * {@code @ApiResponses}. Unexpected errors propagate ({@code throws Exception}) to the global exception mapper.
 */
@Api(WidgetAPI.CREDENTIAL_WIDGET_GROUP_ID)
@Path("/core/widgets")
@ApiCredentialGroup(
        groupId = WidgetAPI.CREDENTIAL_WIDGET_GROUP_ID,
        groupLabelKey = WidgetAPI.CREDENTIAL_WIDGET_GROUP_LABEL_KEY
)
public class WidgetAPI {

    public static final String CREDENTIAL_WIDGET_GROUP_ID = "Widgets";
    public static final String CREDENTIAL_WIDGET_GROUP_LABEL_KEY = "credential-groups.widgets";

    public static final String CREDENTIAL_WIDGET_MODIFICATION_ID = "widget-modification";
    public static final String CREDENTIAL_WIDGET_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_WIDGET_DELETE_ID = "widget-delete";
    public static final String CREDENTIAL_WIDGET_DELETE_LABEL_KEY = "credential.default.delete";

    private static final String NOT_FOUND_TITLE = "Widget not found";
    private static final String NOT_FOUND_MESSAGE = "Unknown widget URI: ";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @ApiOperation("Add a widget")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_WIDGET_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_WIDGET_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 201, message = "A widget is created", response = URI.class),
        @ApiResponse(code = 409, message = "A widget with the same URI or name already exists", response = ErrorDTO.class)
    })
    public Response createWidget(
            @ApiParam("Widget description") @Valid WidgetCreationDTO dto
    ) throws Exception {
        WidgetDAO dao = new WidgetDAO(sparql);

        if (dto.getUri() != null && sparql.uriExists(WidgetModel.class, dto.getUri())) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Widget already exists",
                    "Duplicated URI: " + dto.getUri()
            ).getResponse();
        }
        if (dao.nameExists(dto.getName())) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Widget already exists",
                    "Duplicated name: " + dto.getName()
            ).getResponse();
        }

        WidgetModel model = dto.newModel();
        model.setPublisher(currentUser.getUri());
        dao.create(model);

        return new CreatedUriResponse(model.getUri()).getResponse();
    }

    @PUT
    @ApiOperation("Update a widget")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_WIDGET_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_WIDGET_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 200, message = "Widget updated", response = URI.class),
        @ApiResponse(code = 404, message = "Widget not found", response = ErrorDTO.class)
    })
    public Response updateWidget(
            @ApiParam("Widget description") @Valid WidgetUpdateDTO dto
    ) throws Exception {
        WidgetDAO dao = new WidgetDAO(sparql);

        if (dao.get(dto.getUri(), currentUser) == null) {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    NOT_FOUND_TITLE,
                    NOT_FOUND_MESSAGE + dto.getUri()
            ).getResponse();
        }

        WidgetModel updated = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, updated.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a widget")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_WIDGET_DELETE_ID,
            credentialLabelKey = CREDENTIAL_WIDGET_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 200, message = "Widget deleted", response = URI.class),
        @ApiResponse(code = 404, message = "Widget URI not found", response = ErrorDTO.class)
    })
    public Response deleteWidget(
            @ApiParam(value = "Widget URI", example = "http://opensilex.dev/widgets#my-widget", required = true)
            @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        WidgetDAO dao = new WidgetDAO(sparql);

        if (dao.get(uri, currentUser) == null) {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    NOT_FOUND_TITLE,
                    NOT_FOUND_MESSAGE + uri
            ).getResponse();
        }

        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a widget")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 200, message = "Widget retrieved", response = WidgetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Widget not found", response = ErrorDTO.class)
    })
    public Response getWidget(
            @ApiParam(value = "Widget URI", example = "http://opensilex.dev/widgets#my-widget", required = true)
            @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        WidgetModel model = new WidgetDAO(sparql).get(uri, currentUser);

        if (model == null) {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    NOT_FOUND_TITLE,
                    NOT_FOUND_MESSAGE + uri
            ).getResponse();
        }

        return new SingleObjectResponse<>(WidgetDTO.getDTOFromModel(model)).getResponse();
    }

    @GET
    @ApiOperation("Search widgets")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 200, message = "Return widgets", response = WidgetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchWidgets(
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*")
            @QueryParam("name") String namePattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc")
            @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0")
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20")
            @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        WidgetSearchFilter filter = new WidgetSearchFilter().setNamePattern(namePattern);
        filter.setLang(currentUser.getLanguage());
        filter.setOrderByList(orderByList);
        filter.setPage(page);
        filter.setPageSize(pageSize);

        ListWithPagination<WidgetModel> results = new WidgetDAO(sparql).search(filter);
        ListWithPagination<WidgetDTO> dtos = results.convert(WidgetDTO.class, WidgetDTO::getDTOFromModel);

        return new PaginatedListResponse<>(dtos).getResponse();
    }

    /**
     * Batch lookup: the URIs travel in the JSON body (a GET with a list of URIs in the query string breaks on long
     * lists; the GET variants of other APIs are deprecated, never add a new one).
     */
    @POST
    @Path("by_uris")
    @ApiOperation("Get widgets by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses({
        @ApiResponse(code = 200, message = "Return widgets", response = WidgetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Widget not found (if any provided URI is not found)", response = ErrorDTO.class)
    })
    public Response getWidgetsByURIs(
            @ApiParam(value = "Widget URIs", required = true) @NotNull List<URI> uris
    ) throws Exception {
        List<WidgetModel> models = new WidgetDAO(sparql).getList(uris, currentUser);

        if (models.isEmpty()) {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Widgets not found",
                    "Unknown widget URIs"
            ).getResponse();
        }

        List<WidgetDTO> dtos = models.stream().map(WidgetDTO::getDTOFromModel).toList();
        return new PaginatedListResponse<>(dtos).getResponse();
    }
}
