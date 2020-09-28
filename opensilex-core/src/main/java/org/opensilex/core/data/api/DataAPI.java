//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.core.data.dal.EntityModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author sammy
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/data")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {
    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";

    public static final String DATA_EXAMPLE_URI = "http://opensilex.dev/id/data/1598857852858";
    public static final String DATA_EXAMPLE_OBJECTURI = "http://opensilex.dev/opensilex/2020/o20000345";
    public static final String DATA_EXAMPLE_VARIABLEURI = "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6";
    public static final String DATA_EXAMPLE_PROVENANCEURI = "http://opensilex.dev/provenance/1598001689415";
    public static final String DATA_EXAMPLE_VALUE = "8.6";
    public static final String DATA_EXAMPLE_MINIMAL_DATE  = "2020-08-21T00:00:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2020-09-21T00:00:00";
    
    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.data.modification";

    public static final String CREDENTIAL_DATA_READ_ID = "data-read";
    public static final String CREDENTIAL_DATA_READ_LABEL_KEY = "credential.data.read";

    public static final String CREDENTIAL_DATA_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_DATA_DELETE_LABEL_KEY = "credential.data.delete";

    @Inject
    private DataNucleusService nosql;
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private FileStorageService fs;

    @CurrentUser
    UserModel user;

    @POST
    @Path("create")
    @ApiProtected
    @ApiOperation("Add data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add data", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response addData(
            @ApiParam("Data description") @Valid DataCreationDTO dto
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        DataModel model = dto.newModel();
        
        ErrorResponse error = dao.valid(model);
                
        if (error != null) {
            return error.getResponse();
        } else {
            //dao.prepareURI(model);
            model = (DataModel) dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        }
    }
    
    @POST
    @Path("listcreate")
    @ApiProtected
    @ApiOperation("Add list of data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add data", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response addListData(
            @ApiParam("Data description") @Valid List<DataCreationDTO> dtoList
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        Set<URI> variablesURI = new HashSet<>();
        Set<URI> objectsURI = new HashSet<>();
        Set<String> provenances= new HashSet<>();
        for(DataCreationDTO dto : dtoList ){
            variablesURI.add(dto.getVariable());
            if (dto.getScientificObjects() != null) {
                for(EntityModel object: dto.getScientificObjects())
                    objectsURI.add(object.getUri());
            }            
            provenances.add(dto.getProvenance().getUri().toString());
        }
        
        ErrorResponse error = dao.validList(variablesURI, objectsURI, provenances);
        if (error != null) {
            return error.getResponse();
        } else {
            List<DataModel> dataList = new ArrayList();
            for(DataCreationDTO dto : dtoList ){            
                DataModel model = dto.newModel();
                //dao.prepareURI(model);
                dataList.add(model);
        }
            dao.createAll(dataList);
            return new ObjectUriResponse().getResponse();
        }
        
    }
    
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_READ_ID, credentialLabelKey = CREDENTIAL_DATA_READ_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data retrieved", response = DataGetDTO.class),
        @ApiResponse(code = 404, message = "Data not found", response = ErrorResponse.class)})
    public Response getData(
            @ApiParam(value = "Data URI", /*example = "platform-data:irrigation",*/ required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        DataDAO dao = new DataDAO(nosql,sparql, fs);
        DataModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(DataGetDTO.fromModel(model)).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Data not found",
                    "Unknown data URI: " + uri.toString()).getResponse();
        }
    }
    
    @GET
    @Path("search")
    @ApiOperation("Search data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_READ_ID, credentialLabelKey = CREDENTIAL_DATA_READ_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = DataGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchDataList(
            //@ApiParam(value = "Search by uri", example = DATA_EXAMPLE_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by object uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("objectUri") URI objectUri,
            @ApiParam(value = "Search by variable uri", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variableUri") URI variableUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenanceUri") URI provenanceUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("startDate") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("endDate") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        ListWithPagination<DataModel> resultList = dao.search(
                user,
                //uri,
                objectUri,
                variableUri,
                provenanceUri,
                startDate,
                endDate,
                page,
                pageSize
        );
               
        ListWithPagination<DataGetDTO> resultDTOList = resultList.convert(DataGetDTO.class, DataGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Data deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteData(
            @ApiParam(value = "Data URI", example = DATA_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) {

        try {
            DataDAO dao = new DataDAO(nosql,sparql, fs);
            dao.delete(uri);
            return new ObjectUriResponse(uri).getResponse();

        } catch (NoSQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage())
                    .getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
    
    @PUT
    @Path("{uri}/confidence")
    @ApiProtected
    @ApiOperation("Update confidence")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response confidence(
            @ApiParam("Data description") @Valid DataConfidenceDTO dto,
            @ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try{
            DataDAO dao = new DataDAO(nosql, sparql, fs);
            DataModel model = dto.newModel();
            
            if(!nosql.existByURI(model, uri)) throw new NoSQLInvalidURIException(uri);
            model.setUri(uri);
            model = (DataModel) dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
        }catch (NoSQLInvalidURIException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage()).getResponse();
        }catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
        
    }
    
    @PUT
    @Path("update")
    @ApiProtected
    @ApiOperation("Update data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Confidence update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Data description") @Valid DataUpdateDTO dto
            //@ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try{
            DataDAO dao = new DataDAO(nosql, sparql, fs);
            DataModel model = dto.newModel();
            ErrorResponse error = dao.valid(model);
                
            if (error != null) {
                return error.getResponse();
            } else {
            
                if(!nosql.existByURI(model, dto.getUri())) throw new NoSQLInvalidURIException(dto.getUri());

                model = (DataModel) dao.update(model);
                return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
            }
        }catch (NoSQLInvalidURIException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage()).getResponse();
        }catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
        
    }
    
    /**
     * Saves data file with its metadata and use MULTIPART_FORM_DATA for it.fileContentDisposition parameter is automatically created from submitted file.
     * No example could be provided for this kind of MediaType
     *
     * @param dto
     * @param file
     * @param fileContentDisposition
     * @return the insertion result.
     */
    @POST
    @Path("file")
    @ApiOperation(value = "Post data file")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = ObjectUriResponse.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFile(
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") DataFileCreationDTO dto,
            @ApiParam(value = "Data file", required = true, type = "file") @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        //FileDescriptionDAO fileDescriptionDao = new FileDescriptionDAO(sparql);
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        DataFileModel model = dto.newModel();      
        //Boolean dao.valid(model) = true;
        Boolean valid = true;
        if (valid) {            
            model.setFilename(fileContentDisposition.getFileName());
            dao.createFile(model, file);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } else {
            return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "Unknown Object or Provenance URI",
                        "Unknown Object or Provenance URI"
            ).getResponse();
        }
        
    }
    
    /**
     * Save the metadata of a file already stored in an accessible storage.The absolute path of the file will be ${ws.updir.doc property}/relativePath
 ${ws.updir.doc property} refers to the <ws.updir.doc> property defined in the config.properties file used for building the webservice.
     *
     * @param dtoList
     * @param context
     * @return the insertion result.
     * @throws java.lang.Exception
     * @example [ { "rdfType":
     * "http://www.opensilex.org/vocabulary/oeso#HemisphericalImage", "date":
     * "2017-06-15T10:51:00+0200", "provenanceUri":
     * "http://www.opensilex.org/opensilex/id/provenance/1551805521606",
     * "relativePath" : "4P/464/proc.txt", "concernedItems": [{ "uri":
     * "http://www.opensilex.org/demo/DMO2018-1", "typeURI":
     * "http://www.opensilex.org/vocabulary/oeso#Experiment" }], "metadata": {
     * "sensorUri": "http://www.phenome-fppn.fr/diaphen/2018/s18001" } } ]
     * @example {
     * "metadata": {
     * "pagination": null,
     * "status": [],
     * "datafiles": [
     * "http://www.opensilex.org/opensilex/id/dataFile/HemisphericalImage/ynckimhx54ejoppqewxw2o4aje44kdfvsaimdkptypznrzzbreoa45ae8ad4836741e0ad1a48838bb525bb"
     * ]
     * }
     * }
     */
    @POST
    @Path("filepaths")
    @ApiOperation(value = "Post data about existing files")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file(s) metadata(s) saved", response = ObjectUriResponse.class)})
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFilePaths(
            @ApiParam(value = "Metadata of the file", required = true) @NotNull @Valid List<DataFileCreationDTO> dtoList,
            @Context HttpServletRequest context
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        Set<URI> objectsURI = new HashSet<>();
        Set<String> provenances= new HashSet<>();
        for(DataFileCreationDTO dto : dtoList ){ 
            if (dto.getScientificObjects() != null) {
                for (EntityModel obj:dto.getScientificObjects()) {
                    objectsURI.add(obj.getUri());
                } 
            }                        
            provenances.add(dto.getProvenance().getUri().toString());
        }
        
        ErrorResponse error = dao.validList(new HashSet(), objectsURI, provenances);
        if (error != null) {
            return error.getResponse();
        } else {
            List<DataModel> dataList = new ArrayList();
            for(DataFileCreationDTO dto : dtoList ){            
                DataFileModel model = dto.newModel();
                // get the the absolute file path according to the fileStorageDirectory
                java.nio.file.Path absoluteFilePath = fs.getAbsolutePath(Paths.get(model.getPath()));

                if (!fs.exist(absoluteFilePath)) {
                    return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "File not found",
                                absoluteFilePath.toString()
                    ).getResponse();
                }

                model.setPath(absoluteFilePath.toString());
                model.setFilename(absoluteFilePath.getFileName().toString());
                dataList.add(model);
        }
            dataList = (List<DataModel>) dao.createAll(dataList);
            return new PaginatedListResponse<>(dataList).getResponse();
        }     

    }
    
    /**
     * Returns the content of the file corresponding to the URI given.
     *
     * @param fileUri the {@link URI} of the file to download
     * @param response
     * @return The file content or null with a 404 status if it doesn't exists
     */
    @ApiProtected
    @GET
    @Path("file/{fileUri}")
    @ApiOperation(value = "Get data file")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getDataFile(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("fileUri") @NotNull URI fileUri,
            @Context HttpServletResponse response
    ) {
        try {
            DataDAO dao = new DataDAO(nosql, sparql, fs);

            DataFileModel description = dao.getFile(fileUri);
            if (description == null) {
                return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
            }

            java.nio.file.Path filePath = Paths.get(description.getPath());
            byte[] fileContent = fs.readFileAsByteArray(filePath);

            if (ArrayUtils.isEmpty(fileContent)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
            }
            return Response.ok(fileContent, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + filePath.getFileName().toString() + "\"") //optional
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build();
        }
    }
    
    /**
     * This service returns the description of a file corresponding to the URI given.
     *
     * @param fileUri
     * @return the file description
     * @throws java.lang.Exception
     * @example {
     * "uri": "http://www.phenome-fppn.fr/diaphen/id/dataFile/RGBImage/55fjbbmtmr4m3kkizslzaddfkdt2ranum3ikz6cdiajqzfdc7yqa31d87b83efac4c358ceb5b0da6ed27ff",
     * "rdfType": "http://www.opensilex.org/vocabulary/oeso#RGBImage",
     * "date": "2017-06-15T10:51:00+0200",
     * "concernedItems": [{
     * "uri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     * "typeURI": "http://www.opensilex.org/vocabulary/oeso#Plot"
     * }],
     * "provenanceUri": "http://www.phenome-fppn.fr/diaphen/id/provenance/1552405256945",
     * "metadata": {
     * "sensor": "http://www.phenome-fppn.fr/diaphen/2018/s18035",
     * "position": "1"
     * }
     * }
     */
    @GET
    @Path("file/{fileUri}/description")
    @ApiOperation(value = "Get data file description")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file description", response = DataFileCreationDTO.class),})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescription(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("fileUri") @NotNull URI fileUri
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        DataFileModel description = dao.getFile(fileUri);

        if (description != null) {
            return new SingleObjectResponse<>(DataFileGetDTO.fromModel(description)).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Data not found",
                    "Unknown data URI: " + fileUri.toString()).getResponse();
        }
    }
    
    /**
     * Returns a thumbnail based on the content of the file corresponding to the URI given.The given URI must link to a picture.
     *
     * @param fileUri      the {@link URI} of the file to download
     * @param scaledHeight the height of the thumbnail to return
     * @param scaledWidth  the width of the thumbnail to return
     * @param response
     * @return The file content or null with a 404 status if it doesn't exists
     * @throws java.lang.Exception
     */
    @ApiProtected
    @GET
    @Path("file/thumbnail{fileUri}")
    @ApiOperation(value = "Get picture thumbnail")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve thumbnail of a picture")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getPicturesThumbnails(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("fileUri") @NotNull URI fileUri,
            @ApiParam(value = "Thumbnail width") @QueryParam("scaledWidth") @Min(256) @Max(1920) @DefaultValue("640") Integer scaledWidth,
            @ApiParam(value = "Thumbnail height") @QueryParam("scaledHeight") @Min(144) @Max(1080) @DefaultValue("360") Integer scaledHeight,
            @Context HttpServletResponse response) throws Exception {

        DataDAO dao = new DataDAO(nosql, sparql, fs);

        DataFileModel description = dao.getFile(fileUri);
        if (description == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }

        byte[] imageData = ImageResizer.getInstance().resize(
                fs.readFileAsByteArray(Paths.get(description.getPath())),
                scaledWidth,
                scaledHeight
        );

        return Response.ok(imageData, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + description.getFilename() + "\"") //optional
                .build();

    }
    
    /**
     * This service searches for file descriptions according to the search parameters given.
     *
     * @param pageSize
     * @param objectUri
     * @param provenanceUri
     * @param page
     * @param rdfType
     * @param startDate
     * @param endDate
     * @return List of file description
     * @example [{
     * "uri": "http://www.phenome-fppn.fr/diaphen/id/dataFile/RGBImage/55fjbbmtmr4m3kkizslzaddfkdt2ranum3ikz6cdiajqzfdc7yqa31d87b83efac4c358ceb5b0da6ed27ff",
     * "rdfType": "http://www.opensilex.org/vocabulary/oeso#RGBImage",
     * "date": "2017-06-15T10:51:00+0200",
     * "concernedItems": [{
     * "uri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     * "typeURI": "http://www.opensilex.org/vocabulary/oeso#Plot"
     * }],
     * "provenanceUri": "http://www.phenome-fppn.fr/diaphen/id/provenance/1552405256945",
     * "metadata": {
     * "sensor": "http://www.phenome-fppn.fr/diaphen/2018/s18035",
     * "position": "1"
     * }
     * }]
     */
    @GET
    @Path("file/search")
    @ApiOperation(value = "Retrieve data file descriptions corresponding to the search parameters given.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file descriptions", response = DataFileGetDTO.class, responseContainer = "List")
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescriptionsBySearch(
            @ApiParam(value = "Search by rdf type uri") @QueryParam("rdfType") URI rdfType,
            @ApiParam(value = "Search by object uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("objectUri") URI objectUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenanceUri") URI provenanceUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("startDate") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("endDate") String endDate,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize

    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        ListWithPagination<DataFileModel> resultList = dao.searchFiles(
                user,
                //uri,
                objectUri,
                provenanceUri,
                startDate,
                endDate,
                page,
                pageSize
        );
        
        ListWithPagination<DataFileGetDTO> resultDTOList = resultList.convert(DataFileGetDTO.class, DataFileGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();

    }
    
    @PUT
    @Path("file/update")
    @ApiProtected
    @ApiOperation("Update data file description")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "data file update", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response updateFile(
            @ApiParam("DataFile description") @Valid DataFileUpdateDTO dto
            //@ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try{
            DataDAO dao = new DataDAO(nosql, sparql, fs);
            DataFileModel model = dto.newModel();
            ErrorResponse error = dao.valid(model);
                
            if (error != null) {
                return error.getResponse();
            } else {
            
                if(!nosql.existByURI(model, dto.getUri())) throw new NoSQLInvalidURIException(dto.getUri());

                model = (DataFileModel) dao.update(model);
                return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
            }
        }catch (NoSQLInvalidURIException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Data URI", e.getMessage()).getResponse();
        }catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
        
    }
}
