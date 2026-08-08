//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.characteristic;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.opensilex.core.CoreModule;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opensilex.core.variable.api.VariableAPI.*;

@Tag(name = CREDENTIAL_VARIABLE_GROUP_ID)
@Path(CharacteristicAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class CharacteristicAPI {

    public static final String PATH = "/core/characteristics";
    public static final String GET_BY_URIS_PATH = "by_uris";
    public static final String GET_BY_URIS_URI_PARAM = "uris";
    private static final String SHARED_RESOURCE_INSTANCE_PARAM = "sharedResourceInstance";

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @Operation(summary = "Add a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "A characteristic is created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "A characteristic with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createCharacteristic(
            @Parameter(description = "Characteristic description") @Valid CharacteristicCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
            CharacteristicModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Characteristic already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @Operation(summary = "Get a characteristic")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Characteristic retrieved", content = @Content(schema = @Schema(implementation = CharacteristicDetailsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Unknown characteristic URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getCharacteristic(
            @Parameter(description = "Characteristic URI", example = "http://opensilex.dev/set/variables/characteristic/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        CharacteristicModel model = dao.get(uri);

        if (model != null) {
            CharacteristicDetailsDTO dto = new CharacteristicDetailsDTO(model);
            if (Objects.nonNull(model.getPublisher())) {
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }
    
    
    @GET
    @Path(CharacteristicAPI.GET_BY_URIS_PATH)
    @Operation(summary = "Get detailed characteristics by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return characteristics", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CharacteristicDetailsDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Characteristic not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getCharacteristicsByURIs(
            @Parameter(description = "Characteristics URIs", required = true) @QueryParam(CharacteristicAPI.GET_BY_URIS_URI_PARAM) @NotNull @NotEmpty List<URI> uris,
            @Parameter(description = "Shared resource instance") @QueryParam(CharacteristicAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
            BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);

            try {
                List<CharacteristicDetailsDTO> resultDTOList = dao.getList(uris)
                        .stream()
                        .map(CharacteristicDetailsDTO::new)
                        .collect(Collectors.toList());

                return new PaginatedListResponse<>(resultDTOList).getResponse();

            } catch (SPARQLInvalidUriListException e) {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Characteristics not found", e.getStrUris()).getResponse();
            }
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        ListWithPagination<CharacteristicDetailsDTO> detailsList = service.getListByURI(Paths.get(CharacteristicAPI.PATH, CharacteristicAPI.GET_BY_URIS_PATH).toString(),
                CharacteristicAPI.GET_BY_URIS_URI_PARAM,
                uris, CharacteristicDetailsDTO.class);
        return new PaginatedListResponse<>(detailsList).getResponse();
    }
    
    
    @PUT
    @Operation(summary = "Update a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Characteristic updated", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown characteristic URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateCharacteristic(
            @Parameter(description = "Characteristic description") @Valid CharacteristicUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);

        CharacteristicModel model = dto.newModel();
        dao.update(model, currentUser);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Characteristic deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown characteristic URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCharacteristic(
            @Parameter(description = "Characteristic URI", example = "http://opensilex.dev/set/variables/characteristic/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        dao.delete(uri, Oeso.hasCharacteristic, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }


    @GET
    @Operation(summary = "Search characteristics by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return characteristic list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CharacteristicGetDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCharacteristics(
            @Parameter(description = "Name (regex)", example = "Height") @QueryParam("name") String namePattern ,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize,
            @Parameter(description = "Shared resource instance") @QueryParam(CharacteristicAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
            ) throws Exception {
        if (sharedResourceInstance == null) {
            BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
            ListWithPagination<CharacteristicModel> resultList = dao.search(
                    namePattern,
                    orderByList,
                    page,
                    pageSize,
                    currentUser.getLanguage()
            );

            ListWithPagination<CharacteristicGetDTO> resultDTOList = resultList.convert(
                    CharacteristicGetDTO.class,
                    CharacteristicGetDTO::new
            );
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        Map<String, String[]> searchParams = new HashMap<>(httpRequest.getParameterMap());
        searchParams.remove(CharacteristicAPI.SHARED_RESOURCE_INSTANCE_PARAM);
        return new PaginatedListResponse<>(service.search(CharacteristicAPI.PATH, searchParams, CharacteristicGetDTO.class))
                .getResponse();
    }
}
