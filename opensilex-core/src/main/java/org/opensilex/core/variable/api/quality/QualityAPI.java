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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_READ_ID;
import static org.opensilex.core.variable.api.variable.VariableAPI.CREDENTIAL_VARIABLE_READ_LABEL_KEY;

import org.opensilex.core.variable.api.variable.VariableAPI;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.core.variable.dal.quality.QualityModel;
import org.opensilex.core.variable.dal.variable.BaseVariableDAO;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
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
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Quality already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    @PUT
    @Path("{uri}")
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
            @ApiResponse(code = 400, message = "Invalid or unknown Quality URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )
    public Response updateQuality(
            @ApiParam(value = "Quality URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam("Quality description") @Valid QualityUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);

        QualityModel model = dao.get(uri);
        if (model != null) {
            dao.update(dto.defineModel(model));
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Quality not found",
                    "Unknown quality URI: " + uri
            ).getResponse();
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a quality")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQuality(
            @ApiParam(value = "Quality URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
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
            @ApiParam(value = "Quality URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        QualityModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    QualityGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Quality not found",
                    "Unknown method URI: " + uri.toString()
            ).getResponse();
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
            @ApiResponse(code = 200, message = "Return Quality list", response = QualityGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchQualities(
            @ApiParam(value = "Name regex pattern") @QueryParam("name") String namePattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {
        BaseVariableDAO<QualityModel> dao = new BaseVariableDAO<>(QualityModel.class, sparql);
        ListWithPagination<QualityModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize
        );
        ListWithPagination<QualityGetDTO> resultDTOList = resultList.convert(
                QualityGetDTO.class,
                QualityGetDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
