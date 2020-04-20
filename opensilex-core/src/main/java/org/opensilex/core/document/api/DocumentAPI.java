/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.document.api;

//******************************************************************************
//                          ExperimentAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************


import io.swagger.annotations.*;
import java.io.InputStream;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.document.dal.DocumentDAO;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.nosql.mongodb.MongoDBConnection;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */


@Api(DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID)
@Path("/core/document")
@ApiCredentialGroup(
        groupId = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_ID,
        groupLabelKey = DocumentAPI.CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY
)
public class DocumentAPI {

    public static final String CREDENTIAL_DOCUMENT_GROUP_ID = "Experiments";
    public static final String CREDENTIAL_DOCUMENT_GROUP_LABEL_KEY = "credential-groups.documents";

    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_ID = "document-modification";
    public static final String CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY = "credential.document.modification";

    public static final String CREDENTIAL_DOCUMENT_READ_ID = "document-read";
    public static final String CREDENTIAL_DOCUMENT_READ_LABEL_KEY = "credential.document.read";

    public static final String CREDENTIAL_DOCUMENT_DELETE_ID = "document-delete";
    public static final String CREDENTIAL_DOCUMENT_DELETE_LABEL_KEY = "credential.document.delete";

    protected static final String DOCUMENT_EXAMPLE_URI = "http://opensilex/set/documents/ZA17";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBConnection nosql;
  
    @Inject
    private AuthenticationService authentication;

    /**
     * Create an Experiment
     *
     * @param businessUnitName
     * @param sourceSystemName
     * @param inputStream
     * @param fileDetail
     * @param xpDto the Experiment to create
     * @param <error>
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     */
    @POST
    @Path("create")
    @ApiOperation("Upload file")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_DOCUMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DOCUMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an document", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An document with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)}
    )

    public Response uploadFile(
            @QueryParam("creationDate") @ApiParam(required = true) String label,
            @QueryParam("creator") @ApiParam(required = true, value = "creator") String creator,
            @ApiParam(value = "file to upload", required = true) @FormDataParam("file") InputStream inputStream,
            @ApiParam(value = "file detail", required = true) @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        try { 
            DocumentDAO documentDAO = new DocumentDAO(sparql,nosql);
            DocumentModel filemodel =  new DocumentModel();
            filemodel.setLabel(fileDetail.getName());
            DocumentModel createdFileModel = documentDAO.create(filemodel,inputStream);
            return new ObjectUriResponse(Response.Status.CREATED, createdFileModel.getUri()).getResponse();

//        } catch (SPARQLAlreadyExistingUriException e) {
//            return new ErrorResponse(Response.Status.CONFLICT, "Experiment already exists", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    } 

}
