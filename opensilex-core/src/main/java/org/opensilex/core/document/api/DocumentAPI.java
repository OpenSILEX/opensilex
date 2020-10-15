//******************************************************************************
//                          DocumentAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import java.io.InputStream;
import io.swagger.annotations.*;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;

import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.nio.file.Paths;
import org.opensilex.fs.service.FileStorageService;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.sql.Timestamp;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.URIDeserializer;
import java.io.*; 

/**
 * @author Fernandez Emilie
 */
@Api(DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID)
@Path("/core/document")
@ApiCredentialGroup(
        groupId = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID,
        groupLabelKey = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY
)
public class DocumentAPI {

    public static final String CREDENTIAL_DOCUMENT_GROUP_ID = "Documents";
    public static final String CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY = "credential-groups.documents";

    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_ID = "document-modification";
    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY = "credential.document.modification";

    public static final String CREDENTIAL_DOCUMENT_DELETE_ID = "document-delete";
    public static final String CREDENTIAL_DOCUMENT_DELETE_LABEL_KEY = "credential.document.delete";

    protected static final String DOCUMENT_EXAMPLE_URI = "http://opensilex/set/documents/d21";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private FileStorageService fs;

       /**
     * Create Document
     * @param file
     * @param fileDetail
     * @param docDto
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the created Document {@link URI}
     */
    @POST
    @Path("create")
    @ApiOperation("Post a file")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DOCUMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Document Inserted", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A document with the same URI already exists", response = ErrorResponse.class)
    })

        public Response createDocument(
            @ApiParam(value = "description", required =  true, type = "string") @NotNull @Valid @FormDataParam("description") DocumentCreationDTO docDto, 
            @ApiParam(value = "file", type = "file") @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail
        ) throws Exception {
            DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
            try {
                DocumentModel documentModel = docDto.newModel();
                if (file == null || fileDetail.getSize() == 0 ){
                    return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "Bad file ",
                        "Empty file size or url field"
                ).getResponse();
                }
                if(Boolean.toString(docDto.getDeprecated()) == null){
                    documentModel.setDeprecated("false");
                }
                if(docDto.getCreator() == null){
                    documentModel.setCreator(currentUser.getUri());                
                }

                String format = FilenameUtils.getExtension(fileDetail.getFileName());
                documentModel.setFormat(format); 
                documentDAO.create(documentModel, file);
                return new ObjectUriResponse(Response.Status.CREATED, documentModel.getUri()).getResponse();

            } catch (SPARQLAlreadyExistingUriException e) {
                // Return error response 409 - CONFLICT if document URI already exists
                return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Document already exists",
                    "Duplicated URI: " + e.getUri()
                ).getResponse();
            }
    }

    /**
     * @param Uri Document URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link DocumentGetDTO}
     */
    @GET
    @Path("getMetadata/{uri}")
    @ApiOperation("Get document metadata by URI")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document retrieved", response = DocumentGetDTO.class),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    public Response getDocumentMetadata(
            @ApiParam(value = "Document URI", example = "http://opensilex.dev/set/documents/ZA17", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
        DocumentModel documentModel = documentDAO.getMetadata(uri, currentUser);

        if (documentModel != null) {
            return new SingleObjectResponse<>(DocumentGetDTO.fromModel(documentModel)).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Document not found",
                    "Unknown Document URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * @param Uri Document URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link DocumentGetDTO}
     */
    @GET
    @Path("getFile/{uri}")
    @ApiOperation("Get document file by URI")
    @ApiProtected
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document retrieved"),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    public Response getDocumentFile(
            @ApiParam(value = "Document URI", example = "http://opensilex.dev/set/documents/ZA17", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        uri = new URI(URIDeserializer.getExpandedURI(uri.toString()));
        DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
        byte[] file = documentDAO.getFile(uri);
        if (file != null) {
             return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                            .build();  
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "File not found",
            "Unknown file URI: " + uri.toString()).getResponse();
        }
    }

    /**
     * Updates document metadata.
     * @param docDto 
     * @return Response the request result
     */
    @PUT
    @Path("update")
    @ApiOperation(value = "Update document metadata")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document's metadata updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDocument(
        @ApiParam(value = "description", required =  true, type = "string") @NotNull @Valid @FormDataParam("description") DocumentCreationDTO docDto
        ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
        DocumentModel documentModel = docDto.newModel();
        documentDAO.update(documentModel, currentUser);
        return new ObjectUriResponse(Response.Status.OK, documentModel.getUri()).getResponse();
    }

    /**
     * Remove a document
     * @param uri document uri
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted Document {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a document")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DOCUMENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_DOCUMENT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    public Response deleteDocument(
            @ApiParam(value = "Document URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        uri = new URI(URIDeserializer.getExpandedURI(uri.toString()));
        DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
        documentDAO.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Search documents
     * @param name
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     */
    @GET
    @Path("search")
    @ApiOperation("Search Documents")
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Document list", response = DocumentGetDTO.class, responseContainer = "List")
    })
    public Response searchDocuments(
            @ApiParam(value = "Search by name", example = "experimental_protocol_3") @QueryParam("name") String name,
            @ApiParam(value = "Search file deprecated", example = "true") @QueryParam("deprecated") String deprecated,
            @ApiParam(value = "Search by subject", example = "keyword") @QueryParam("subject") String subject,
            @ApiParam(value = "Search by date", example = "2020") @QueryParam("date") String date,
            @ApiParam(value = "Search by type", example = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument") @QueryParam("type") URI rdfType,
            @ApiParam(value = "Search by creator", example = "dev-usr:admin.opensilex") @QueryParam("user") URI user,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, fs);
        ListWithPagination<DocumentModel> resultList = documentDAO.search(
                name,
                deprecated,
                subject,
                date,
                rdfType,
                user,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<DocumentGetDTO> resultDTOList = resultList.convert(DocumentGetDTO.class, DocumentGetDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

}
