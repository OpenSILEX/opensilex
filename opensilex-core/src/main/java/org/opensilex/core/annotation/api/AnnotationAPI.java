//******************************************************************************
//                          AnnotationAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import io.swagger.annotations.*;
import org.opensilex.core.annotation.dal.AnnotationDao;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.NamedResourcePaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@Api(AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_ID)
@Path("/core/annotation")
@ApiCredentialGroup(
        groupId = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_ID,
        groupLabelKey = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_LABEL_KEY
)
public class AnnotationAPI {

    public static final String CREDENTIAL_ANNOTATION_GROUP_ID = "Annotations";
    public static final String CREDENTIAL_ANNOTATION_GROUP_LABEL_KEY = "credential-groups.annotations";

    public static final String CREDENTIAL_ANNOTATION_MODIFICATION_ID = "annotation-modification";
    public static final String CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY = "credential.annotation.modification";

    public static final String CREDENTIAL_ANNOTATION_DELETE_ID = "annotation-delete";
    public static final String CREDENTIAL_ANNOTATION_DELETE_LABEL_KEY = "credential.annotation.delete";

    public static final String CREDENTIAL_ANNOTATION_READ_ID = "annotation-read";
    public static final String CREDENTIAL_ANNOTATION_READ_LABEL_KEY = "credential.annotation.read";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    @POST
    @Path("create")
    @ApiOperation("Create an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create an annotation", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An annotation with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAnnotation(@Valid AnnotationCreationDTO dto) throws Exception {

        try {
            AnnotationDao dao = new AnnotationDao(sparql);
            AnnotationModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Annotation already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @PUT
    @Path("update")
    @ApiOperation("Update an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return updated annotation", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Annotation URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAnnotation(@ApiParam("Annotation description") @Valid AnnotationUpdateDTO dto) throws Exception {

        AnnotationDao dao = new AnnotationDao(sparql);
        dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, dto.getUri()).getResponse();
    }

    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Annotation deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Annotation URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAnnotation(
            @ApiParam(value = "Annotation URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        AnnotationDao dao = new AnnotationDao(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("get/{uri}")
    @ApiOperation("Get an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_READ_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Annotation retrieved", response = AnnotationGetDTO.class),
            @ApiResponse(code = 404, message = "Annotation URI not found", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnnotation(
            @ApiParam(value = "Event URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        AnnotationDao dao = new AnnotationDao(sparql);
        AnnotationModel model = dao.get(uri, currentUser);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }

        AnnotationGetDTO dto = new AnnotationGetDTO(model);
        return new SingleObjectResponse<>(dto).getResponse();
    }

    @GET
    @Path("motivations/search")
    @ApiOperation("Search motivations")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_READ_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return motivation list", response = MotivationGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMotivations(
            @ApiParam(value = "Motivation name regex pattern", example = "description") @QueryParam("bodyValue") String stringPattern,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {

        AnnotationDao dao = new AnnotationDao(sparql);

        ListWithPagination<MotivationModel> resultList = dao.searchMotivations(
                stringPattern,
                currentUser.getLanguage(),
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<MotivationGetDTO> resultDTOList = resultList.convert(
                MotivationGetDTO.class,
                MotivationGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("search")
    @ApiOperation("Search annotations")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_READ_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_READ_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return annotation list", response = AnnotationGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAnnotations(
            @ApiParam(value = "Annotation body value regex pattern", example = "The pest attack") @QueryParam("bodyValue") String bodyValuePattern,
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI target,
            @ApiParam(value = "Motivation URI", example = "http://www.w3.org/ns/oa#describing") @QueryParam("motivation") URI motivation,
            @ApiParam(value = "Search by creator", example = "http://opensilex.dev/users#Admin.OpenSilex") @QueryParam("creator") URI creator,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize
    ) throws Exception {

        AnnotationDao dao = new AnnotationDao(sparql);

        ListWithPagination<AnnotationModel> resultList = dao.search(
                bodyValuePattern,
                target,
                motivation,
                creator,
                currentUser.getLanguage(),
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<AnnotationGetDTO> resultDTOList = resultList.convert(
                AnnotationGetDTO.class,
                AnnotationGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();

    }


}
