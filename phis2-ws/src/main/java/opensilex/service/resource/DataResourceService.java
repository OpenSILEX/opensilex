//******************************************************************************
//                            DataResourceService.java
// SILEX-PHIS
// Copyright © INRAE 2020
// Creation date: February 2020
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import opensilex.service.PropertiesFileManager;
import opensilex.service.configuration.DateFormat;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.DataDAO;
import opensilex.service.dao.FileDescriptionDAO;
import opensilex.service.dao.ProvenanceDAO;
import opensilex.service.dao.ScientificObjectRdf4jDAO;
import opensilex.service.dao.VariableDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.data.DataDTO;
import opensilex.service.resource.dto.data.DataPostDTO;
import opensilex.service.resource.dto.data.FileDescriptionDTO;
import opensilex.service.resource.dto.data.FileDescriptionPostDTO;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.model.Data;
import opensilex.service.model.FileDescription;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.data.DataSearchDTO;
import opensilex.service.resource.dto.data.FileDescriptionWebPathPostDTO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.sparql.service.SPARQLService;

/**
 * Data resource service.
 *
 * @Author Arnaud Charleroy
 */
@Api("/data")
@Path("/data")
public class DataResourceService extends ResourceService {

    @Inject
    private FileStorageService fs;

    @Inject
    public DataResourceService(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    /**
     * Service to insert data.
     *
     * @param data
     * @example
     * [
     *  {
     *      "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/s18521",
     *      "variableUri": "http://www.phenome-fppn.fr/id/variables/v001",
     *      "date": "2017-06-15T10:51:00+0200",
     *      "value": "0.5"
     *  }
     * ]
     * @param context
     * @return the insertion result.
     */
    @POST
    @ApiOperation(value = "Post data",
            notes = "Register data in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "data saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postData(
            @ApiParam(value = DocumentationAnnotation.DATA_POST_DEFINITION) @Valid List<DataPostDTO> data,
            @Context HttpServletRequest context) {

        AbstractResultForm postResponse = null;

        try {
            if (data != null && !data.isEmpty()) {
                DataDAO dataDAO = new DataDAO(sparql);

                dataDAO.user = userSession.getUser();

                POSTResultsReturn result = dataDAO.checkAndInsert(dataDTOsToData(data));

                if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                    postResponse.getMetadata().setDatafiles(result.getCreatedResources());
                } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || result.getHttpStatus().equals(Response.Status.OK)
                        || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(result.statusList);
                }
                return Response.status(result.getHttpStatus()).entity(postResponse).build();
            } else {
                postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "No data to add"));
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }
        } catch (ParseException e) {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Generates an data list from a given list of DataPostDTO.
     *
     * @param dataDTOs
     * @return the list of data
     */
    private List<Data> dataDTOsToData(List<DataPostDTO> dataDTOs) throws ParseException {
        ArrayList<Data> dataList = new ArrayList<>();

        for (DataPostDTO dataDTO : dataDTOs) {
            dataList.add(dataDTO.createObjectFromDTO());
        }

        return dataList;
    }

    /**
     * Service to get scientific data
     *
     * @param pageSize
     * @param page
     * @param variable
     * @param startDate
     * @param endDate
     * @param object
     * @param provenance
     * @param dateSortAsc
     * @param requestContext
     * @return list of the data corresponding to the search params given
     * @example
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *               {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/id/data/d2plf65my4rc2odiv2lbjgukc2zswkqyoddh25jtoy4b5pf3le3q4ec5c332f5cd44ce82977e404cebf83c",
     *                 "provenanceUri": "http://www.phenome-fppn.fr/mtp/2018/pv181515071552",
     *                 "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *                 "variableUri": "http://www.phenome-fppn.fr/diaphen/id/variables/v009",
     *                 "date": "2017-06-15T00:00:00+0200",
     *                 "value": 2.4
     *               },
     *               {
     *                 "uri": "http://www.phenome-fppn.fr/diaphen/id/data/pttdrrqybxoyku4img323dyrhmpp267mhnpiw3vld2wm6tap3vwq93b344c429ec45bb9b185edfe5bc2b64",
     *                 "provenanceUri": "http://www.phenome-fppn.fr/mtp/2018/pv181515071552",
     *                 "objectUri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *                 "variableUri": "http://www.phenome-fppn.fr/diaphen/id/variables/v009",
     *                 "date": "2017-06-16T00:00:00+0200",
     *                 "value": "2017-06-15T00:00:00+0200"
     *               }
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get data corresponding to the search parameters given.",
            notes = "Retrieve all data corresponding to the search parameters given,"
            + "<br/>Date parameters could be either a datetime like: " + DocumentationAnnotation.EXAMPLE_XSDDATETIME
            + "<br/>or simply a date like: " + DocumentationAnnotation.EXAMPLE_DATE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all data", response = Data.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getData(
            @ApiParam(value = "Search by variable uri", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI, required = true) @QueryParam("variable") @URL @Required String variable,
            @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
            @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
            @ApiParam(value = "Search by object uri", example = DocumentationAnnotation.EXAMPLE_SENSOR_URI) @QueryParam("object") @URL String object,
            @ApiParam(value = "Search by provenance uri", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI) @QueryParam("provenance") @URL String provenance,
            @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) {
        // 1. Initialize dataDAO with parameters
        DataDAO dataDAO = new DataDAO(sparql);

        dataDAO.variableUri = variable;

        dataDAO.startDate = startDate;
        dataDAO.endDate = endDate;
        dataDAO.objectUri = object;
        dataDAO.provenanceUri = provenance;
        dataDAO.dateSortAsc = dateSortAsc;

        dataDAO.user = userSession.getUser();
        dataDAO.setPage(page);
        dataDAO.setPageSize(pageSize);

        // 2. Get data count
        int totalCount = dataDAO.count();

        // 3. Get data page list
        ArrayList<Data> dataList = dataDAO.allPaginate();

        // 4. Initialize return variables
        ArrayList<DataDTO> list = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<DataDTO> getResponse;

        if (dataList == null) {
            // Request failure
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (dataList.isEmpty()) {
            // No results
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            // Convert all measures object to DTO's
            dataList.forEach((data) -> {
                list.add(new DataDTO(data));
            });

            // Return list of DTO
            getResponse = new ResultForm<>(dataDAO.getPageSize(), dataDAO.getPage(), list, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    /**
     * Saves data file with its metadata and use MULTIPART_FORM_DATA for it.
     * fileContentDisposition parameter is automatically created from submitted file.
     * No example could be provided for this kind of MediaType 
     * @param descriptionDto
     * @param file
     * @param fileContentDisposition
     * @return the insertion result.
     */
    @POST
    @Path("file")
    @ApiOperation(value = "Post data file")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiProtected
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFile(
            @ApiParam(value = "File description with metadata", required = true, type = "string") @NotNull @Valid @FormDataParam("description") FileDescriptionPostDTO descriptionDto,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") File file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition
    ) throws Exception {
        FileDescriptionDAO fileDescriptionDao = new FileDescriptionDAO(sparql);
        AbstractResultForm postResponse = null;
        try {
            FileDescription description = descriptionDto.createObjectFromDTO();
            description.setFilename(fileContentDisposition.getFileName());
            POSTResultsReturn result = fileDescriptionDao.checkAndInsert(
                    description,
                    file,
                    fs
            );

            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }

            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } catch (ParseException e) {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Save the metadata of a file already stored in an accessible storage.
     * The absolute path of the file will be ${ws.updir.doc property}/relativePath
     * ${ws.updir.doc property} refers to the <ws.updir.doc> property defined in the config.properties file used for building the webservice.
     * @param descriptionsDto
     * @param context
     * @example [ { "rdfType":
     * "http://www.opensilex.org/vocabulary/oeso#HemisphericalImage", "date":
     * "2017-06-15T10:51:00+0200", "provenanceUri":
     * "http://www.opensilex.org/opensilex/id/provenance/1551805521606",
     * "relativePath" : "4P/464/proc.txt", "concernedItems": [{ "uri":
     * "http://www.opensilex.org/demo/DMO2018-1", "typeURI":
     * "http://www.opensilex.org/vocabulary/oeso#Experiment" }], "metadata": {
     * "sensorUri": "http://www.phenome-fppn.fr/diaphen/2018/s18001" } } ]
     * @return the insertion result.
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": [
     *              "http://www.opensilex.org/opensilex/id/dataFile/HemisphericalImage/ynckimhx54ejoppqewxw2o4aje44kdfvsaimdkptypznrzzbreoa45ae8ad4836741e0ad1a48838bb525bb"
     *          ]
     *      }
     * }
     */
    @POST
    @Path("filepaths")
    @ApiOperation(value = "Post data about existing files")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file(s) metadata(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDataFilePaths(
            @ApiParam(value = "Metadata of the file", required = true) @NotNull @Valid List<FileDescriptionWebPathPostDTO> descriptionsDto,
            @Context HttpServletRequest context
    ) throws Exception {
        FileDescriptionDAO fileDescriptionDao = new FileDescriptionDAO(sparql);
        AbstractResultForm postResponse = null;
        POSTResultsReturn result = new POSTResultsReturn();

        try {
            String fileStorageDirectory = PropertiesFileManager.getConfigFileProperty("service", "uploadFileServerDirectory");
            List<FileDescription> descriptions = new ArrayList<>();
            Optional<String> checkFsError = Optional.empty();

            for (FileDescriptionWebPathPostDTO description : descriptionsDto) {
                FileDescription fileDescription = description.createObjectFromDTO();
                // get the the absolute file path according to the fileStorageDirectory
                File realFile = Paths.get(fileStorageDirectory + '/' + fileDescription.getPath()).toFile();
                
                checkFsError = checkFilePath(realFile);
                if (checkFsError.isPresent()) { // check if a error msg was returned
                    break;
                }
                fileDescription.setPath(realFile.getAbsolutePath());
                fileDescription.setFilename(realFile.getName());
                descriptions.add(fileDescription);
            }

            if (!checkFsError.isPresent()) { // check if a error msg was returned
                result = fileDescriptionDao.checkAndInsertWithWebPath(descriptions); // insert description with DAO
            } else {
                result.setErrorMsg(checkFsError.get());
                result.setDataState(false);
                result.setHttpStatus(Response.Status.BAD_REQUEST);
            }

            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }

            return Response.status(result.getHttpStatus()).entity(postResponse).build();
        } catch (ParseException e) {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, e.getMessage()));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Check if a file exists, if it's a file and if it's readable
     *
     * @param file : the file to check
     * @return a {@link Optional} of {@link String} which describe an error msg
     * if an error was encountered <br> return {@link Optional#empty()} else
     *
     */
    protected Optional<String> checkFilePath(File file) {

        if (!file.exists()) {
            return Optional.of("File not found : " + file.getPath());
        } else if (!file.isFile()) {
            return Optional.of(file.getPath() + " is not a file");
        } else if (!file.canRead()) {
            return Optional.of(file.getPath() + " is not readable");
        }
        return Optional.empty();
    }

    /**
     * Returns the content of the file corresponding to the URI given. No
     * authentication on this service because image file must be accessible
     * directly.
     *
     * @param fileUri
     * @param response
     * @return The file content or null with a 404 status if it doesn't exists
     */
    @GET
    @Path("file/{fileUri}")
    @ApiOperation(value = "Get data file")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 404, message = DocumentationAnnotation.FILE_NOT_FOUND),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getDataFile(
            @ApiParam(value = "Search by fileUri", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @PathParam("fileUri") @URL @Required String fileUri,
            @Context HttpServletResponse response
    ) throws Exception {
        FileDescriptionDAO dataFileDao = new FileDescriptionDAO(sparql);

        FileDescription description = dataFileDao.findFileDescriptionByUri(fileUri);

        if (description == null) {
            return Response.status(404).build();
        }

        try {
            FileInputStream stream = new FileInputStream(new File(description.getPath()));

            return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + description.getFilename() + "\"") //optional
                    .build();
        } catch (FileNotFoundException ex) {
            return Response.status(404).build();
        }
    }

    /**
     * This service returns the description of a file corresponding to the URI given.
     * @param fileUri
     * @param response
     * @return the file description
     * @example 
     * {
     *    "uri": "http://www.phenome-fppn.fr/diaphen/id/dataFile/RGBImage/55fjbbmtmr4m3kkizslzaddfkdt2ranum3ikz6cdiajqzfdc7yqa31d87b83efac4c358ceb5b0da6ed27ff",
     *    "rdfType": "http://www.opensilex.org/vocabulary/oeso#RGBImage",
     *    "date": "2017-06-15T10:51:00+0200",
     *    "concernedItems": [{
     *      "uri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *      "typeURI": "http://www.opensilex.org/vocabulary/oeso#Plot"
     *    }],
     *    "provenanceUri": "http://www.phenome-fppn.fr/diaphen/id/provenance/1552405256945",
     *    "metadata": {
     *      "sensor": "http://www.phenome-fppn.fr/diaphen/2018/s18035",
     *      "position": "1"
     *    }
     * }
     */
    @GET
    @Path("file/{fileUri}/description")
    @ApiOperation(value = "Get data file description")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file description", response = FileDescriptionDTO.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 404, message = DocumentationAnnotation.FILE_NOT_FOUND),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescription(
            @ApiParam(value = "Search by fileUri", required = true, example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI)
            @PathParam("fileUri") @URL @Required String fileUri,
            @Context HttpServletResponse response
    ) throws Exception {
        FileDescriptionDAO fileDescriptionDao = new FileDescriptionDAO(sparql);

        FileDescription description = fileDescriptionDao.findFileDescriptionByUri(fileUri);

        if (description == null) {
            return Response.status(404).build();
        }

        return Response.status(Response.Status.OK).entity(new FileDescriptionDTO(description)).build();
    }

    /**
     * This service searches for file descriptions according to the search parameters given.
     * @param pageSize
     * @param page
     * @param rdfType
     * @param startDate
     * @param endDate
     * @param provenance
     * @param concernedItems
     * @param jsonValueFilter
     * @param dateSortAsc
     * @return List of file description
    * @example 
     * [{
     *    "uri": "http://www.phenome-fppn.fr/diaphen/id/dataFile/RGBImage/55fjbbmtmr4m3kkizslzaddfkdt2ranum3ikz6cdiajqzfdc7yqa31d87b83efac4c358ceb5b0da6ed27ff",
     *    "rdfType": "http://www.opensilex.org/vocabulary/oeso#RGBImage",
     *    "date": "2017-06-15T10:51:00+0200",
     *    "concernedItems": [{
     *      "uri": "http://www.phenome-fppn.fr/diaphen/2018/o18001199",
     *      "typeURI": "http://www.opensilex.org/vocabulary/oeso#Plot"
     *    }],
     *    "provenanceUri": "http://www.phenome-fppn.fr/diaphen/id/provenance/1552405256945",
     *    "metadata": {
     *      "sensor": "http://www.phenome-fppn.fr/diaphen/2018/s18035",
     *      "position": "1"
     *    }
     * }]
     */
    @GET
    @Path("file/search")
    @ApiOperation(value = "Retrieve data file descriptions corresponding to the search parameters given.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve file descriptions", response = FileDescriptionDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFileDescriptionsBySearch(
            @ApiParam(value = "Search by rdf type uri", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI, required = true) @QueryParam("rdfType") @URL @Required String rdfType,
            @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
            @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
            @ApiParam(value = "Search by provenance uri", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI) @QueryParam("provenance") @URL String provenance,
            @ApiParam(value = "Search by concerned items uri", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI) @QueryParam("concernedItems") @URL List<String> concernedItems,
            @ApiParam(value = "Search by json filter", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_METADATA) @QueryParam("jsonValueFilter") String jsonValueFilter,
            @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page
    ) throws Exception {
        FileDescriptionDAO fileDescriptionDao = new FileDescriptionDAO(sparql);

        // 1. Set all varaibles corresponding to the search
        fileDescriptionDao.user = userSession.getUser();
        fileDescriptionDao.setPage(page);
        fileDescriptionDao.setPageSize(pageSize);

        // 2. Get data count
        long totalCount = fileDescriptionDao.count(
                rdfType,
                startDate,
                endDate,
                provenance,
                jsonValueFilter,
                concernedItems,
                dateSortAsc
        );

        // 3. Get data page list
        ArrayList<FileDescription> dataList = fileDescriptionDao.search(
                rdfType,
                startDate,
                endDate,
                provenance,
                jsonValueFilter,
                concernedItems,
                dateSortAsc
        );

        // 4. Initialize return variables
        ArrayList<FileDescriptionDTO> list = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<FileDescriptionDTO> getResponse;

        if (dataList == null) {
            // Request failure
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (dataList.isEmpty()) {
            // No results
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            // Convert all measures object to DTO's
            dataList.forEach((data) -> {
                list.add(new FileDescriptionDTO(data));
            });

            // Return list of DTO
            getResponse = new ResultForm<>(fileDescriptionDao.getPageSize(), fileDescriptionDao.getPage(), list, true, (int) totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }

    /**
     * Service to search data
     *
     * @param pageSize
     * @param page
     * @param variableUri
     * @param startDate
     * @param endDate
     * @param objectUri
     * @param objectLabel
     * @param provenanceUri
     * @param provenanceLabel
     * @param dateSortAsc
     * @return list of the data corresponding to the search params given
     * @example
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 3,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                 "uri": "http://www.opensilex.org/opensilex/id/data/k3zilz2rrjhkxo4ppy43372rr5hyrbehjuf2stecbekvkxyqcjdq84b1df953972418a8d5808ba2bca3baedfsf",
     *                 "provenance": {
     *                      "uri": "http://www.opensilex.org/opensilex/id/provenance/1552386023784",
     *                      "label": "provenance-label"
     *                  },
     *                 "object": {
     *                     "uri": "http://www.opensilex.org/opensilex/2019/o19000060",
     *                     "labels": [
     *                         "2"
     *                     ]
     *                  },
     *                 "variable": {
     *                     "uri": "http://www.opensilex.org/opensilex/id/variables/v001",
     *                     "label": "trait_method_unit"
     *                 },
     *                 "date": "2014-01-04T00:55:00+0100",
     *                 "value": "19"
     *              },
     *          ]
     *      }
     *  }
     */
    @GET
    @Path("search")
    @ApiOperation(value = "Get data corresponding to the search parameters given.",
            notes = "Retrieve all data corresponding to the search parameters given,"
            + "<br/>Date parameters could be either a datetime like: " + DocumentationAnnotation.EXAMPLE_XSDDATETIME
            + "<br/>or simply a date like: " + DocumentationAnnotation.EXAMPLE_DATE)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all data", response = Data.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataSearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by variable uri", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("variableUri") @URL @Required String variableUri,
            @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
            @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
            @ApiParam(value = "Search by object uri", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_URI) @QueryParam("objectUri") @URL String objectUri,
            @ApiParam(value = "Search by object label", example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_ALIAS) @QueryParam("objectLabel") String objectLabel,
            @ApiParam(value = "Search by provenance uri", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI) @QueryParam("provenanceUri") @URL String provenanceUri,
            @ApiParam(value = "Search by provenance label", example = DocumentationAnnotation.EXAMPLE_PROVENANCE_LABEL) @QueryParam("provenanceLabel") String provenanceLabel,
            @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc
    ) throws Exception {
        ArrayList<DataSearchDTO> list = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<DataSearchDTO> getResponse;

        DataDAO dataDAO = new DataDAO(sparql);

        List<String> objectsUris = new ArrayList<>();
        List<String> provenancesUris = new ArrayList<>();

        Map<String, List<String>> objectsUrisAndLabels = new HashMap<>();
        Map<String, String> provenancesUrisAndLabels = new HashMap<>();

        //1. Get list of objects uris corresponding to the label given if needed.
        ScientificObjectRdf4jDAO scientificObjectDAO = new ScientificObjectRdf4jDAO(sparql);
        if (objectUri != null && !objectUri.isEmpty()) {
            objectsUrisAndLabels.put(objectUri, scientificObjectDAO.findLabelsForUri(objectUri));
        } else if (objectLabel != null && !objectLabel.isEmpty()) { //We need to get the list of the uris of the scientific object with this label (like)
            objectsUrisAndLabels = scientificObjectDAO.findUriAndLabelsByLabelAndRdfType(objectLabel, Oeso.CONCEPT_SCIENTIFIC_OBJECT.toString());
        }

        for (String uri : objectsUrisAndLabels.keySet()) {
            objectsUris.add(uri);
        }

        //2. Get list of provenances uris corresponding to the label given if needed.
        ProvenanceDAO provenanceDAO = new ProvenanceDAO(sparql);
        if (provenanceUri != null && !provenanceUri.isEmpty()) {
            //If the provenance URI is given, we need the provenance label
            provenancesUris.add(provenanceUri);
        } else if (provenanceLabel != null && !provenanceLabel.isEmpty()) {
            //If the provenance URI is empty and a label is given, we search the provenance(s) with the given label (like)
            provenancesUrisAndLabels = provenanceDAO.findUriAndLabelsByLabel(provenanceLabel);
        }

        for (String uri : provenancesUrisAndLabels.keySet()) {
            provenancesUris.add(uri);
        }

        //3. Get variable label
        VariableDAO variableDAO = new VariableDAO(sparql);
        if (!variableDAO.existAndIsVariable(variableUri)) {
            // Request failure
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            statusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown variable URI : " + variableUri));
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
        }
        String variableLabel = variableDAO.findLabelsForUri(variableUri).get(0);

        //4. Get count
        Integer totalCount = dataDAO.count(variableUri, startDate, endDate, objectsUris, provenancesUris);

        //5. Get data
        List<Data> dataList = dataDAO.find(page, pageSize, variableUri, startDate, endDate, objectsUris, provenancesUris);

        //6. Return result
        if (dataList == null) {
            // Request failure
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else if (dataList.isEmpty()) {
            // No results
            getResponse = new ResultForm<>(0, 0, list, true, 0);
            return noResultFound(getResponse, statusList);
        } else {
            // Convert all data object to DTO's
            for (Data data : dataList) {
                if (data.getObjectUri() != null && !objectsUrisAndLabels.containsKey(data.getObjectUri())) {
                    //We need to get the labels of the object
                    objectsUrisAndLabels.put(data.getObjectUri(), scientificObjectDAO.findLabelsForUri(data.getObjectUri()));
                }

                if (!provenancesUrisAndLabels.containsKey(data.getProvenanceUri())) {
                    //We need to get the label of the provenance
                    provenancesUrisAndLabels.put(data.getProvenanceUri(), provenanceDAO.findLabelByUri(data.getProvenanceUri()));
                }

                //Get provenance label
                String dataProvenanceLabel = provenancesUrisAndLabels.get(data.getProvenanceUri());
                //Get object labels
                List<String> dataObjectLabels = new ArrayList<>();
                if (objectsUrisAndLabels.get(data.getObjectUri()) != null) {
                    dataObjectLabels = objectsUrisAndLabels.get(data.getObjectUri());
                }

                list.add(new DataSearchDTO(data, dataProvenanceLabel, dataObjectLabels, variableLabel));
            }

            // Return list of DTO
            getResponse = new ResultForm<>(pageSize, page, list, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
