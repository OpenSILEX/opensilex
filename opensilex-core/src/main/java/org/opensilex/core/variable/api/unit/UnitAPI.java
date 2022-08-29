//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.unit;

import io.swagger.annotations.*;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.UnitModel;
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
@Path(UnitAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class UnitAPI {

    public static final String PATH = "/core/units";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Add an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "An unit is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An unit with the same URI already exists", response = ErrorResponse.class),
    })
    public Response createUnit(
            @ApiParam("Unit description") @Valid UnitCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
            UnitModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Unit already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an unit")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit retrieved", response = UnitDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    public Response getUnit(
            @ApiParam(value = "Unit URI", example = "http://opensilex.dev/set/variables/unit/Centimeter", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
        UnitModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(new UnitDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed units by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return units", response = UnitDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Unit not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getUnitsByURIs(
            @ApiParam(value = "Units URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);

        try {
            List<UnitDetailsDTO> resultDTOList = dao.getList(uris)
                    .stream()
                    .map(UnitDetailsDTO::new)
                    .collect(Collectors.toList());

            return new PaginatedListResponse<>(resultDTOList).getResponse();

        }catch (SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.NOT_FOUND, "Units not found", e.getStrUris()).getResponse();
        }

    }
    
    
    @PUT
    @ApiOperation("Update an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    public Response updateUnit(
            @ApiParam("Unit description") @Valid UnitUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);

        UnitModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUnit(
            @ApiParam(value = "Unit URI", example = "http://opensilex.dev/set/variables/unit/Centimeter", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }


    @GET
    @ApiOperation("Search units by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Unit list", response = UnitGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUnits(
            @ApiParam(value = "Name (regex)", example = "Centimeter") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
        ListWithPagination<UnitModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<UnitGetDTO> resultDTOList = resultList.convert(
                UnitGetDTO.class,
                UnitGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();    }
}
