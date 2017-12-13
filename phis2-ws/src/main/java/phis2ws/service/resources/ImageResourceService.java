//**********************************************************************************************
//                                       ImageResourceService.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the images data service
//***********************************************************************************************
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
import java.text.ParseException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.mongo.ImageMetadataDaoMongo;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.ImageMetadataDTO;
import phis2ws.service.utils.FileUploader;
import phis2ws.service.utils.ImageWaitingCheck;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.ImageMetadata;

@Api("/images")
@Path("/images")
public class ImageResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(ImageResourceService.class);
    
    @Context
    UriInfo uri;
    
    //Session Utilisateur
    @SessionInject
    Session userSession;
    
     // Gère les annotations en attene
    public final static ExecutorService threadPool = Executors.newCachedThreadPool();
    // Deux Maps qui contiennent les informations sur les annotations en attentes
    public final static Map<String, Boolean> waitingMetadataFileCheck = new HashMap<>();
    public final static Map<String, ImageMetadata> waitingMetadataInformation = new HashMap<>();
    
    /**
     * Vérifie un ensemble de métadonnées d'images
     * @param headers
     * @param imagesMetadata
     * @return 
     */
    @POST
    @ApiOperation(value = "Save a file", notes = DocumentationAnnotation.ADMIN_ONLY_NOTES) 
    @ApiResponses(value = {
        @ApiResponse(code = 202, message = "Metadata verified and correct", response = ImageMetadataDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiImplicitParams({
       @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postImagesMetadata(@Context HttpHeaders headers,
            @ApiParam(value = "JSON Image metadata", required = true) List<ImageMetadataDTO> imagesMetadata) {
        AbstractResultForm postResponse;
        if (imagesMetadata != null && !imagesMetadata.isEmpty()) {
            ImageMetadataDaoMongo imageDaoMongo = new ImageMetadataDaoMongo();
            imageDaoMongo.user = userSession.getUser();
            
            //Vérification des métadonnées
            final POSTResultsReturn checkImageMetadata = imageDaoMongo.check(imagesMetadata); 
            
            if (checkImageMetadata.statusList == null) { //Les métadonnées ne sont pas bonnes
                postResponse = new ResponseFormPOST();
            } else if (checkImageMetadata.getDataState()) {//Les métadonnées sont bonnes
                ArrayList<String> imagesUploadLinks = new ArrayList<>();
                long imagesNumber = imageDaoMongo.getNbImagesYear();
                URINamespaces uriNamespaces = new URINamespaces();
                for (ImageMetadataDTO imageMetadata : imagesMetadata) {
                    final UriBuilder uploadPath = uri.getBaseUriBuilder();
                    
                    //Calcul du nombre de 0 à ajouter devant le numéro de l'image
                    String nbImagesByYear = Long.toString(imagesNumber);
                    while (nbImagesByYear.length() < 10) {
                        nbImagesByYear = "0" + nbImagesByYear;
                    }
                    
                    String uniqueId = "i" + Year.now().toString().substring(2, 4) + nbImagesByYear;
                    final String imageUri = uriNamespaces.getContextsProperty("pxPlatform") + "/" + Year.now().toString() + "/" + uniqueId;
                    final String uploadLink = uploadPath.path("images").path("upload").queryParam("uri", imageUri).toString();
                    imagesUploadLinks.add(uploadLink);
                    
                    waitingMetadataFileCheck.put(imageUri, false); // fichier en attente
                    ImageMetadata imageMetadataToSave = imageMetadata.createObjectFromDTO();
                    imageMetadataToSave.setUri(imageUri);
                    waitingMetadataInformation.put(imageUri, imageMetadataToSave);
                        //Lancement THREAD pour le fichier en attente
                    threadPool.submit(new ImageWaitingCheck(imageUri));
                }
                final Status waitingTimeStatus = new Status("Timeout", StatusCodeMsg.INFO, " Timeout :" + PropertiesFileManager.getConfigFileProperty("service", "waitingFileTime") + " seconds");
                checkImageMetadata.statusList.add(waitingTimeStatus);
                postResponse = new ResponseFormPOST(checkImageMetadata.statusList);
                postResponse.getMetadata().setDatafiles(imagesUploadLinks);
            } else {
                postResponse = new ResponseFormPOST(checkImageMetadata.statusList);
            }
            return Response.status(checkImageMetadata.getHttpStatus()).entity(postResponse).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormPOST()).build();
        }
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
    
    private String getServerImagesDirectory(String imageUri) {
        return PropertiesFileManager.getConfigFileProperty("service", "uploadImageServerDirectory") + "/"
                + PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "platform") + "/" 
                + Year.now().toString();
    }
    
    /**
     * 
     * @param imageUri
     * @return le nom de l'image. Il est utilise l'identifiant de l'image présent dans l'uri. 
     *              ex. i170000000009
     */
    private String getImageName(String imageUri) {
        return imageUri.substring(imageUri.lastIndexOf("/") + 1, imageUri.length());
    }
    
    /**
     * Adresse du fichier à envoyer
     * @param in File
     * @param imageUri Metadata uri
     * @param headers
     * @param request
     * @return
     * @throws URISyntaxException 
     */
    @POST
    @Path("upload")
    @ApiOperation(value = "Post data file", notes = DocumentationAnnotation.USER_ONLY_NOTES + " Not working from this documentation. Implement a client or use Postman application.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Image file and image metadata saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiImplicitParams({
       @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postImageFile(
        @ApiParam(value = "File to upload") File in,
        @ApiParam(value = "Uri given from \"images\" path for upload") @QueryParam("uri") String imageUri,
        @Context HttpHeaders headers,
        @Context HttpServletRequest request) throws URISyntaxException, ParseException {
        ResponseFormPOST postResponse = null;
        List<Status> statusList = new ArrayList<>();
        
        //Métadonnées associées présentes
        if (!waitingMetadataFileCheck.containsKey(imageUri)) {
            statusList.add(new Status("No waiting image", StatusCodeMsg.ERR, "No waiting file for the following uri : " + imageUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        if (headers != null && headers.getLength() <= 0) {
            statusList.add(new Status("File error", StatusCodeMsg.ERR, "File Size : " + headers.getLength() + " octets"));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        //Vérification du checksum
        String hash = getHash(in);
        if (hash != null && !waitingMetadataInformation.get(imageUri).getFileInformations().getChecksum().equals(hash)) {
            statusList.add(new Status("MD5 error", StatusCodeMsg.ERR, "Checksum MD5 doesn't match. Corrupted File."));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        FileUploader jsch = new FileUploader();
        
        final String serverFileName = getImageName(imageUri) + "." + waitingMetadataInformation.get(imageUri).getFileInformations().getExtension();
        final String serverImagesDirectory = getServerImagesDirectory(imageUri);
        
        try {
            waitingMetadataFileCheck.put(imageUri, Boolean.TRUE); //Traitement en cours du fichier
            //SILEX:test
            jsch.getChannelSftp().cd(serverImagesDirectory);
            //\SILEX:test
        } catch (SftpException e) {
            statusList.add(new Status("SftException", StatusCodeMsg.ERR, e.getMessage()));
            LOGGER.error(e.getMessage(), serverImagesDirectory + " " + e);
        }
        
        boolean fileTransfered = jsch.fileTransfer(in, serverFileName);
        jsch.closeConnection();
        
        if (!fileTransfered) { //Si l'image n'a pas été enregistrée
            statusList.add(new Status("Image upload error", StatusCodeMsg.ERR, "An error occurred during file upload. Try to submit it again " + imageUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        waitingMetadataInformation.get(imageUri).getFileInformations().setServerFilePath(serverImagesDirectory + serverFileName);
        
        ImageMetadataDaoMongo imageMetadataDaoMongo = new ImageMetadataDaoMongo();
        imageMetadataDaoMongo.user = userSession.getUser();
        
        final POSTResultsReturn insertMetadata = imageMetadataDaoMongo.insert(Arrays.asList(waitingMetadataInformation.get(imageUri)));
        postResponse = new ResponseFormPOST(insertMetadata.statusList);
        
        if (insertMetadata.getDataState()) {
            waitingMetadataFileCheck.remove(imageUri);
            waitingMetadataInformation.remove(imageUri);
            
            if (insertMetadata.getHttpStatus() == Response.Status.CREATED) {
                postResponse.getMetadata().setDatafiles((ArrayList) insertMetadata.createdResources);
                final URI newUri = new URI(uri.getPath());
                return Response.status(insertMetadata.getHttpStatus()).location(newUri).entity(postResponse).build();
            } else {
                return Response.status(insertMetadata.getHttpStatus()).entity(postResponse).build();
            }
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseFormPOST()).build();
    }
}
