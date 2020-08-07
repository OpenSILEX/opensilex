//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.quality;

import io.swagger.annotations.*;

import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_READ_ID;
import static org.opensilex.core.variable.api.VariableAPI.CREDENTIAL_VARIABLE_READ_LABEL_KEY;

import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.dal.QualityModel;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.NamedResourcePaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path("/core/variable/quality")
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class QualityAPI {

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @Path("create")
    @ApiOperation("Create a quality")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a Quality", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A Quality with the same URI already exists", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )
    public Response createQuality(
            @ApiParam("Quality description") @Valid QualityCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
            QualityModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Quality already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("update")
    @ApiOperation("Update a quality")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Quality updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Invalid or unknown quality URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )
    public Response updateQuality(
            @ApiParam("Quality description") @Valid QualityUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);

        QualityModel model = dto.newModel();
        dao.update(model);
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a quality")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Quality deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Quality URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQuality(
            @ApiParam(value = "Quality URI", example = "http://opensilex.dev/set/variables/quality/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a quality")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Quality retrieved", response = QualityGetDTO.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response getQuality(
            @ApiParam(value = "Quality URI", example = "http://opensilex.dev/set/variables/quality/Height", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        QualityModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    QualityGetDTO.fromModel(model)
            ).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("search")
    @ApiOperation("Search entities corresponding to given criteria")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_READ_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Quality list", response = NamedResourceDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchQualities(
            @ApiParam(value = "Name regex pattern", example = "height") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        ListWithPagination<QualityModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );
        return new NamedResourcePaginatedListResponse<>(resultList).getResponse();
    }
}
