//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.result.DeleteResult;
import com.opencsv.CSVWriter;
import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.bll.DataExportInformation;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.bll.DataLongExportInformation;
import org.opensilex.core.data.bll.DataWideExportInformation;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.device.api.DeviceAPI;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.variable.api.VariableGetDTO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.*;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.pagination.PaginatedSearchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author sammy
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/data")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class DataAPI {

    public static final String PATH = "/core/data";

    protected final static Logger LOGGER = LoggerFactory.getLogger(DataAPI.class);

    public static final String CREDENTIAL_DATA_GROUP_ID = "Data";
    public static final String CREDENTIAL_DATA_GROUP_LABEL_KEY = "credential-groups.data";

    public static final String DATA_EXAMPLE_URI = "http://opensilex.dev/id/data/1598857852858";
    public static final String DATA_EXAMPLE_OBJECTURI = "http://opensilex.dev/opensilex/2020/o20000345";
    public static final String DATA_EXAMPLE_VARIABLEURI = "http://opensilex.dev/variable#variable.2020-08-21_11-21-23entity6_method6_quality6_unit6";
    public static final String DATA_EXAMPLE_CONFIDENCE = "0.5";
    public static final String DATA_EXAMPLE_CONFIDENCE_MAX = "1";
    public static final String DATA_EXAMPLE_PROVENANCEURI = "http://opensilex.dev/provenance/1598001689415";
    public static final String DATA_EXAMPLE_VALUE = "8.6";
    public static final String DATA_EXAMPLE_MINIMAL_DATE = "2020-08-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_MAXIMAL_DATE = "2020-09-21T00:00:00+01:00";
    public static final String DATA_EXAMPLE_TIMEZONE = "Europe/Paris";
    public static final String DATA_EXAMPLE_METADATA = "{ \"LabelView\" : \"side90\",\n" +
            "\"paramA\" : \"90\"}";
    public static final String DATA_EXAMPLE_OPERATOR = "dev:id/user/isa.droits";

    public static final String CREDENTIAL_DATA_MODIFICATION_ID = "data-modification";
    public static final String CREDENTIAL_DATA_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_DATA_DELETE_ID = "data-delete";
    public static final String CREDENTIAL_DATA_DELETE_LABEL_KEY = "credential.default.delete";
    public static final int SIZE_MAX = 50000;
    
    Map<URI, URI> rootDeviceTypes = null;
    private final Map<DeviceModel, List<URI>> variablesToDevices = new HashMap<>();
    
    @Inject
    private MongoDBService nosql;

    @Inject
    private SPARQLService sparql;

    @Inject
    private FileStorageService fs;

    @CurrentUser
    AccountModel user;

    @POST
    @ApiProtected
    @ApiOperation("Add data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Add data", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response addListData(
            @ApiParam("Data description") @Valid @NotNull @NotEmpty List<DataCreationDTO> dtoList
    ) throws Exception {
        DataLogic dataBLL = new DataLogic(sparql, nosql, fs, user);

        try {
            if (dtoList.size() > SIZE_MAX) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_SIZE_LIMIT", "Single data import limit reached").getResponse();
            }
            List<DataModel> dataList = new ArrayList<>();

            for (DataCreationDTO dto : dtoList) {
                DataModel model = dto.newModel();
                dataList.add(model);
            }
            List<URI> createdResources = dataBLL.createMany(dataList);

            return new CreatedUriResponse(createdResources).getResponse();

        } catch (MongoDbUniqueIndexConstraintViolation duplicateError){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", duplicateError.getMessage())
                    .getResponse();
        }
        catch (MongoBulkWriteException someWriteException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "WRITE_ERROR", someWriteException.getMessage())
                    .getResponse();
        }
        catch (MongoException mongoException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "MONGO_EXCEPTION", mongoException.getMessage())
                    .getResponse();
        }
        catch(NoSQLAlreadyExistingUriException noSQLAlreadyExistingUriException){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_URI", noSQLAlreadyExistingUriException.getMessage())
                    .getResponse();
        }
        catch(URISyntaxException uriSyntaxException){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "URI_SYNTAX_ERROR", uriSyntaxException.getMessage())
                    .getResponse();
        }
        catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        } catch (DataTypeException | NoVariableDataTypeException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_TYPE_ERROR", e.getMessage())
                    .getResponse();
        } catch (NoSQLInvalidUriListException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data retrieved", response = DataGetDetailsDTO.class),
            @ApiResponse(code = 404, message = "Data not found", response = ErrorResponse.class)})
    public Response getData(
            @ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        DataLogic dataBLL = new DataLogic(sparql, nosql, fs, user);

        try {
            DataModel model = dataBLL.get(uri);
            DataGetDetailsDTO dto = DataGetDetailsDTO.getDtoFromModel(model, new VariableDAO(sparql, nosql, fs, user).getAllDateVariables());

            // fetch detailed information about publisher account
            if(model.getPublisher() != null){
                UserGetDTO userGetDTO = UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher()));
                dto.setPublisher(userGetDTO);
            }
            return new SingleObjectResponse<>(dto).getResponse();

        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
    }

    @GET
    @Path("mathematicalOperators")
    @ApiOperation("Get mathematical operators")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of mathematical operators", response = String.class, responseContainer = "List")})
    public Response getMathematicalOperators(){
        return new PaginatedListResponse<>(Arrays.stream(MathematicalOperator.values()).map(Enum::toString).collect(Collectors.toList())).getResponse();
    }

    private Set<URI> targetByGermplasmFilter(List<URI> targets , URI germplasmGroup, List<URI> germplasmUris, List<URI> experiments) throws Exception {

        //Get scientific objects associated to germplasms inside germplasmGroup if it's not null
        //Or/And scientific objects associated with passed germplasms

        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        Set<URI> finalTargetsFilter = new HashSet<>(targets);

        //If no experiments were passed we must only look for objects in experiments that the user is allowed to see
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        List<URI> xpForTargetSearch = experiments;

        if (CollectionUtils.isEmpty(experiments)) {

            ExperimentSearchFilter xpFilter = new ExperimentSearchFilter();
            xpFilter.setUser(user);
            int xpQuantity = experimentDAO.count();
            xpFilter.setPage(0);
            xpFilter.setPageSize(xpQuantity);

            xpForTargetSearch = experimentDAO.search(xpFilter)
                    .getList()
                    .stream()
                    .map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList());
        }

        List<URI> germplasmGroupTargets = scientificObjectDAO.getScientificObjectUrisAssociatedWithGermplasms(xpForTargetSearch, germplasmGroup, germplasmUris);
        finalTargetsFilter.addAll(germplasmGroupTargets);
        return finalTargetsFilter;
    }

    @POST
    @Path("search")
    @ApiOperation(
            value = "Search data for a large list of targets",
            notes = "Optimized search. The total count of element is not returned. Use countData (/count) service in order to get exact count of element"
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data list", response = DataGetSearchDTO.class, responseContainer = "List")
    })
    public Response searchDataListByTargets(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Targets uris, can be an empty array but can't be null", name = "targets") List<URI> targets,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Group filter") @QueryParam("group_of_germplasm") @ValidURI URI germplasmGroup,
            @ApiParam(value = "Search by operators", example = DATA_EXAMPLE_OPERATOR ) @QueryParam("operators") List<URI> operators,
            @ApiParam(value = "Targets uris, can be an empty array but can't be null", name = "germplasmUris") @QueryParam("germplasmUris") List<URI> germplasmUris,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    )throws Exception {
        return getDataList(
                startDate,
                endDate,
                timezone,
                experiments,
                targets,
                variables,
                devices,
                confidenceMin,
                confidenceMax,
                provenances,
                metadata,
                operators,
                germplasmGroup,
                germplasmUris,
                orderByList,
                page,
                pageSize,
                PaginatedSearchStrategy.HAS_NEXT_PAGE
        );
    }

    @POST
    @Path("by_targets")
    @ApiOperation(
            value = "Search data for a large list of targets",
            notes = "Deprecated. Use searchDataListByTargets (/search) service which is more optimized"
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data list", response = DataGetSearchDTO.class, responseContainer = "List")
    })
    @Deprecated(
            forRemoval = true
    )
    public Response getDataListByTargets(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Targets uris, can be an empty array but can't be null", name = "targets") List<URI> targets,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Group filter") @QueryParam("group_of_germplasm") @ValidURI URI germplasmGroup,
            @ApiParam(value = "Search by operators", example = DATA_EXAMPLE_OPERATOR ) @QueryParam("operators") List<URI> operators,
            @ApiParam(value = "Targets uris, can be an empty array but can't be null", name = "germplasmUris") @QueryParam("germplasmUris") List<URI> germplasmUris,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    )throws Exception {
        return getDataList(
                startDate,
                endDate,
                timezone,
                experiments,
                targets,
                variables,
                devices,
                confidenceMin,
                confidenceMax,
                provenances,
                metadata,
                operators,
                germplasmGroup,
                germplasmUris,
                orderByList,
                page,
                pageSize,
                PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH
        );
    }

    @GET
    @ApiOperation(
            value = "Search data",
            notes = "Deprecated. Use searchDataListByTargets (/search) service which is more optimized"
    )
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return data list", response = DataGetSearchDTO.class, responseContainer = "List")
    })
    @Deprecated(
            forRemoval = true
    )
    public Response searchDataList(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> targets,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Search by operators", example = DATA_EXAMPLE_OPERATOR ) @QueryParam("operators") List<URI> operators,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        return getDataList(
                startDate,
                endDate,
                timezone,
                experiments,
                targets,
                variables,
                devices,
                confidenceMin,
                confidenceMax,
                provenances,
                metadata,
                operators,
                null,
                null,
                orderByList,
                page,
                pageSize,
                PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH
        );
    }

    private Response getDataList(
            String startDate,
            String endDate,
            String timezone,
            List<URI> experiments,
            List<URI> targets,
            List<URI> variables,
            List<URI> devices,
            Float confidenceMin,
            Float confidenceMax,
            List<URI> provenances,
            String metadata,
            List<URI> operators,
            URI germplasmGroup,
            List<URI> germplasmUris,
            List<OrderBy> orderByList,
            int page,
            int pageSize,
            PaginatedSearchStrategy paginationStrategy) throws Exception{

        DataSearchFilter filter;

        try{
            filter = getSearchFilter(startDate, endDate, timezone, experiments, targets, variables, devices, confidenceMin, confidenceMax, provenances, metadata, operators, germplasmGroup, germplasmUris, orderByList, page, pageSize);
            if(filter == null){
                return new PaginatedListResponse<>(new ListWithPagination<>(page, pageSize)).getResponse();
            }
        }catch (DateValidationException e){
            return new DateMappingExceptionResponse().toResponse(e);
        }catch (JsonParseException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "METADATA_PARAM_ERROR", "unable to parse metadata").getResponse();
        }

        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);

        Set<URI> dateVariables = new VariableDAO(sparql, nosql, fs, user).getAllDateVariables();

        //Define query and it's conversion method here
        var query = new MongoSearchQuery<DataModel, DataSearchFilter, DataGetSearchDTO>()
                .setFilter(filter)
                .setConvertFunction(model -> DataGetSearchDTO.getDtoFromModel(model, dateVariables))
                .setPaginationStrategy(paginationStrategy);

        // Paginated search : direct convert from model -> dto, no count of data
        ListWithPagination<DataGetSearchDTO> results = dataLogic.searchWithPagination(query);

        return new PaginatedListResponse<>(results).getResponse();
    }

    private DataSearchFilter getSearchFilter(String startDate,
                                             String endDate,
                                             String timezone,
                                             List<URI> experiments,
                                             List<URI> targets,
                                             List<URI> variables,
                                             List<URI> devices,
                                             Float confidenceMin,
                                             Float confidenceMax,
                                             List<URI> provenances,
                                             String metadata,
                                             List<URI> operators,
                                             URI germplasmGroup,
                                             List<URI> germplasmUris,
                                             List<OrderBy> orderByList,
                                             int page,
                                             int pageSize) throws Exception {

        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
        }
        if (endDate != null) {
            endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
        }
        Document metadataFilter = null;
        if (metadata != null) {
            metadataFilter = Document.parse(metadata);
        }

        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setVariables(variables);
        filter.setProvenances(provenances);
        filter.setDevices(devices);
        filter.setStartDate(startInstant);
        filter.setEndDate(endInstant);
        filter.setConfidenceMin(confidenceMin);
        filter.setConfidenceMax(confidenceMax);
        filter.setMetadata(metadataFilter);
        filter.setOperators(operators);

        filter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList);

        Collection<URI> finalTargets = targets;

        //Get scientific objects associated to germplasms inside germplasmGroup if it's not null
        //Or/And scientific objects associated with passed germplasms
        if (germplasmGroup != null || !CollectionUtils.isEmpty(germplasmUris)) {
            finalTargets = targetByGermplasmFilter(targets, germplasmGroup, germplasmUris, experiments);

            //if targets is still empty when a group was passed then we don't want any data to be returned
            if(finalTargets.isEmpty()){
               return null;
            }
        }
        filter.setTargets(finalTargets);
        return filter;
    }



    @POST
    @Path("/count")
    @ApiOperation("Count data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return the number of data ", response = Long.class)
    })
    public Response countData(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE_MAX) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Search by operators", example = DATA_EXAMPLE_OPERATOR ) @QueryParam("operators") List<URI> operators,
            @ApiParam(value = "Group filter") @QueryParam("group_of_germplasm") @ValidURI URI germplasmGroup,
            @ApiParam(value = "Germplasm uris, can be an empty array but can't be null", name = "germplasmUris") @QueryParam("germplasmUris") List<URI> germplasmUris,
            @ApiParam(value = "Count limit. Specify the maximum number of data to count. Set to 0 for no limit", example = "10000") @QueryParam("count_limit") @DefaultValue("1000") @Min(0) int countLimit,
            @ApiParam(value = "Targets uris, can be an empty array but can't be null", name = "targets") List<URI> targets
    ) throws Exception {

        DataSearchFilter filter;

        try{
            filter = getSearchFilter(startDate, endDate, timezone, experiments, targets, variables, devices, confidenceMin, confidenceMax, provenances, metadata, operators, germplasmGroup, germplasmUris, null, 0, 0);
            if(filter == null){
                return new SingleObjectResponse<>(0).getResponse();
            }
        }catch (DateValidationException e){
            return new DateMappingExceptionResponse().toResponse(e);
        }catch (JsonParseException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "METADATA_PARAM_ERROR", "unable to parse metadata").getResponse();
        }

        CountOptions countOptions = new CountOptions().limit(countLimit);
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        long count = dataLogic.countData(filter, countOptions);
        return new SingleObjectResponse<>(count).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete data")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data deleted", response = URI.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteData(
            @ApiParam(value = "Data URI", example = DATA_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        try {
            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
            dataLogic.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }
    }

    @PUT
    @Path("{uri}/confidence")
    @ApiProtected
    @ApiOperation("Update confidence index")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Confidence update", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response updateConfidence(
            @ApiParam("Data description") @Valid DataConfidenceDTO dto,
            @ApiParam(value = "Data URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        try {
            DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
            dataLogic.updateConfidence(uri, dto.getConfidence());
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", uri);
        }

    }

    @PUT
    @ApiProtected
    @ApiOperation("Update data")
    @ApiCredential(credentialId = CREDENTIAL_DATA_MODIFICATION_ID, credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Confidence update", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response update(
            @ApiParam("Data description") @Valid DataUpdateDTO dto
    ) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        try {
            DataModel model = dto.newModel();
            dataLogic.update(model);
            return new ObjectUriResponse(model.getUri()).getResponse();

        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", dto.getUri());
        } catch (MongoBulkWriteException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getMessage())
                    .getResponse();
        } catch (MongoCommandException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DUPLICATE_DATA_KEY", e.getErrorMessage())
                    .getResponse();
        } catch (DateValidationException e) {
            return new DateMappingExceptionResponse().toResponse(e);
        }
    }

    @DELETE
    @ApiOperation("Delete data on criteria")
    @ApiProtected
    @ApiCredential(credentialId = CREDENTIAL_DATA_DELETE_ID, credentialLabelKey = CREDENTIAL_DATA_DELETE_LABEL_KEY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Data deleted", response = URI.class),
            @ApiResponse(code = 400, message = "Invalid or unknown Data URI", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteDataOnSearch(
            @ApiParam(value = "Search by experiment uri", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment") URI experimentUri,
            @ApiParam(value = "Search by target uri", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("target") URI objectUri,
            @ApiParam(value = "Search by variable uri", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variable") URI variableUri,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") URI provenanceUri
    ) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        if (experimentUri != null) {
            filter.setExperiments(Collections.singletonList(experimentUri));
        }
        if (objectUri != null) {
            filter.setTargets(Collections.singletonList(objectUri));
        }
        if (provenanceUri != null) {
            filter.setProvenances(Collections.singletonList(provenanceUri));
        }
        if (variableUri != null) {
            filter.setVariables(Collections.singletonList(variableUri));
        }

        DeleteResult result = dataLogic.deleteManyByFilter(filter);
        return new SingleObjectResponse<>(result).getResponse();
    }

    /**
     * @param startDate     startDate
     * @param endDate       endDate
     * @param timezone      timezone
     * @param experiments   experimentUris
     * @param objects       objectUris
     * @param variables     variableUris
     * @param confidenceMin confidenceMin
     * @param confidenceMax confidenceMax
     * @param provenances   provenanceUris
     * @param metadata      metadata json filter
     * @param csvFormat     long or wide format
     * @param orderByList   orderByList
     * @param page          page number
     * @param pageSize      page size
     * @return
     * @throws Exception
     */
    @Deprecated
    @GET
    @Path("export")
    @ApiOperation("Export data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return a csv file with data list results in wide or long format"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportData(
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenances", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Search by operators", example = DATA_EXAMPLE_OPERATOR ) @QueryParam("operators") List<URI> operators,
            @ApiParam(value = "Format wide or long", example = "wide") @DefaultValue("wide") @QueryParam("mode") String csvFormat,
            @ApiParam(value = "Export also raw_data") @DefaultValue("false") @QueryParam("with_raw_data") boolean withRawData,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        try{
            return prepareCSVExportResponse(
                    startDate,
                    endDate,
                    timezone,
                    experiments,
                    objects,
                    variables,
                    devices,
                    confidenceMin,
                    confidenceMax,
                    provenances,
                    metadata,
                    operators,
                    csvFormat,
                    withRawData,
                    orderByList,
                    page,
                    pageSize
            );
        }catch(Exception e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "EXPORT_ERROR", e.getMessage()).getResponse();
        }
    }

    @POST
    @Path("export")
    @ApiOperation("Export data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return a csv file with data list results in wide or long format"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportData(
            @ApiParam("CSV export configuration") @Valid DataSearchDTO dto
    ) throws Exception {
        try{
            return prepareCSVExportResponse(
                    dto.getStartDate(),
                    dto.getEndDate(),
                    dto.getTimezone(),
                    dto.getExperiments(),
                    dto.getObjects(),
                    dto.getVariables(),
                    dto.getDevices(),
                    dto.getConfidenceMin(),
                    dto.getConfidenceMax(),
                    dto.getProvenances(),
                    dto.getMetadata(),
                    null,
                    dto.csvFormat,
                    dto.withRawData,
                    null,
                    0,
                    0
            );
        }catch(Exception e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "EXPORT_ERROR", e.getMessage()).getResponse();
        }
    }

    /**
     * Handles any common code of the export services (Logs, validation, error responses, etc...)
     * @return the export response or an error response
     */
    private Response prepareCSVExportResponse(
            String startDate,
            String endDate,
            String timezone,
            List<URI> experiments,
            List<URI> objects,
            List<URI> variables,
            List<URI> devices,
            Float confidenceMin,
            Float confidenceMax,
            List<URI> provenances,
            String metadata,
            List<URI> operators,
            String csvFormat,
            boolean withRawData,
            List<OrderBy> orderByList,
            int page,
            int pageSize
    ) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);

        DataSearchFilter filter;

        try{
            filter = getSearchFilter(startDate, endDate, timezone, experiments, objects, variables, devices, confidenceMin, confidenceMax, provenances, metadata, operators, null, null, orderByList, page, pageSize);
            if(filter == null){
                return new ErrorResponse(Response.Status.BAD_REQUEST, "EMPTY_SEARCH_FILTER", "The filter used to export was null").getResponse();
            }
        }catch (DateValidationException e){
            return new DateMappingExceptionResponse().toResponse(e);
        }catch (JsonParseException e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "METADATA_PARAM_ERROR", "unable to parse metadata").getResponse();
        }catch (Exception e){
            return new ErrorResponse(Response.Status.BAD_REQUEST, "SEARCH_FILTER_ERROR", e.getMessage()).getResponse();
        }

        Instant start = Instant.now();
        DataExportInformation dataExportInformation = dataLogic.getDataExportInformation(!csvFormat.equals("long"), filter, LOGGER);

        Response prepareCSVExport = null;

        if (csvFormat.equals("long")) {
            prepareCSVExport = prepareCSVLongExportResponse(((DataLongExportInformation)dataExportInformation), withRawData);
        } else {
            prepareCSVExport = prepareCSVWideExportResponse(((DataWideExportInformation)dataExportInformation), withRawData);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Export data " + Long.toString(timeElapsed) + " milliseconds elapsed");

        return prepareCSVExport;
    }

    public Response prepareCSVWideExportResponse(DataWideExportInformation dataExportInformation, boolean withRawData) throws Exception {

        Instant startWriteCSVTime = Instant.now();

        List<String> defaultColumns = new ArrayList<>();

        // first static columns

        defaultColumns.add("Experiment");
        defaultColumns.add("Target");
        defaultColumns.add("Date");

        List<String> methods = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            methods.add("");
        }
        methods.add("Method");
        List<String> units = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            units.add("");
        }
        units.add("Unit");
        List<String> variablesList = new ArrayList<>();
        for (int i = 0; i < (defaultColumns.size() - 1); i++) {
            variablesList.add("");
        }
        variablesList.add("Variable");

        List<VariableModel> variablesModelList = new ArrayList<>(dataExportInformation.getVariables().values());

        Map<URI, Integer> variableUriIndex = new HashMap<>();
        for (VariableModel variableModel : variablesModelList) {

            MethodModel method = variableModel.getMethod();

            if (method != null) {
                String methodID = method.getName() + " (" + method.getUri().toString() + ")";
                methods.add(methodID);
            } else {
                methods.add("");
            }
            UnitModel unit = variableModel.getUnit();

            String unitID = unit.getName() + " (" + unit.getUri().toString() + ")";
            units.add(unitID);
            variablesList.add(variableModel.getName() + " (" + variableModel.getUri() + ")");
            defaultColumns.add(variableModel.getName());
            if (withRawData) {
                defaultColumns.add("Raw data");
                methods.add("");
                units.add("");
                variablesList.add("");
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 2));
            } else {
                variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 1));
            }
        }
        // static supplementary columns
        defaultColumns.add("Provenance");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Target URI");
        defaultColumns.add("Provenance URI");

        // ObjectURI, ObjectName, Factor, Date, Confidence, Variable n ...
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            // Method
            // Unit
            // Variable
            writer.writeNext(methods.toArray(new String[methods.size()]));
            writer.writeNext(units.toArray(new String[units.size()]));
            writer.writeNext(variablesList.toArray(new String[variablesList.size()]));
            // empty line
            writer.writeNext(new String[defaultColumns.size()]);
            // headers
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));

            // Search in map indexed by date for exp, prov, object and data
            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataExportDTO>>> instantProvUriDataEntry : dataExportInformation.getDataByIndexAndInstant().entrySet()) {
                Map<ExportDataIndex, List<DataExportDTO>> mapProvUriData = instantProvUriDataEntry.getValue();
                // Search in map indexed by  prov and data
                for (Map.Entry<ExportDataIndex, List<DataExportDTO>> provUriObjectEntry : mapProvUriData.entrySet()) {
                    List<DataExportDTO> val = provUriObjectEntry.getValue();

                    ArrayList<String> csvRow = new ArrayList<>();
                    //first is used to have value with the same dates on the same line
                    boolean first = true;

                    for (DataExportDTO dataGetDTO : val) {

                        if (first) {

                            csvRow = new ArrayList<>();

                            // experiment
                            ExperimentModel experiment = null;
                            if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                                experiment = dataExportInformation.getExperiments().get(dataGetDTO.getExperiment());
                            }

                            if (experiment != null) {
                                csvRow.add(experiment.getName());
                            } else {
                                csvRow.add("");
                            }

                            // target
                            SPARQLNamedResourceModel target = null;
                            if(dataGetDTO.getTarget() != null){
                                target = dataExportInformation.getObjects().get(dataGetDTO.getTarget());
                            }

                            if(target != null){
                                csvRow.add(target.getName());
                            }else{
                                csvRow.add("");
                            }

                            // date
                            csvRow.add(dataGetDTO.getDate());

                            // write blank columns for value and rawData
                            for (int i = 0; i < variablesModelList.size(); i++) {
                                csvRow.add("");
                                if (withRawData) {
                                    csvRow.add("");
                                }
                            }

                            // provenance
                            if (dataExportInformation.getProvenances().containsKey(dataGetDTO.getProvenance().getUri())) {
                                csvRow.add(dataExportInformation.getProvenances().get(dataGetDTO.getProvenance().getUri()).getName());
                            } else {
                                csvRow.add("");
                            }

                            // experiment URI
                            if (experiment != null) {
                                csvRow.add(experiment.getUri().toString());
                            } else {
                                csvRow.add("");
                            }

                            // target URI
                            if(target != null){
                                csvRow.add(target.getUri().toString());
                            }else{
                                csvRow.add("");
                            }

                            // provenance Uri
                            csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                            first = false;
                        }
                        // compare only expended URIs
                        URI dataGetDTOVariable = URI.create(SPARQLDeserializers.getExpandedURI(dataGetDTO.getVariable()));

                        // value
                        if (dataGetDTO.getValue() == null){
                            csvRow.set(variableUriIndex.get(dataGetDTOVariable), null);
                        } else {
                            csvRow.set(variableUriIndex.get(dataGetDTOVariable), dataGetDTO.getValue().toString());
                        }

                        // raw data
                        if (withRawData) {
                            if (dataGetDTO.getRawData() == null){
                                csvRow.set(variableUriIndex.get(dataGetDTOVariable)+1, null);
                            } else {
                                csvRow.set(variableUriIndex.get(dataGetDTOVariable)+1, Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            }
                        }

                    }


                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);

                }
            }

            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_wide_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(startWriteCSVTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName )
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Response prepareCSVLongExportResponse(DataLongExportInformation dataExportInformation, boolean withRawData) throws Exception {
        Instant startWriteCSVTime = Instant.now();

        List<String> defaultColumns = new ArrayList<>();

        defaultColumns.add("Experiment");
        defaultColumns.add("Target");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        if (withRawData) {
            defaultColumns.add("Raw data");
        }
        defaultColumns.add("Data Description");
        defaultColumns.add("");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Target URI");
        defaultColumns.add("Variable URI");
        defaultColumns.add("Data Description URI");

        // See defaultColumns order
        //        Target
        //        Date
        //        Variable
        //        Method
        //        Unit
        //        Value
        //        Data Description
        //
        //        Target URI
        //        Variable URI
        //        Data Description URI
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));
            for (Map.Entry<Instant, List<DataGetDTO>> entry : dataExportInformation.getDataByInstant().entrySet()) {
                List<DataGetDTO> val = entry.getValue();
                for (DataGetDTO dataGetDTO : val) {

                    //1 row per experiment
                    int maxRows = 1;
                    if (dataGetDTO.getProvenance().getExperiments() != null && dataGetDTO.getProvenance().getExperiments().size()>1) {
                        maxRows = dataGetDTO.getProvenance().getExperiments().size();
                    }

                    for (int j=0; j<maxRows; j++) {
                        ArrayList<String> csvRow = new ArrayList<>();
                        // experiment
                        ExperimentModel experiment = null;
                        if (dataGetDTO.getProvenance().getExperiments() != null && !dataGetDTO.getProvenance().getExperiments().isEmpty()) {
                            experiment = dataExportInformation.getExperiments().get(dataGetDTO.getProvenance().getExperiments().get(j));
                        }

                        if (experiment != null) {
                            csvRow.add(experiment.getName());
                        } else {
                            csvRow.add("");
                        }

                        SPARQLNamedResourceModel target = null;
                        if(dataGetDTO.getTarget() != null){
                            target = dataExportInformation.getObjects().get(dataGetDTO.getTarget());
                        }
                        // target name
                        if(target != null){
                            csvRow.add(target.getName());
                        }else{
                            csvRow.add("");
                        }

                        // date
                        csvRow.add(dataGetDTO.getDate());
                        // variable
                        csvRow.add(dataExportInformation.getVariables().get(dataGetDTO.getVariable()).getName());
                        // method
                        if (dataExportInformation.getVariables().get(dataGetDTO.getVariable()).getMethod() != null) {
                            csvRow.add(dataExportInformation.getVariables().get(dataGetDTO.getVariable()).getMethod().getName());
                        } else {
                            csvRow.add("");
                        }
                        // unit
                        csvRow.add(dataExportInformation.getVariables().get(dataGetDTO.getVariable()).getUnit().getName());
                        // value
                        if(dataGetDTO.getValue() == null){
                            csvRow.add(null);
                        }else{
                            csvRow.add(dataGetDTO.getValue().toString());
                        }

                        // rawData
                        if (withRawData) {
                            if(dataGetDTO.getRawData() == null){
                                csvRow.add(null);
                            }else{
                                csvRow.add(Arrays.toString(dataGetDTO.getRawData().toArray()).replace("[", "").replace("]", ""));
                            }
                        }

                        // provenance
                        if (dataExportInformation.getProvenances().containsKey(dataGetDTO.getProvenance().getUri())) {
                            csvRow.add(dataExportInformation.getProvenances().get(dataGetDTO.getProvenance().getUri()).getName());
                        } else {
                            csvRow.add("");
                        }
                        csvRow.add("");

                        // experiment URI
                        if (experiment != null) {
                            csvRow.add(experiment.getUri().toString());
                        } else {
                            csvRow.add("");
                        }

                        // target uri
                        if (target != null) {
                            csvRow.add(target.getUri().toString());
                        } else {
                            csvRow.add("");
                        }

                        // variable uri
                        csvRow.add(dataGetDTO.getVariable().toString());
                        // provenance Uri
                        csvRow.add(dataGetDTO.getProvenance().getUri().toString());

                        String[] row = csvRow.toArray(new String[csvRow.size()]);
                        writer.writeNext(row);
                    }
                }
            }
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_long_format" + dtf.format(date) + ".csv";

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(startWriteCSVTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName)
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    @GET
    @Path("provenances")
    @ApiOperation("Search provenances linked to data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getUsedProvenances(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices
    ) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        List<ProvenanceModel> usedProvenances = dataLogic.searchUsedProvenances(experiments, objects, variables, devices);
        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>();
        usedProvenances.forEach(result -> {
            resultDTOList.add(ProvenanceGetDTO.fromModel(result));
        });
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @POST
    @Path("provenances/by_targets")
    @ApiOperation("Search provenances linked to data for a large list of targets")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response getUsedProvenancesByTargets(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by variables uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by devices uris", example = DeviceAPI.DEVICE_EXAMPLE_URI) @QueryParam("devices") List<URI> devices,
            @ApiParam(value = "Targets uris") List<URI> targets
    ) throws Exception {
        if (targets == null) {
            targets = new ArrayList<>();
        }
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        List<ProvenanceModel> usedProvenances = dataLogic.searchUsedProvenances(experiments, targets, variables, devices);
        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>();
        usedProvenances.forEach(result -> {
            resultDTOList.add(ProvenanceGetDTO.fromModel(result));
        });
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    
    @GET
    @Path("variables")
    @ApiOperation("Get variables linked to data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables list", response = VariableGetDTO.class, responseContainer = "List")
    })
    public Response getUsedVariables(
            @ApiParam(value = "Search by experiment uris", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiments") List<URI> experiments,
            @ApiParam(value = "Search by targets uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("targets") List<URI> objects,
            @ApiParam(value = "Search by provenance uris", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by device uris") @QueryParam("devices") List<URI> devices
    ) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);
        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setTargets(objects);
        filter.setProvenances(provenances);
        filter.setDevices(devices);

        List<VariableGetDTO> dtoList = dataLogic.getUsedVariablesByFilter(filter).stream()
                .map(e -> VariableGetDTO.fromModel(e, null))
                .collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @POST
    @Path("import")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data are imported", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
        credentialId = CREDENTIAL_DATA_MODIFICATION_ID, 
        credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importCSVData(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment")  @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);

        DataCSVValidationModel validation = dataLogic.validateWholeCSV(true, provenance, experiment, file, LOGGER);

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            List<DataModel> data = new ArrayList<>(validation.getData().keySet());
            try {
                dataLogic.createManyFromImport(data, validation);

            } catch (NoSQLTooLargeSetException ex) {
                validation.setTooLargeDataset(true);
            } catch (MongoBulkWriteException duplicateError) {
                List<BulkWriteError> bulkErrors = duplicateError.getWriteErrors();
                for (int i = 0; i < bulkErrors.size(); i++) {
                    int index = bulkErrors.get(i).getIndex();
                    DataModel dataModel = data.get(index);
                    int variableIndex = validation.getHeaders().indexOf(dataModel.getVariable().toString());
                    String variableName = validation.getHeadersLabels().get(variableIndex) + '(' + validation.getHeaders().get(variableIndex) + ')';
                    CSVCell csvCell = new CSVCell(validation.getData().get(data.get(index)), validation.getHeaders().indexOf(dataModel.getVariable().toString()), dataModel.getValue().toString(), variableName);
                    validation.addDuplicatedDataError(csvCell);
                }
            } catch (MongoCommandException e) {
                CSVCell csvCell = new CSVCell(-1, -1, "Unknown value", "Unknown variable");
                validation.addDuplicatedDataError(csvCell);
            } catch (DataTypeException e) {
                int indexOfVariable = validation.getHeaders().indexOf(e.getVariable().toString());
                String variableName = validation.getHeadersLabels().get(indexOfVariable) + '(' + validation.getHeaders().get(indexOfVariable) + ')';
                validation.addInvalidDataTypeError(new CSVCell(e.getDataIndex(), indexOfVariable, e.getValue().toString(), variableName));
            }
            Instant finish = Instant.now();
            long timeElapsed = Duration.between(start, finish).toMillis();
            LOGGER.debug("Insertion " + Long.toString(timeElapsed) + " milliseconds elapsed");
            
            validation.setValidCSV(!validation.hasErrors());
        }
        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();
        csvValidation.setDataErrors(validation);
        return new SingleObjectResponse<>(csvValidation).getResponse();
    }
    
    @POST
    @Path("import_validation")
    @ApiOperation(value = "Import a CSV file for the given provenanceURI.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data are validated", response = DataCSVValidationDTO.class)})
    @ApiProtected
    @ApiCredential(
            groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
            groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateCSV(
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = ExperimentAPI.EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI) @QueryParam("experiment")  @ValidURI URI experiment,
            @ApiParam(value = "File", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        DataLogic dataLogic = new DataLogic(sparql, nosql, fs, user);

        DataCSVValidationModel validation = dataLogic.validateWholeCSV(false, provenance, experiment, file, LOGGER);

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();
        csvValidation.setDataErrors(validation);

        return new SingleObjectResponse<>(csvValidation).getResponse();
    }

    @GET
    @Path("/data_serie/facility")
    @ApiOperation("Get all data series associated with a facility")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return a list of data serie", response = DataVariableSeriesGetDTO.class)
    })
    public Response getDataSeriesByFacility(
            @ApiParam(value = "variable URI", example = "http://example.com/", required = true) @QueryParam("variable") @NotNull URI variableUri,
            @ApiParam(value = "target URI", example = "http://example.com/", required = true) @QueryParam("target") @NotNull URI facilityUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Retreive calculated series only", example = "false") @QueryParam("calculated_only") Boolean calculatedOnly
    ) throws Exception {

        return new SingleObjectResponse<>(
                new DataLogic(sparql, nosql, fs, user).getDataSeriesByFacility(
                        variableUri,
                        facilityUri,
                        startDate,
                        endDate,
                        calculatedOnly,
                        LOGGER)
        ).getResponse();
    }

}
