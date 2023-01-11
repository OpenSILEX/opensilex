//******************************************************************************
//                          DocumentAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import io.swagger.annotations.*;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
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
import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * @author Fernandez Emilie
 */
@Api(DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID)
@Path("/core/documents")
@ApiCredentialGroup(
        groupId = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID,
        groupLabelKey = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY
)
public class DocumentAPI {

    public static final String CREDENTIAL_DOCUMENT_GROUP_ID = "Documents";
    public static final String CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY = "credential-groups.documents";

    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_ID = "document-modification";
    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_DOCUMENT_DELETE_ID = "document-delete";
    public static final String CREDENTIAL_DOCUMENT_DELETE_LABEL_KEY = "credential.default.delete";

    protected static final String DOCUMENT_EXAMPLE_URI = "http://opensilex/set/documents/d21";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

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
    @ApiOperation(value = "Add a document", 
    notes = "{ uri: http://opensilex.dev/set/documents#ProtocolExperimental, identifier: doi:10.1340/309registries, rdf_type: http://www.opensilex.org/vocabulary/oeso#ScientificDocument, title: title, date: 2020-06-01, description: description, targets: http://opensilex.dev/opensilex/id/variables/v001, authors: Author name, language: fr, format: jpg, deprecated: false, keywords: keywords}")
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
            @ApiParam(value = "File description with metadata", required =  true, type="string") @NotNull @Valid @FormDataParam("description") DocumentCreationDTO docDto, 
            @ApiParam(value = "file", type = "file") @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileDetail
        ) throws Exception {
            DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
            try {
                DocumentModel documentModel = docDto.newModel();

                if (file != null && (file.length() == 0 || file.length() >= 104857600)) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "Bad file",
                            "Empty or too large file size:  0 MB < filesize < 100 MB"
                    ).getResponse();
                }

                if (file != null) {
                    String format = FilenameUtils.getExtension(fileDetail.getFileName());
                    documentModel.setFormat(format);
                } else if (docDto.getSource() == null) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "No file or source",
                            "A document should have a file or a source, none was provided"
                    ).getResponse();
                }

                if(Boolean.toString(docDto.getDeprecated()) == null){
                    documentModel.setDeprecated("false");
                }

                boolean isType = documentDAO.isDocumentType(docDto.getType()); 
                if (!isType) {
                    // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "rdfType doesn't exist in the ontology",
                            "wrong rdfType: " + docDto.getType().toString()
                    ).getResponse();
                }

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
     * @param uri Document URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link DocumentGetDTO}
     */
    @GET
    @Path("{uri}/description")
    @ApiOperation("Get document's description")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document retrieved", response = DocumentGetDTO.class),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    public Response getDocumentMetadata(
            @ApiParam(value = "Document URI", example = "http://opensilex.dev/set/documents/ZA17", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
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
     * @param uri Document URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link DocumentGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get document")
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
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
        DocumentModel metadata = documentDAO.getMetadata(uri, currentUser);

        if (metadata == null) {
            throw new NotFoundException("No metadata was found for the document");
        }

        if (metadata.getSource() != null) {
            throw new NotFoundException("The document has no attached file, but a source URL is present : " + metadata.getSource().toString());
        }

        try {
            byte[] file = documentDAO.getFile(uri);
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                            .build();  
        } 
        catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
    }

    /**
     * Updates document metadata.
     * @param docDto 
     * @return Response the request result
     */
    @PUT
    @ApiOperation(value = "Update document's description")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document's metadata updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Document URI not found", response = ErrorResponse.class)
    })
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DOCUMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDocument(
        @ApiParam(value = "description", required =  true, type = "string") @NotNull @Valid @FormDataParam("description") DocumentCreationDTO docDto
        ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
        DocumentModel documentModel = docDto.newModel();

        boolean isType = documentDAO.isDocumentType(docDto.getType()); 
        if (!isType) {
            // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "wrong rdfType: " + docDto.getType().toString()
            ).getResponse();
        }

        documentDAO.update(documentModel, currentUser);
        return new ObjectUriResponse(Response.Status.OK, documentModel.getUri()).getResponse();
    }

    /**
     * Remove a document
     * @param uri document uri
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted Document {@link URI}
     */
    @DELETE
    @Path("{uri}")
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
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
        documentDAO.delete(uri, currentUser);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Search documents
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     */
    @GET
    @ApiOperation("Search documents")
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Document list", response = DocumentGetDTO.class, responseContainer = "List")
    })
    public Response searchDocuments(
            @ApiParam(value = "Search by type", example = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument") @QueryParam("rdf_type") URI type,
            @ApiParam(value = "Regex pattern for filtering list by title", example = "experimental_protocol_3") @QueryParam("title") String title,
            @ApiParam(value = "Regex pattern for filtering list by date", example = "2020") @QueryParam("date") String date,
            @ApiParam(value = "Search by targets", example = "dev-expe:za17") @QueryParam("targets") URI targets,
            @ApiParam(value = "Regex pattern for filtering list by author", example = "Firstname Lastname") @QueryParam("authors") String authors,
            @ApiParam(value = "Regex pattern for filtering list by keyword", example = "keyword") @QueryParam("keyword") String subject,
            @ApiParam(value = "Regex pattern for filtering list by keyword or title", example = "keyword or title") @QueryParam("multiple") String multiple,
            @ApiParam(value = "Search deprecated file", example = "true") @QueryParam("deprecated") String deprecated,
            @ApiParam(value = "List of fields to sort as an array of fieldTitle=asc|desc", example = "date=asc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DocumentDAO documentDAO = new DocumentDAO(sparql, nosql, fs);
        ListWithPagination<DocumentModel> resultList = documentDAO.search(
                currentUser,
                type,
                title,
                date,
                targets,
                authors,
                subject,
                multiple,
                deprecated,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<DocumentGetDTO> resultDTOList = resultList.convert(DocumentGetDTO.class, DocumentGetDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

}
