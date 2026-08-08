//******************************************************************************
//                          AnnotationAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

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
import org.opensilex.core.annotation.dal.AnnotationDAO;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.nosql.mongodb.MongoDBService;
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

/**
 * @author Renaud COLIN
 */
@Tag(name = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_ID)
@Path("/core/annotations")
@ApiCredentialGroup(
        groupId = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_ID,
        groupLabelKey = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_LABEL_KEY
)
public class AnnotationAPI {

    public static final String CREDENTIAL_ANNOTATION_GROUP_ID = "Annotations";
    public static final String CREDENTIAL_ANNOTATION_GROUP_LABEL_KEY = "credential-groups.annotations";

    public static final String CREDENTIAL_ANNOTATION_MODIFICATION_ID = "annotation-modification";
    public static final String CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_ANNOTATION_DELETE_ID = "annotation-delete";
    public static final String CREDENTIAL_ANNOTATION_DELETE_LABEL_KEY = "credential.default.delete";

    @Inject
    private SPARQLService sparql;
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;

    @POST
    @Operation(summary = "Create an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "An annotation is created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "409", description = "An annotation with the same URI already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAnnotation(@Valid AnnotationCreationDTO dto) throws Exception {

        try {
            AnnotationDAO dao = new AnnotationDAO(sparql, nosql);
            AnnotationModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            return new CreatedUriResponse(model.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Annotation already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @PUT
    @Operation(summary = "Update an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Annotation created", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Unknown annotation URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAnnotation(@Parameter(description = "Annotation description") @Valid AnnotationUpdateDTO dto) throws Exception {

        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);
        dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, dto.getUri()).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete an annotation")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_ANNOTATION_DELETE_ID,
            credentialLabelKey = CREDENTIAL_ANNOTATION_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Annotation deleted", content = @Content(schema = @Schema(implementation = URI.class))),
            @ApiResponse(responseCode = "404", description = "Annotation URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAnnotation(
            @Parameter(description = "Annotation URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @Path("{uri}")
    @Operation(summary = "Get an annotation")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Annotation retrieved", content = @Content(schema = @Schema(implementation = AnnotationGetDTO.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User authenticated but not authorized to access this annotation", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Unknown annotation URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        public Response getAnnotation(
                @Parameter(description = "Event URI", example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415", required = true) @PathParam("uri") @NotNull URI uri
        ) throws Exception {
        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);
        
        // Check user access rights
        switch (dao.checkAccess(uri, currentUser)) {
                case UNAUTHORIZED:
                return new ErrorResponse(Response.Status.UNAUTHORIZED, "Unauthorized", "User is not authenticated").getResponse();
                case FORBIDDEN:
                return new ErrorResponse(Response.Status.FORBIDDEN, "Forbidden", "User does not have access to this annotation").getResponse();
                case NOT_FOUND:
                return new ErrorResponse(Response.Status.NOT_FOUND, "Annotation not found", "Unknown annotation URI: " + uri).getResponse();
        }
        
        AnnotationModel model = dao.get(uri, currentUser);
        
        if (model == null) {
                throw new NotFoundURIException(uri);
        }

        AnnotationGetDTO dto = new AnnotationGetDTO(model);
        return new SingleObjectResponse<>(dto).getResponse();
        }

    @GET
    @Path("/motivations")
    @Operation(summary = "Search motivations")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return motivations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MotivationGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMotivations(
            @Parameter(description = "Motivation name regex pattern", example = "describing") @QueryParam("name") String namePattern,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);

        ListWithPagination<MotivationModel> resultList = dao.searchMotivations(
                namePattern,
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
    @Operation(summary = "Search annotations")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return annotations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnnotationGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAnnotations(
            @Parameter(description = "Description (regex)", example = "The pest attack") @QueryParam("description") String descriptionPattern,
            @Parameter(description = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI target,
            @Parameter(description = "Motivation URI", example = "http://www.w3.org/ns/oa#describing") @QueryParam("motivation") URI motivation,
            @Parameter(description = "Author URI", example = "http://opensilex.dev/users#Admin.OpenSilex") @QueryParam("author") URI publisher,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "author=asc") @DefaultValue("created=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);

        ListWithPagination<AnnotationModel> resultList = dao.search(
                descriptionPattern,
                target,
                motivation,
                publisher,
                currentUser.getLanguage(),
                orderByList,
                page,
                pageSize,
                currentUser
        );

        ListWithPagination<AnnotationGetDTO> resultDTOList = resultList.convert(
                AnnotationGetDTO.class,
                AnnotationGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();

    }

    @GET
    @Path("count")
    @Operation(summary = "Count annotations")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the number of annotations associated to a given target", content = @Content(schema = @Schema(implementation = Integer.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countAnnotations(
            @Parameter(description = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") URI target) throws Exception {

        AnnotationDAO dao = new AnnotationDAO(sparql, nosql);
        int annotationCount = dao.countAnnotations(target, currentUser);

        return new SingleObjectResponse<>(annotationCount).getResponse();
    }

}
