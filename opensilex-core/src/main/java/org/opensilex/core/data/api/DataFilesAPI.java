//******************************************************************************
//                          DataFilesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteError;
import io.swagger.annotations.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.device.api.DeviceAPI;
import org.opensilex.core.exception.DateMappingExceptionResponse;
import org.opensilex.core.exception.DateValidationException;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLInvalidUriListException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_OBJECTURI;
import static org.opensilex.core.data.dal.DataDAO.FS_FILE_PREFIX;


/**
 *
 * @author Alice Boizet
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/datafiles")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataFilesAPI {
    
    public static final String DATAFILE_EXAMPLE_URI = "http://opensilex.dev/id/file/1598857852858";
    public static final String DATAFILE_EXAMPLE_TYPE = "http://www.opensilex.org/vocabulary/oeso#Image";
    
    @Inject
    private MongoDBService nosql;
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel user;
    
     /**
     * Saves data file with its metadata and use MULTIPART_FORM_DATA for it.fileContentDisposition parameter is automatically created from submitted file.No example could be provided for this kind of MediaType
     *
     * @param dto
     * @param file
     * @param fileContentDisposition
     * @return the insertion result.
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation(value = "Add a data file",
    notes = "{\"rdf_type\":\"" +  DATAFILE_EXAMPLE_TYPE + "\", "
            + "\"date\":\"" +  DataAPI.DATA_EXAMPLE_MINIMAL_DATE + "\", "
            + "\"target\":\"http://plot01\", "
            + "\"provenance\": { \"uri\":\"" +  DataAPI.DATA_EXAMPLE_PROVENANCEURI + "\" }, "
            + "\"metadata\":" +  DataAPI.DATA_EXAMPLE_METADATA + "}"
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = URI.class)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFile(
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") DataFileCreationDTO dto,
            @ApiParam(value = "Data file", required = true, type = "file") @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        try {
            validDataFileDescription(Arrays.asList(dto));
            DataFileModel model = dto.newModel();  
            model.setFilename(fileContentDisposition.getFileName());
            dao.insertFile(model, file);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (MongoWriteException duplicateKey) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Duplicate Data", duplicateKey.getMessage())
                .getResponse();
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        } catch (NoSQLInvalidUriListException e) {
            throw new NotFoundException(e.getMessage());
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
    @Path("description")
    @ApiOperation(value = "Describe datafiles and give their relative paths in the configured storage system. In the case of already stored datafiles.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file(s) metadata(s) saved", response = URI.class)})
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFilePaths(
            @ApiParam(value = "Metadata of the file", required = true) @NotNull @Valid List<DataFilePathCreationDTO> dtoList,
            @Context HttpServletRequest context
    ) throws Exception {  
        
        DataDAO dao = new DataDAO(nosql, sparql, fs);        

        try {
            if (dtoList.size()> DataAPI.SIZE_MAX) {
                throw new NoSQLTooLargeSetException(DataAPI.SIZE_MAX, dtoList.size());
            }
            validDataFileDescription(dtoList);
            List<DataFileModel> dataList = new ArrayList();
            for(DataFilePathCreationDTO dto : dtoList ){            
                DataFileModel model = dto.newModel();
                // get the the absolute file path according to the fileStorageDirectory
                java.nio.file.Path absoluteFilePath = fs.getAbsolutePath(FS_FILE_PREFIX, Paths.get(model.getPath()));

                if (model.getArchive() == null && !fs.exist(FS_FILE_PREFIX, absoluteFilePath)) {
                    return new ErrorResponse(
                                Response.Status.BAD_REQUEST,
                                "File not found",
                                absoluteFilePath.toString()
                    ).getResponse();
                }

                model.setFilename(absoluteFilePath.getFileName().toString());
                dataList.add(model);
            }

            dataList = (List<DataFileModel>) dao.createAllFiles(dataList);
            List<URI> createdResources = new ArrayList<>();
            for (DataModel data : dataList){
                createdResources.add(data.getUri());
            }          

            return new ObjectUriResponse(Response.Status.CREATED, createdResources).getResponse();

        } catch(NoSQLTooLargeSetException ex) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_SIZE_LIMIT",
                ex.getMessage()).getResponse();

        } catch (MongoBulkWriteException duplicateError) {
            List<DataFilePathCreationDTO> datas = new ArrayList();
            List<BulkWriteError> errors = duplicateError.getWriteErrors();
            for (int i=0 ; i < errors.size() ; i++) {
                int index = errors.get(i).getIndex();
                datas.add(dtoList.get(index));
            }                    
            ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
            String json = mapper.writeValueAsString(datas);

            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", json)
            .getResponse();

        } catch (MongoCommandException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getErrorMessage())
            .getResponse();
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        }       

    }

    @GET
    @Path("count")
    @ApiOperation("Count datafiles")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of datafiles associated to a given target", response = Integer.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countDatafiles(
            @ApiParam(value = "Target URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("target") List<URI> target,
        @ApiParam(value = "Device URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("device") List<URI> device) throws  Exception {

        DataDAO dao = new DataDAO(nosql, sparql, fs);
        int datafileCount = dao.countFiles(null, null, null, target, null, device, null, null, null);
        return new SingleObjectResponse<>(datafileCount).getResponse();
    }
    
    /**
     * Returns the content of the file corresponding to the URI given.
     *
     * @param uri
     * @param response
     * @return The file content or null with a 404 status if it doesn't exists
     */
    @ApiProtected
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a data file")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getDataFile(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("uri") @NotNull URI uri,
            @Context HttpServletResponse response
    ) throws NotFoundURIException, IOException, URISyntaxException {
        try {
            DataDAO dao = new DataDAO(nosql, sparql, fs);

            DataFileModel description = dao.getFile(uri);

            java.nio.file.Path filePath = Paths.get(description.getPath());
            byte[] fileContent = fs.readFileAsByteArray(FS_FILE_PREFIX, filePath);
            if(description.getArchive() != null) {
                return Response.status(Response.Status.NOT_IMPLEMENTED.getStatusCode()).build();
            }
            if (ArrayUtils.isEmpty(fileContent)) {
                return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
            }
            return Response.ok(fileContent, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + filePath.getFileName().toString() + "\"") //optional
                    .build();
            
        } catch (NoSQLInvalidURIException e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();           
        } catch (IOException e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        } 
    }
    
    /**
     * This service returns the description of a file corresponding to the URI given.
     *
     * @param uri
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
    @Path("{uri}/description")
    @ApiOperation(value = "Get a data file description")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file description", response = DataFileGetDTO.class),})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescription(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        try {
            DataFileModel description = dao.getFile(uri);
            return new SingleObjectResponse<>(DataFileGetDTO.fromModel(description)).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown file URI ", uri);   
        }
    }
    
    /**
     * Returns a thumbnail based on the content of the file corresponding to the URI given.The given URI must link to a picture.
     *
     * @param uri      the {@link URI} of the file to download
     * @param scaledHeight the height of the thumbnail to return
     * @param scaledWidth  the width of the thumbnail to return
     * @param response
     * @return The file content or null with a 404 status if it doesn't exists
     * @throws java.lang.Exception
     */
    @ApiProtected
    @GET
    @Path("{uri}/thumbnail")
    @ApiOperation(value = "Get a picture thumbnail")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve thumbnail of a picture"),
        @ApiResponse(code = 400, message = "Error while building the thumbnail, this error can occur when the file requested is not an image"),
        @ApiResponse(code = 404, message = "the image has not been found")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getPicturesThumbnails(
            @ApiParam(value = "Search by fileUri", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Thumbnail width") @QueryParam("scaled_width") @Min(256) @Max(1920) @DefaultValue("640") Integer scaledWidth,
            @ApiParam(value = "Thumbnail height") @QueryParam("scaled_height") @Min(144) @Max(1080) @DefaultValue("360") Integer scaledHeight,
            @Context HttpServletResponse response) throws Exception {

        DataDAO dao = new DataDAO(nosql, sparql, fs);
        
        try {
            DataFileModel description = dao.getFile(uri);

            Pattern pattern = Pattern.compile("(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP)$") ;
            if( pattern.matcher(description.getFilename()).matches()) {
                byte[] image = fs.readFileAsByteArray(FS_FILE_PREFIX, Paths.get(description.getPath()));

                byte[] imageData = ImageResizer.getInstance().resize(
                        image,
                        scaledWidth,
                        scaledHeight
                );

                return Response.ok(imageData, MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Disposition", "attachment; filename=\"" + description.getFilename() + "\"") //optional
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
            }




        } catch (NoSQLInvalidURIException e) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        } catch (java.io.IOException e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }
    }
    
    /**
     * This service searches for file descriptions according to the search parameters given.
     *
     * @param pageSize
     * @param targets
     * @param provenances
     * @param timezone
     * @param experiments
     * @param page
     * @param devices
     * @param rdfType
     * @param metadata
     * @param orderByList
     * @param startDate
     * @param endDate
     * @return List of file description
     * @throws java.lang.Exception
     */
    @GET
    @ApiOperation(value = "Search data files")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file descriptions", response = DataFileGetDTO.class, responseContainer = "List")
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescriptionsBySearch(
            @ApiParam(value = "Search by rdf type uri") @QueryParam("rdf_type") URI rdfType,
            @ApiParam(value = "Search by minimal date", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DataAPI.DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DataAPI.DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiments", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris list", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> targets,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by provenance uris list", example = DataAPI.DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DataAPI.DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize

    ) throws Exception {
        return  searchDataFiles(rdfType, startDate, endDate, timezone, experiments, targets, devices, provenances, metadata, orderByList, page, pageSize);
    }

    @POST
    @Path("by_targets")
    @ApiOperation("Search data files for a large list of targets ")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data file list", response = DataFileGetDTO.class, responseContainer = "List")
    })
    public Response getDataFileDescriptionsByTargets(
            @ApiParam(value = "Search by rdf type uri") @QueryParam("rdf_type") URI rdfType,
            @ApiParam(value = "Search by minimal date", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DataAPI.DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DataAPI.DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiments", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by provenance uris list", example = DataAPI.DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DataAPI.DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "Search by targets uris list")  List<URI> targets
            ) throws Exception {

        if (targets == null) {
            targets = new ArrayList<>();
        }
        return  searchDataFiles(rdfType, startDate, endDate, timezone, experiments, targets, devices, provenances, metadata, orderByList, page, pageSize);
    }

    private Response searchDataFiles(
            URI rdfType,
            String startDate,
            String endDate,
            String timezone,
            List<URI> experiments,
            List<URI> targets,
            List<URI> devices,
            List<URI> provenances,
            String metadata,
            List<OrderBy> orderByList,
            int page,
            int pageSize

    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try  {
                startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
            } catch (DateValidationException e) {
                return new DateMappingExceptionResponse().toResponse(e);
            }
        }

        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "METADATA_PARAM_ERROR", "unable to parse metadata")
                        .getResponse();
            }
        }

        OntologyDAO ontoDao = new OntologyDAO(sparql);
        Set<URI> rdfTypes = new HashSet<>();

        if (rdfType != null) {
//            rdfTypes = ontoDao.getSubclassRdfTypes(rdfType, user.getLanguage());
            OntologyStore cache = SPARQLModule.getOntologyStoreInstance();
            SPARQLTreeListModel<ClassModel> treeList = cache.searchSubClasses(rdfType, null, user.getLanguage(), false);
            treeList.traverse(classModel -> rdfTypes.add(URI.create(SPARQLDeserializers.getExpandedURI(classModel.getUri()))));
            rdfTypes.add(URI.create(SPARQLDeserializers.getExpandedURI(rdfType))); // fix root
//            List<ResourceTreeDTO> treeDto = ResourceTreeDTO.fromResourceTree(treeList);
        }

        ListWithPagination<DataFileModel> resultList = dao.searchFiles(
                user,
                new ArrayList<>(rdfTypes),
                experiments,
                targets,
                provenances,
                devices,
                startInstant,
                endInstant,
                metadataFilter,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<DataFileGetDTO> resultDTOList = resultList.convert(DataFileGetDTO.class, DataFileGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();

    }

    private <T extends DataFileCreationDTO> void validDataFileDescription(List<T> dtoList) throws Exception {
        Set<URI> objectURIs = new HashSet<>();
        Set<URI> notFoundedObjectURIs = new HashSet<>();
        Set<URI> provenanceURIs= new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs= new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        Set<URI> fileTypes= new HashSet<>();
        Set<URI> notFoundedFileTypes = new HashSet<>();
        
        for (DataFileCreationDTO dto : dtoList) {          
            
            //check objects uri
            if (dto.getTarget() != null) {
                if (!objectURIs.contains(dto.getTarget())) {
                    objectURIs.add(dto.getTarget());
                    if (!sparql.uriExists((Node) null, dto.getTarget())) {
                        notFoundedObjectURIs.add(dto.getTarget());
                    }
                }         
            }
        
            //check provenance uri
            ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);
            if (!provenanceURIs.contains(dto.getProvenance().getUri())) {
                provenanceURIs.add(dto.getProvenance().getUri());
                if (!provDAO.provenanceExists(dto.getProvenance().getUri())) {
                    notFoundedProvenanceURIs.add(dto.getProvenance().getUri());
                }
            }        
            
            // check experiments uri
            if (dto.getProvenance().getExperiments() != null) {
                for (URI exp:dto.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }    
                    } 
                }
            }
            
            // check rdfType
            if (!fileTypes.contains(dto.getRdfType())) {
                fileTypes.add(dto.getRdfType());
                if (!sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(dto.getRdfType()), Ontology.subClassAny, Oeso.Datafile)
                    )
                ) {
                    notFoundedFileTypes.add(dto.getRdfType());
                }
            }
            
        }      
        
        if (!notFoundedObjectURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong target uris", new ArrayList<>(notFoundedObjectURIs));
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris: ", new ArrayList<>(notFoundedProvenanceURIs));
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris: ", new ArrayList<>(notFoundedExpURIs));
        }
        if (!notFoundedFileTypes.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong rdf_types: ", new ArrayList<>(notFoundedFileTypes));
        }
    }
 
    @GET
    @Path("provenances")
    @ApiOperation("Search provenances linked to datafiles")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getDatafilesProvenances(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> targets,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices            
    ) throws Exception {
        return searchDatafilesProvenances(experiments, targets, devices);
    }

    @POST
    @Path("provenances/by_targets")
    @ApiOperation("Search provenances linked to datafiles for a large list of targets")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getDatafilesProvenancesByTargets(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by targets uris") List<URI> targets
            ) throws Exception {
        if (targets == null) {
            targets = new ArrayList<>();
        }
        return searchDatafilesProvenances(experiments, targets, devices);
    }

    private Response searchDatafilesProvenances(
            List<URI> experiments,
            List<URI> targets,
            List<URI> devices
    ) throws Exception {

        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        Set<URI> provenanceURIs = dataDAO.getDatafileProvenances(user, experiments, targets, devices);

        ProvenanceDAO provenanceDAO = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> resultList = provenanceDAO.getListByURIs(new ArrayList<>(provenanceURIs));
        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>();

        resultList.forEach(result -> {
            resultDTOList.add(ProvenanceGetDTO.fromModel(result));
        });
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
}
