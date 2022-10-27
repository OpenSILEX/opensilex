//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
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

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.VariableAPI;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.InterestEntityModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * @author Hamza IKIOU
 */

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(InterestEntityAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class InterestEntityAPI {
    
    public static final String PATH = "/core/entities_of_interest";
    
    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;
    
    @POST
    @ApiOperation("Add an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "An entity of interest is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An entity of interest with the same URI already exists", response = ErrorResponse.class),
    })
    public Response createInterestEntity(
            @ApiParam("Entity of interest description") @Valid InterestEntityCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
            InterestEntityModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Entity of interest already exists", duplicateUriException.getMessage()).getResponse();
        }
    }
    
    @GET
    @Path("{uri}")
    @ApiOperation("Get an entity of interest")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity of interest retrieved", response = InterestEntityDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown entity of interest URI", response = ErrorResponse.class)
    })
    public Response getInterestEntity(
            @ApiParam(value = "Entity of interest URI", example = "http://opensilex.dev/set/variables/entity_of_interest/Plot", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
        InterestEntityModel model = dao.get(uri);
        if (model != null) {
            return new SingleObjectResponse<>(new InterestEntityDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed entities of interest by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return entities of interest", response = InterestEntityDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Entity of interest not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getInterestEntitiesByURIs(
            @ApiParam(value = "Entities of interest URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        
        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
        List<InterestEntityModel> models = dao.getList(uris);

        if (!models.isEmpty()) {
            List<InterestEntityDetailsDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(new InterestEntityDetailsDTO(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(Response.Status.NOT_FOUND, "Entities of interest not found", "Unknown entity of interest URIs").getResponse();
        }
    }

    
    @PUT
    @ApiOperation("Update an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity of interest updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown entity of interest URI", response = ErrorResponse.class)
    })
    public Response updateInterestEntity(
            @ApiParam("Entity of interest description") @Valid InterestEntityUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);

        InterestEntityModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an entity of interest")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity of interest deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown entity of interest URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteInterestEntity(
            @ApiParam(value = "Entity of interest URI", example = "http://opensilex.dev/set/variables/entity_of_interest/Plot", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<InterestEntityModel> dao = new BaseVariableDAO<>(InterestEntityModel.class, sparql);
        dao.delete(uri, Oeso.hasEntityOfInterest);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }
    
    @GET
    @ApiOperation("Search entities of interest by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return entities of interest", response = InterestEntityGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchInterestEntity(
            @ApiParam(value = "Name (regex)", example = "plot") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

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
}
