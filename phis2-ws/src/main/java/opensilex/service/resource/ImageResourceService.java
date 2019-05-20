//******************************************************************************
//                              ImageResourceService.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 8 Dec.. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

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
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import opensilex.service.PropertiesFileManager;
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.ImageMetadataMongoDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.ImageMetadataDTO;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.FileUploader;
import opensilex.service.utils.ImageWaitingCheck;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.ImageMetadata;

/**
 * Image resource service.
 * @update [Andréas Garcia] Jan. 2019: modify "concern(s)" occurences into "concernedItem(s)"
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Deprecated
@Api("/images")
@Path("/images")
public class ImageResourceService extends ResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(ImageResourceService.class);
    
    @Context
    UriInfo uri;
    
    // For the waiting annotations
    public final static ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    
    // contains the informations abour the waiting annotations
    public final static Map<String, Boolean> WAITING_METADATA_FILE_CHECK = new HashMap<>();
    
    // contains the informations abour the waiting annotations
    public final static Map<String, ImageMetadata> WAITING_METADATA_INFORMATION = new HashMap<>();
    
    /**
     * Checks images metadata.
     * @param headers
     * @param imagesMetadata 
     * @example
     * metadata wanted for each image: 
     *  { 
     *      rdfType,
     *      concernedItems [
     *          {
     *              uri,
     *              typeURI
     *          }
     *      ],
     *      shootingConfigurations {
     *          date,
     *          position,
     *          sensor
     *      },
     *      storage {
     *          checksum,
     *          extension
     *     }
     *  }
     * @return the URL to save the image(s) if metadata is correct
     */
    @Deprecated
    @POST
    @ApiOperation(value = "Save a file", notes = DocumentationAnnotation.ADMIN_ONLY_NOTES) 
    @ApiResponses(value = {
        @ApiResponse(code = 202, message = "Metadata verified and correct", response = ImageMetadataDTO.class, responseContainer = "List"),
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
    public Response postImagesMetadata(@Context HttpHeaders headers,
            @ApiParam(value = "JSON Image metadata", required = true) @Valid List<ImageMetadataDTO> imagesMetadata) {
        AbstractResultForm postResponse;
        if (imagesMetadata != null && !imagesMetadata.isEmpty()) {
            ImageMetadataMongoDAO imageMongoDao = new ImageMetadataMongoDAO();
            imageMongoDao.user = userSession.getUser();
            
            final POSTResultsReturn checkImageMetadata = imageMongoDao.check(imagesMetadata); 
            
            if (checkImageMetadata.statusList == null) { // bad metadata
                postResponse = new ResponseFormPOST();
            } else if (checkImageMetadata.getDataState()) {// metadata ok
                ArrayList<String> imagesUploadLinks = new ArrayList<>();
                String lastGeneratedUri = null;
                for (ImageMetadataDTO imageMetadata : imagesMetadata) {
                    try {
                        final UriBuilder uploadPath = uri.getBaseUriBuilder();
                        
                        // generates the imageUri
                        UriGenerator uriGenerator = new UriGenerator();
                        final String imageUri = uriGenerator.generateNewInstanceUri(
                                Oeso.CONCEPT_IMAGE.toString(), 
                                Year.now().toString(), 
                                lastGeneratedUri);
                        lastGeneratedUri = imageUri;
                        
                        final String uploadLink = uploadPath.path("images").path("upload").queryParam("uri", imageUri).toString();
                        imagesUploadLinks.add(uploadLink);
                        
                        WAITING_METADATA_FILE_CHECK.put(imageUri, false); // file waiting
                        ImageMetadata imageMetadataToSave = imageMetadata.createObjectFromDTO();
                        imageMetadataToSave.setUri(imageUri);
                        WAITING_METADATA_INFORMATION.put(imageUri, imageMetadataToSave);
                        // Launch the thread for the expected file
                        THREAD_POOL.submit(new ImageWaitingCheck(imageUri));
                    } catch (Exception ex) { // In the images case, no exception should be raised
                        java.util.logging.Logger.getLogger(ImageResourceService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                final Status waitingTimeStatus = new Status(
                        StatusCodeMsg.TIMEOUT, 
                        StatusCodeMsg.INFO, 
                        " Timeout :" + PropertiesFileManager.getConfigFileProperty("service", "waitingFileTime") + " seconds");
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
    
    /**
     * Calculates the hash of a file.
     * @param file the file we want the hash
     * @return the hash file
     */
    private String getHash(File file) {
        String hash = null;
        try {
            hash = MD5.asHex(MD5.getHash(file)); // e.g: 106fa487baa1728083747de1c6df73e9
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return hash;
    }
    
    /**
     * Calculates the server image directory for an image.
     * @return the server image directory of the image. 
     *         The images are saved by year of insertion.
     * @example http://www.phenome-fppn.fr/platform/2017/i170000000000)
     * 
     */
    private String getServerImagesDirectory() {
        return PropertiesFileManager.getConfigFileProperty("service", "uploadImageServerDirectory") + "/"
                + PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructure") + "/" 
                + Year.now().toString();
    }
    
    /**
     * @return the web access image directory
     * @example http://localhost/images/platform/2017/i170000000000)
     */
    private String getWebAccessImagesDirectory() {
        return PropertiesFileManager.getConfigFileProperty("service", "imageFileServerDirectory") + "/"
                + PropertiesFileManager.getConfigFileProperty("sesame_rdf_config", "infrastructure") + "/" 
                + Year.now().toString();
    }
    
    /**
     * @param imageUri
     * @return the image name, extracted from the image URI. 
     *         It corresponds to the image id from the URI 
     * @example i170000000000
     */
    private String getImageName(String imageUri) {
        return imageUri.substring(imageUri.lastIndexOf("/") + 1, imageUri.length());
    }
    
    /**
     * @param in File
     * @param imageUri Metadata uri
     * @param headers
     * @param request
     * @return
     * @throws URISyntaxException 
     * @throws java.text.ParseException 
     */
    @Deprecated
    @POST
    @Path("upload")
    @ApiOperation(value = "Post data file", notes = DocumentationAnnotation.USER_ONLY_NOTES 
                            + " Not working from this documentation. Implement a client or use Postman application.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Image file and image metadata saved", response = ResponseFormPOST.class),
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
    public Response postImageFile(
        @ApiParam(value = "File to upload") File in,
        @ApiParam(value = "Uri given from \"images\" path for upload") @QueryParam("uri") @URL @Required String imageUri,
        @Context HttpHeaders headers,
        @Context HttpServletRequest request) throws URISyntaxException, ParseException {
        ResponseFormPOST postResponse;
        List<Status> statusList = new ArrayList<>();
        
        // The file metadata exists
        if (!WAITING_METADATA_FILE_CHECK.containsKey(imageUri)) {
            statusList.add(new Status("No waiting image", StatusCodeMsg.ERR, "No waiting file for the following uri : " + imageUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        if (headers != null && headers.getLength() <= 0) {
            statusList.add(new Status(StatusCodeMsg.FILE_ERROR, StatusCodeMsg.ERR, "File Size : " + headers.getLength() + " octets"));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        // check the checksum
        String hash = getHash(in);
        if (hash != null && !WAITING_METADATA_INFORMATION.get(imageUri).getFileInformations().getChecksum().equals(hash)) {
            statusList.add(new Status(StatusCodeMsg.MD5_ERROR, StatusCodeMsg.ERR, "Checksum MD5 doesn't match. Corrupted File."));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        FileUploader jsch = new FileUploader();
        
        final String serverFileName = getImageName(imageUri) + "." + WAITING_METADATA_INFORMATION.get(imageUri).getFileInformations().getExtension();
        final String serverImagesDirectory = getServerImagesDirectory();
        final String webAccessImagesDirectory = getWebAccessImagesDirectory();
    
        try {
            WAITING_METADATA_FILE_CHECK.put(imageUri, Boolean.TRUE);
            //SILEX:test
            jsch.getChannelSftp().cd(serverImagesDirectory);
            //\SILEX:test
        } catch (SftpException e) {
            try {
                //Create repository if it does not exist
                jsch.createNestedDirectories(serverImagesDirectory);
                jsch.getChannelSftp().cd(serverImagesDirectory);
                LOGGER.debug("Create directory : " + serverImagesDirectory);
            } catch (SftpException ex) {
                statusList.add(new Status(StatusCodeMsg.SFTP_EXCEPTION, StatusCodeMsg.ERR, e.getMessage()));
                LOGGER.error(e.getMessage(), serverImagesDirectory + " " + ex);
            }
        }
        
        boolean fileTransfered = jsch.fileTransfer(in, serverFileName);
        jsch.closeConnection();
        
        if (!fileTransfered) { //If the image has not been register
            statusList.add(new Status(
                    "Image upload error", 
                    StatusCodeMsg.ERR, 
                    "An error occurred during file upload. Try to submit it again " + imageUri));
            postResponse = new ResponseFormPOST(statusList);
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
        
        WAITING_METADATA_INFORMATION.get(imageUri)
                .getFileInformations()
                .setServerFilePath(webAccessImagesDirectory + "/" + serverFileName);
        
        ImageMetadataMongoDAO imageMetadataMongoDao = new ImageMetadataMongoDAO();
        imageMetadataMongoDao.user = userSession.getUser();
        
        final POSTResultsReturn insertMetadata = imageMetadataMongoDao.insert(Arrays.asList(WAITING_METADATA_INFORMATION.get(imageUri)));
        postResponse = new ResponseFormPOST(insertMetadata.statusList);
        
        if (insertMetadata.getDataState()) {
            WAITING_METADATA_FILE_CHECK.remove(imageUri);
            WAITING_METADATA_INFORMATION.remove(imageUri);
            
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
    
    /**
     * Searches images metadata corresponding to a user search.
     * @param imageMetadataMongoDao
     * @return the images corresponding to the search
     */
    private Response getImagesData(ImageMetadataMongoDAO imageMetadataMongoDao) {
        ArrayList<ImageMetadata> imagesMetadata;
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<ImageMetadata> getResponse;
        
        imagesMetadata = imageMetadataMongoDao.allPaginate();
        
        if (imagesMetadata == null) {
            getResponse = new ResultForm<>(0, 0, imagesMetadata, true);
            return noResultFound(getResponse, statusList);
        } else if (!imagesMetadata.isEmpty()) {
            getResponse = new ResultForm<>(imageMetadataMongoDao.getPageSize(), imageMetadataMongoDao.getPage(), imagesMetadata, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResultForm<ImageMetadata>(0, 0, imagesMetadata, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    /**
     * 
     * @param pageSize
     * @param page
     * @param uri image URI (e.g http://www.phenome-fppn.fr/phis_field/2017/i170000000000)
     * @param rdfType image type (e.g http://www.opensilex.org/vocabulary/oeso#HemisphericalImage)
     * @param concernedItems URIs of the items concerned by the searched image(s), separated by ";". 
     * (e.g http://phenome-fppn.fr/phis_field/ao1;http://phenome-fppn.fr/phis_field/ao2)
     * @param startDate start date of the shooting. Format YYYY-MM-DD (e.g 2015-07-07)
     * @param endDate end date of the shooting. Format YYYY-MM-DD (e.g 2015-07-08)
     * @param sensor URI of the sensor providing the image (e.g. http://www.phenome-fppn.fr/diaphen/2018/s18035)
     * @return the images list corresponding to the search parameters given (all the images if no search param) 
     * /!\ there is a pagination 
     * JSON returned : 
     * [
     *    { //first image description
     *         uri,
     *         rdfType,
     *         concernedItems [
     *             {
     *                 uri,
     *                 rdfType
     *             }
     *         ],
     *         shootingConfigurations {
     *             date,
     *             timestamp,
     *             sensorPosition
     *         },
     *         storage {
     *             extension,
     *             md5sum,
     *             serverFilePath
     *         }
     *     },
     *     ...
     * ]
     */
    @Deprecated
    @GET
    @ApiOperation(value = "Get all images corresponding to the search params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all images", response = ImageMetadata.class, responseContainer = "List"),
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImagesBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
        @ApiParam(value = "Search by image uri", example = DocumentationAnnotation.EXAMPLE_IMAGE_URI) @QueryParam("uri") @URL String uri,
        @ApiParam(value = "Search by image type", example = DocumentationAnnotation.EXAMPLE_IMAGE_TYPE) @QueryParam("rdfType") @URL String rdfType,
        @ApiParam(value = "Search by concerned item uri - each concerned item uri must be separated by \";\"", example = DocumentationAnnotation.EXAMPLE_IMAGE_CONCERNED_ITEMS) @QueryParam("concernedItems") String concernedItems,
        @ApiParam(value = "Search by interval - start date", example = DocumentationAnnotation.EXAMPLE_IMAGE_DATE) @QueryParam("startDate") @opensilex.service.resource.validation.interfaces.Date(DateFormat.YMDHMSZ) String startDate,
        @ApiParam(value = "Search by interval - end date", example = DocumentationAnnotation.EXAMPLE_IMAGE_DATE) @QueryParam("endDate") @opensilex.service.resource.validation.interfaces.Date(DateFormat.YMDHMSZ) String endDate,
        @ApiParam(value = "Search by sensor", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("sensor") @URL String sensor) {
        
        ImageMetadataMongoDAO imageMetadataMongoDao = new ImageMetadataMongoDAO();
        
        if (uri != null) {
            imageMetadataMongoDao.uri = uri;
        }
        if (rdfType != null) {
            imageMetadataMongoDao.rdfType = rdfType;
        }
        if (concernedItems != null) {
            imageMetadataMongoDao.concernedItems = new ArrayList<>(Arrays.asList(concernedItems.split(";")));
        }
        if (startDate != null) {
            //SILEX:todo
            //check date format
            imageMetadataMongoDao.startDate = startDate;
            //\SILEX:todo
            if (endDate != null) {
                imageMetadataMongoDao.endDate = endDate;
            } else {
                imageMetadataMongoDao.endDate = new SimpleDateFormat(DateFormat.YMD.toString()).format(new Date());
            }
        }
        if (sensor != null) {
            imageMetadataMongoDao.sensor = sensor;
        }
        
        imageMetadataMongoDao.user = userSession.getUser();
        imageMetadataMongoDao.setPage(page);
        imageMetadataMongoDao.setPageSize(pageSize);
        
        return getImagesData(imageMetadataMongoDao);
    }
}
