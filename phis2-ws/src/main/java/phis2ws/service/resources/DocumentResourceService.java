//******************************************************************************
//                                       DocumentResourceService.java
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug, 2016
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************

package phis2ws.service.resources;

import com.jcraft.jsch.SftpException;
import com.twmacinta.util.MD5;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.manager.DAOFactory;
import phis2ws.service.dao.mongo.DocumentDaoMongo;
import phis2ws.service.dao.sesame.DocumentDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Contexts;
import phis2ws.service.resources.dto.DocumentMetadataDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.DocumentWaitingCheck;
import phis2ws.service.utils.FileUploader;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.ResourcesUtils;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormDocumentMetadata;
import phis2ws.service.view.brapi.form.ResponseFormDocumentType;
import phis2ws.service.view.brapi.form.ResponseFormGET;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.Document;
import phis2ws.service.resources.validation.interfaces.SortingValue;

/**
 * Represents the documents service.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Morgane Vidal] March, 2017 : no explanation
 * @update [Arnaud Charleroy] 04 September, 2018 : create automatically document directory if not
 * @update [Arnaud Charleroy] 07, September 2018 : add sort feature, query optimization (limit , offset, group_concat)
 *                                                 add comments and CONSTANTS to the code
 */
@Api("/documents")
@Path("/documents")
public class DocumentResourceService extends ResourceService {
    @Context
    UriInfo uri;
    
    final static Logger LOGGER = LoggerFactory.getLogger(DocumentResourceService.class);
    
    // Gère les annotations en attene
    public final static ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    // Deux Maps qui contiennent les informations sur les annotations en attentes
    public final static Map<String, Boolean> WAITING_ANNOT_FILE_CHECK = new HashMap<>();
    public final static Map<String, DocumentMetadataDTO> WAITING_ANNOT_INFORMATION = new HashMap<>();
    
    /**
     * Vérifie un ensemble d'annotation au format JSON
     *
     * @param headers Entête de la requête
     * @param documentsAnnotations documentsAnnotations
     * @return
     */
    @POST
    @ApiOperation(value = "Save a file", notes = DocumentationAnnotation.USER_ONLY_NOTES)
    @ApiResponses(value = {
        @ApiResponse(code = 202, message = "Metadata verified and correct", response = DocumentMetadataDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDocuments(@Context HttpHeaders headers,
            @ApiParam(value = "JSON Document metadata", required = true) @Valid List<DocumentMetadataDTO> documentsAnnotations) throws RepositoryException {
        AbstractResultForm postResponse;
        if (documentsAnnotations != null && !documentsAnnotations.isEmpty()) {
            //Insertion du document
            DocumentDaoSesame documentDao = new DocumentDaoSesame();
            documentDao.user = userSession.getUser();
            //Vérification des documentsAnnotations
            final POSTResultsReturn checkAnnots = documentDao.check(documentsAnnotations);
            
            if (checkAnnots.statusList == null) { //Les annotations ne sont pas bonnes
                postResponse = new ResponseFormPOST();
            } else if (checkAnnots.getDataState()) { // Si les annotations sont bonnes
                List<String> uriList = new ArrayList<>();
                Iterator<DocumentMetadataDTO> itdocsMetadata = documentsAnnotations.iterator();
                while (itdocsMetadata.hasNext()) {
                    DocumentMetadataDTO docsM = itdocsMetadata.next();
                    //Construction des URI
                    final UriBuilder uploadPath = uri.getBaseUriBuilder();
                    String name = new StringBuilder("document").append(ResourcesUtils.getUniqueID()).toString(); // docsM + idUni
                    final String docsUri = Contexts.DOCUMENTS.toString() + "/" + name;
                    final String uploadLink = uploadPath.path("documents").path("upload").queryParam("uri", docsUri).toString();
//                  //Ajout URI en attente
                    uriList.add(uploadLink);
                    WAITING_ANNOT_FILE_CHECK.put(docsUri, false); // fichier en attente
                    WAITING_ANNOT_INFORMATION.put(docsUri, docsM);
//                        //Lancement THREAD pour le fichier en attente
                    THREAD_POOL.submit(new DocumentWaitingCheck(docsUri));
                }
                final Status waitingTimeStatus = new Status("Timeout", StatusCodeMsg.INFO, " Timeout :" + PropertiesFileManager.getConfigFileProperty("service", "waitingFileTime") + " seconds");
                checkAnnots.statusList.add(waitingTimeStatus);
                postResponse = new ResponseFormPOST(checkAnnots.statusList);
                postResponse.getMetadata().setDatafiles(uriList);
            } else {
                postResponse = new ResponseFormPOST(checkAnnots.statusList);
            }
            return Response.status(checkAnnots.getHttpStatus()).entity(postResponse).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }
    }
    
    /**
     * Adresse du fichier a envoyer
     *
     * @param in Fichier
     * @param docUri Uri de l'annotation
     * @param headers Entête de la requête
     * @param request
     * @return
     * @throws URISyntaxException
     */
    @POST
    @Path("upload")
    @ApiOperation(value = "Post data file", notes = DocumentationAnnotation.USER_ONLY_NOTES + " Not working from this documentation. Implement a client or use Postman application.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Document file and document metadata saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDocumentFile(
            @ApiParam(value = "File to upload") File in,
            @ApiParam(value = "URI given from \"/documents\" path for upload") @QueryParam("uri") @URL String docUri,
            @Context HttpHeaders headers,
            @Context HttpServletRequest request) throws URISyntaxException {
        ResponseFormPOST postResponse = null;
        List<Status> statusList = new ArrayList();
        
        // Annotation présente
        if (!WAITING_ANNOT_FILE_CHECK.containsKey(docUri)) { 
            statusList.add(new Status("No waiting file", "Error", "No waiting file for the following uri : " + docUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        if (headers != null && headers.getLength() <= 0) {
            statusList.add(new Status("File error", "Error", "File Size : " + headers.getLength() + " octets"));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        // Vérification du checksum md5
        String hash = getHash(in);
        if (hash != null && !WAITING_ANNOT_INFORMATION.get(docUri).getChecksum().equals(hash)) {
            statusList.add(new Status("MD5 error", "Error", "Checksum MD5 doesn't match. Corrupted File."));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        String media = WAITING_ANNOT_INFORMATION.get(docUri).getDocumentType();
        media = media.substring(media.lastIndexOf("#") + 1, media.length());
        
        //SILEX:info
        // Manage authentication error
        //SILEX:info
        FileUploader jsch = null;    
        try {
            jsch = new FileUploader();
        } catch (Exception exp) {
            LOGGER.error(exp.getMessage(), exp);
            throw new WebApplicationException(
                      Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                      .entity(new ResponseFormPOST(new Status("FileUploaderException",
                                                            StatusCodeMsg.ERR,
                                                            "Problem with file system configuration")
                                                )).build());
        }
        //SILEX:conception
        // Add a class to group constants for properties
        //\SILEX:conception
        final String webAppApiDocsName = PropertiesFileManager.getConfigFileProperty("service", "webAppApiDocsName");
        try {
            WAITING_ANNOT_FILE_CHECK.put(docUri, Boolean.TRUE); // Processing file
            LOGGER.debug(jsch.getSFTPWorkingDirectory() + "/" + media);
            // Create document directory if it doesn't exists
            File documentDirectory = new File(jsch.getSFTPWorkingDirectory());
            if (!documentDirectory.isDirectory()) {
                if (!documentDirectory.mkdirs()) {
                    LOGGER.error("Can't create " + webAppApiDocsName + " temporary documents directory");
                    throw new WebApplicationException(
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ResponseFormPOST(new Status("Can't create " + webAppApiDocsName + " temporary documents directory", StatusCodeMsg.ERR, null))).build());
                }else{
                    // Add good rights on the document directory which is on the server
                    try {
                        Runtime.getRuntime().exec("chmod -R 755 " + jsch.getSFTPWorkingDirectory());
                        LOGGER.info( webAppApiDocsName + " temporary documents directory rights successfully updated");
                    } catch (IOException e) {
                        LOGGER.error("Can't change rights on " + webAppApiDocsName + " temporary documents directory");
                    }
                }
            }
            //SILEX:test
            jsch.getChannelSftp().cd(jsch.getSFTPWorkingDirectory());
            //\SILEX:test
        } catch (SftpException e) {
            statusList.add(new Status("SftException", StatusCodeMsg.ERR, e.getMessage()));
            LOGGER.error(e.getMessage(), e);
        }
        
        final String serverFileName = ResourcesUtils.getUniqueID() + "." + WAITING_ANNOT_INFORMATION.get(docUri).getExtension();
        final String serverFilePath = jsch.getSFTPWorkingDirectory() + "/" + serverFileName;
        
        boolean fileTransfered = jsch.fileTransfer(in, serverFileName);
        jsch.closeConnection();
        
        if (!fileTransfered) { // Si le fichier n'a pas été enregistré
            statusList.add(new Status("File upload error", "Error", "Problem during file upload. Try to submit it again " + docUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        WAITING_ANNOT_INFORMATION.get(docUri).setServerFilePath(serverFilePath);
        DocumentDaoSesame documentsDao = DAOFactory.getSESAMEDAOFactory().getDocumentsDaoSesame();
        if (request.getRemoteAddr() != null) {
            documentsDao.remoteUserAdress = request.getRemoteAddr();
        }
        documentsDao.user = userSession.getUser();
        final POSTResultsReturn insertAnnotationJSON = documentsDao.insert(Arrays.asList(WAITING_ANNOT_INFORMATION.get(docUri)));

        postResponse = new ResponseFormPOST(insertAnnotationJSON.statusList);

        if (insertAnnotationJSON.getDataState()) { // Etat du fichier JSON
            WAITING_ANNOT_FILE_CHECK.remove(docUri);
            WAITING_ANNOT_INFORMATION.remove(docUri);
            if (insertAnnotationJSON.getHttpStatus() == Response.Status.CREATED) {
                postResponse.getMetadata().setDatafiles((ArrayList) insertAnnotationJSON.createdResources);
                final URI newUri = new URI(uri.getPath());
                return Response.status(insertAnnotationJSON.getHttpStatus()).location(newUri).entity(postResponse).build();
            } else {
                return Response.status(insertAnnotationJSON.getHttpStatus()).entity(postResponse).build();
            }
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormPOST()).build();
    }
    
    private String getHash(File in) {
        String hash = null;
        try {
            hash = MD5.asHex(MD5.getHash(in)); // Ex : 106fa487baa1728083747de1c6df73e9
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return hash;
    }
    
    @GET
    @Path("types")
    @ApiOperation(value = "Get all documents types",
            notes = "Retrieve all documents types ")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all documents type"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsType(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        DocumentDaoSesame documentsDao = new DocumentDaoSesame();
        Status errorStatus = null;
        try {
            ArrayList<String> documentCategories = documentsDao.getDocumentsTypes();
            if (documentCategories.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity(new ResponseFormGET()).build();
            }
            return Response.status(Response.Status.OK).entity(new ResponseFormDocumentType(limit, page, documentCategories, false)).build();
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            errorStatus = new Status("Error", StatusCodeMsg.ERR, ex.getMessage());
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormGET(errorStatus)).build();
    }
    
    /**
     * 
     * @param limit
     * @param page
     * @param uri
     * @param documentType
     * @param creator
     * @param language
     * @param title
     * @param creationDate
     * @param extension
     * @param concernedItem
     * @param status
     * @param sortByDate sort the results by date (Descending by default)
     * @return the result of the request
     */
    @GET
    @ApiOperation(value = "Get all documents metadata corresponding to the searched params given",
                  notes = "Retrieve all documents authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all documents ", response = DocumentMetadataDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsMetadataBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by document type", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_TYPE) @QueryParam("documentType") @URL String documentType,
        @ApiParam(value = "Search by creator", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_CREATOR) @QueryParam("creator") String creator,
        @ApiParam(value = "Search by language", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_LANGUAGE) @QueryParam("language") String language,
        @ApiParam(value = "Search by title", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_TITLE) @QueryParam("title") String title,
        @ApiParam(value = "Search by creation date", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_CREATION_DATE) @QueryParam("creationDate") @Date(DateFormat.YMD) String creationDate,
        @ApiParam(value = "Search by extension", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_EXTENSION) @QueryParam("extension") String extension,
        @ApiParam(value = "Search by concerned item", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("concernedItem") @URL String concernedItem,
        @ApiParam(value = "Search by status", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_STATUS) @QueryParam("status") String status,
        @ApiParam(value = "Sort results by date", allowableValues = DocumentationAnnotation.EXAMPLE_SORTING_ALLOWABLE_VALUES) @QueryParam("sortByDate") @SortingValue String sortByDate) {
        
        //SILEX:conception
        //Pour l'instant la recherche de documents liés à un élément se fait sur un seul élément. 
        //Par la suite il faudra la faire sur une liste d'éléments
        //\SILEX:conception
        
        DocumentDaoSesame documentDao = new DocumentDaoSesame();
        
        if (uri != null) {
            documentDao.uri = uri;
        }
        if (documentType != null) {
            documentDao.documentType = documentType;
        }
        if (creator != null) {
            documentDao.creator = creator;
        }
        if (language != null) {
            documentDao.language = language;
        }
        if (title != null) {
            documentDao.title = title;
        }
        if (creationDate != null) {
            documentDao.creationDate = creationDate;
        }
        if (extension != null) {
            documentDao.format = extension;
        }
        if (concernedItem != null) {
            documentDao.concernedItemsUris.add(concernedItem);
        }
        if (status != null) {
            documentDao.status = status;
        }
        // Sort the result list by date, descending by default
        if (sortByDate != null) {
            documentDao.sortByDate = sortByDate;
        }
        
        documentDao.user = userSession.getUser();
        documentDao.setPage(page);
        documentDao.setPageSize(limit);
        
        return getDocumentsMetadata(documentDao);
    }
    
    /**
     * SILEX:todo
     * We must find a way to send validation errors in json when an error occured.
     * Maybe just change the response status.
     * \SILEX:todo
     * @param documentURI l'uri du document à télécharger
     * @return la réponse, avec le document si l'uri existe bien
     */
    @GET
    @Path("{documentURI}")
    @ApiOperation(value = "Get a document (by receiving it's uri)",
                  notes = "Retrieve the document corresponding to the uri given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve document"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getDocumentByUri(
            @ApiParam(value = DocumentationAnnotation.DOCUMENT_URI_DEFINITION, required = true, example = DocumentationAnnotation.EXAMPLE_DOCUMENT_URI) @PathParam("documentURI") String documentURI) {
        return getFile(documentURI);
    }
    
    /**
     * @action modifie une liste de métadonnées de documents en fonction des informations envoyées 
     * @param documentsMetadata
     * @param context
     * @return Response le resultat de la requête
     */
    @PUT
    @ApiOperation(value = "Update document metadata")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Document's metadata updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Document not found"),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putDocumentMetadata(
    @ApiParam(value = "Json document metadata") ArrayList<DocumentMetadataDTO> documentsMetadata,
    @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (documentsMetadata != null && !documentsMetadata.isEmpty()) {
            DocumentDaoSesame documentDaoSesame = new DocumentDaoSesame();
            if (documentDaoSesame.remoteUserAdress != null) {
                documentDaoSesame.remoteUserAdress = context.getRemoteAddr();
            }
            documentDaoSesame.user = userSession.getUser();
            
            //Vérification des données et update de la BD
            POSTResultsReturn result = documentDaoSesame.checkAndUpdateList(documentsMetadata);
            
            if (result.getHttpStatus().equals(Response.Status.OK)) { //200 users modifiés
                postResponse = new ResponseFormPOST(result.statusList);
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty document(s) to update"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Collecte les métadonnées de documents issues d'une recherche utilisateur
     * @param documentDao DocumentDaoSesame
     * @return la réponse dédiée à l'utilisateur. 
     *         Contient la liste des métadonnées de documents correspondant au résultat de la recherche
     */
    private Response getDocumentsMetadata(DocumentDaoSesame documentDao) {
        ArrayList<Document> documentsMetadata;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormDocumentMetadata getResponse;
        
        documentDao.user = userSession.getUser();
        // Count all documents for this specific request
        Integer totalCount = documentDao.count();
        // Retreive all documents for this specific request
        documentsMetadata = documentDao.allPaginate();
        
        if (documentsMetadata == null) {
            getResponse = new ResponseFormDocumentMetadata(0, 0, documentsMetadata, true);
            return noResultFound(getResponse, statusList);
        } else if (!documentsMetadata.isEmpty()) {
            getResponse = new ResponseFormDocumentMetadata(documentDao.getPageSize(), documentDao.getPage(), documentsMetadata, true, totalCount);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormDocumentMetadata(0, 0, documentsMetadata, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * 
     * @param documentURI l'URI du document à télécharger
     * @return La réponse, contenant le document à télécharger s'il existe
     */
    private Response getFile(String documentURI) {
        DocumentDaoMongo documentDaoMongo = new DocumentDaoMongo();
        File file = documentDaoMongo.getDocument(documentURI);
        
        if (file == null) {
            return Response.noContent().build();
        } else {
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                           .header("Content-Disposition", "attachement; filename=\"" + file.getName() + "\"")
                           .build();
        }
    }
}
