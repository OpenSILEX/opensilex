//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

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
import org.opensilex.core.variable.dal.InterestEntityModel;
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
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

import static org.opensilex.core.variable.api.VariableAPI.*;

/**
 * @author Hamza IKIOU
 */

@Tag(name = CREDENTIAL_VARIABLE_GROUP_ID)
@Path(InterestEntityAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class InterestEntityAPI {
    
    public static final String PATH = "/core/entities_of_interest";
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
    @Operation(summary = "Add an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "An entity of interest is created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "An entity of interest with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response createInterestEntity(
            @Parameter(description = "Entity of interest description") @Valid InterestEntityCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
            InterestEntityModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new CreatedUriResponse(shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Entity of interest already exists", duplicateUriException.getMessage()).getResponse();
        }
    }
    
    @GET
    @Path("{uri}")
    @Operation(summary = "Get an entity of interest")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity of interest retrieved", content = @Content(schema = @Schema(implementation = InterestEntityDetailsDTO.class))),
            @ApiResponse(responseCode = "404", description = "Unknown entity of interest URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getInterestEntity(
            @Parameter(description = "Entity of interest URI", example = "http://opensilex.dev/set/variables/entity_of_interest/Plot", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
        InterestEntityModel model = dao.get(uri);
        if (model != null) {
            InterestEntityDetailsDTO dto = new InterestEntityDetailsDTO(model);
            if (Objects.nonNull(model.getPublisher())) {
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path(InterestEntityAPI.GET_BY_URIS_PATH)
    @Operation(summary = "Get detailed entities of interest by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return entities of interest", content = @Content(array = @ArraySchema(schema = @Schema(implementation = InterestEntityDetailsDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Entity of interest not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getInterestEntitiesByURIs(
            @Parameter(description = "Entities of interest URIs", required = true) @QueryParam(InterestEntityAPI.GET_BY_URIS_URI_PARAM) @NotNull List<URI> uris,
            @Parameter(description = "Shared resource instance") @QueryParam(InterestEntityAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
    ) throws Exception {
        if (sharedResourceInstance == null) {
            BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
            List<InterestEntityModel> models = dao.getList(uris);

            if (!models.isEmpty()) {
                List<InterestEntityDetailsDTO> resultDTOList = new ArrayList<>(models.size());
                models.forEach(result -> {
                    resultDTOList.add(new InterestEntityDetailsDTO(result));
                });

                return new PaginatedListResponse<>(resultDTOList).getResponse();
            } else {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Entities of interest not found", "Unknown entity of interest URIs").getResponse();
            }
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        ListWithPagination<InterestEntityDetailsDTO> detailsList = service.getListByURI(Paths.get(InterestEntityAPI.PATH, InterestEntityAPI.GET_BY_URIS_PATH).toString(),
                InterestEntityAPI.GET_BY_URIS_URI_PARAM,
                uris, InterestEntityDetailsDTO.class);
        return new PaginatedListResponse<>(detailsList).getResponse();
    }


    @PUT
    @Operation(summary = "Update an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity of interest updated", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown entity of interest URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateInterestEntity(
            @Parameter(description = "Entity of interest description") @Valid InterestEntityUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);

        InterestEntityModel model = dto.newModel();
        dao.update(model, currentUser);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }
    
    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity of interest deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown entity of interest URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteInterestEntity(
            @Parameter(description = "Entity of interest URI", example = "http://opensilex.dev/set/variables/entity_of_interest/Plot", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
        dao.delete(uri, Oeso.hasEntityOfInterest, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }
    
    @GET
    @Operation(summary = "Search entities of interest by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return entities of interest", content = @Content(array = @ArraySchema(schema = @Schema(implementation = InterestEntityGetDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchInterestEntity(
            @Parameter(description = "Name (regex)", example = "plot") @QueryParam("name") String namePattern ,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize,
            @Parameter(description = "Shared resource instance") @QueryParam(InterestEntityAPI.SHARED_RESOURCE_INSTANCE_PARAM) URI sharedResourceInstance
            ) throws Exception {
        if (sharedResourceInstance == null) {
            BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
            ListWithPagination<InterestEntityModel> resultList = dao.search(
                    namePattern,
                    orderByList,
                    page,
                    pageSize,
                    currentUser.getLanguage()
            );

            ListWithPagination<InterestEntityGetDTO> resultDTOList = resultList.convert(
                    InterestEntityGetDTO.class,
                    InterestEntityGetDTO::new
            );
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }

        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage()
        );

        Map<String, String[]> searchParams = new HashMap<>(httpRequest.getParameterMap());
        searchParams.remove(InterestEntityAPI.SHARED_RESOURCE_INSTANCE_PARAM);
        return new PaginatedListResponse<>(service.search(InterestEntityAPI.PATH, searchParams, InterestEntityGetDTO.class))
                .getResponse();
    }
}
