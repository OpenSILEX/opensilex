//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.characteristic;

import io.swagger.annotations.*;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.CharacteristicModel;
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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensilex.core.variable.api.VariableAPI.*;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(CharacteristicAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class CharacteristicAPI {

    public static final String PATH = "/core/characteristics";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Add a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A characteristic is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A characteristic with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createCharacteristic(
            @ApiParam("Characteristic description") @Valid CharacteristicCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
            CharacteristicModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Characteristic already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a characteristic")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Characteristic retrieved", response = CharacteristicDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown characteristic URI", response = ErrorResponse.class)
    })
    public Response getCharacteristic(
            @ApiParam(value = "Characteristic URI", example = "http://opensilex.dev/set/variables/characteristic/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        CharacteristicModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(new CharacteristicDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }
    
    
    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed characteristics by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return characteristics", response = CharacteristicDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Characteristic not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getCharacteristicsByURIs(
            @ApiParam(value = "Characteristics URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty List<URI> uris
    ) throws Exception {
        
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);

        try {
            List<CharacteristicDetailsDTO> resultDTOList = dao.getList(uris)
                    .stream()
                    .map(CharacteristicDetailsDTO::new)
                    .collect(Collectors.toList());

            return new PaginatedListResponse<>(resultDTOList).getResponse();

        }catch (SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.NOT_FOUND, "Characteristics not found", e.getStrUris()).getResponse();
        }
    }
    
    
    @PUT
    @ApiOperation("Update a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Characteristic updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown characteristic URI", response = ErrorResponse.class)
    })
    public Response updateCharacteristic(
            @ApiParam("Characteristic description") @Valid CharacteristicUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);

        CharacteristicModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a characteristic")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Characteristic deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown characteristic URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCharacteristic(
            @ApiParam(value = "Characteristic URI", example = "http://opensilex.dev/set/variables/characteristic/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }


    @GET
    @ApiOperation("Search characteristics by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return characteristic list", response = CharacteristicGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCharacteristics(
            @ApiParam(value = "Name (regex)", example = "Height") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<CharacteristicModel> dao = new BaseVariableDAO<>(CharacteristicModel.class, sparql);
        ListWithPagination<CharacteristicModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<CharacteristicGetDTO> resultDTOList = resultList.convert(
                CharacteristicGetDTO.class,
                CharacteristicGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
