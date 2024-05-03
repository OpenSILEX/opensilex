//******************************************************************************
//                          DataAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.result.DeleteResult;
import com.opencsv.CSVWriter;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.swagger.annotations.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.annotation.dal.AnnotationDAO;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.core.data.dal.*;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.device.api.DeviceAPI;
import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.exception.*;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.experiment.utils.ExportDataIndex;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.api.VariableGetDTO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
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
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.data.utils.DataMathFunctions.*;


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
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
        List<DataModel> dataList = new ArrayList<>();
        try {
            if (dtoList.size() > SIZE_MAX) {
                throw new NoSQLTooLargeSetException(SIZE_MAX, dtoList.size());
            }

            for (DataCreationDTO dto : dtoList) {
                DataModel model = dto.newModel();
                model.setPublisher(user.getUri());
                dataList.add(model);
            }

            dataList = validData(dataList);

            dao.create(dataList);
            if(variablesToDevices.size() > 0) {

                DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
                for (Map.Entry variablesToDevice : variablesToDevices.entrySet() ){

                    deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );

                }
            }

            List<URI> createdResources = new ArrayList<>();
            for (DataModel data : dataList) {
                createdResources.add(data.getUri());
            }
            return new CreatedUriResponse(createdResources).getResponse();
        } catch (NoSQLTooLargeSetException ex) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "DATA_SIZE_LIMIT",
                    ex.getMessage()).getResponse();

        }catch (MongoDbUniqueIndexConstraintViolation duplicateError){
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
            @ApiParam(value = "Data URI", /*example = "platform-data:irrigation",*/ required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);

        try {
            DataModel model = dao.get(uri);
            DataGetDetailsDTO dto = DataGetDetailsDTO.getDtoFromModel(model, getAllDateVariables());

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

        Set<URI> dateVariables = getAllDateVariables();
        DataDaoV2 dataDaoV2 = new DataDaoV2(sparql, nosql, fs);

        // Paginated search : direct convert from model -> dto, no count of data
        ListWithPagination<DataGetSearchDTO> results = dataDaoV2.searchWithPagination(
                new MongoSearchQuery<DataModel, DataSearchFilter, DataGetSearchDTO>()
                        .setFilter(filter)
                        .setConvertFunction(model -> DataGetSearchDTO.getDtoFromModel(model, dateVariables))
                        .setPaginationStrategy(paginationStrategy)
        );

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
        long count = new DataDaoV2(sparql, nosql, fs).count(null, filter, countOptions);
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
            DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
            dao.delete(uri);
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
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
        try {
            DataModel data = dao.get(uri);
            data.setConfidence(dto.getConfidence());
            dao.update(data);
            return new ObjectUriResponse(Response.Status.OK, data.getUri()).getResponse();
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

        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
        try {
            DataModel model = dto.newModel();
            validData(Collections.singletonList(model));
            dao.update(model);
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
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(Collections.singletonList(experimentUri));
        filter.setTargets(Collections.singletonList(objectUri));
        filter.setProvenances(Collections.singletonList(provenanceUri));
        filter.setVariables(Collections.singletonList(variableUri));

        DeleteResult result = dao.deleteMany(filter);
        return new SingleObjectResponse<>(result).getResponse();
    }

    /** 
     * check that value is coherent with the variable datatype 
     *
     * @param variable
     * @param data
     * @throws Exception
     */
    private void setDataValidValue(VariableModel variable, DataModel data) throws Exception {
        if (data.getValue() != null) {
            URI variableUri = variable.getUri();
            URI dataType = variable.getDataType();
            Object value = data.getValue();
            DataValidateUtils.checkAndConvertValue(data, variableUri, value, dataType);
        }
    }
    
    
    /** 
     * First check in the data provenance if there is a device 
     * Then check in the provenance and fill the data provenance with the device
     * If no device and no target return an exception
     *
     * @param provDAO
     * @param data
     * @param hasTarget 
     * @param variableCheckedDevice 
     * @param provenanceToDevice 

     * @throws Exception
     */
    private void variablesDeviceAssociation(ProvenanceDaoV2 provDAO, DataModel data, boolean hasTarget, Map<DeviceModel, URI> variableCheckedDevice, Map<URI, DeviceModel> provenanceToDevice) throws Exception{
        
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        URI provenanceURI = data.getProvenance().getUri();
        DeviceModel deviceFromProvWasAssociated = checkAndReturnDeviceFromDataProvenance(data, deviceDAO); 
        if (deviceFromProvWasAssociated == null) {
            
            DeviceModel device = null;
            if(provenanceToDevice.containsKey(provenanceURI)) {
               device = provenanceToDevice.get(provenanceURI);
               //check
            } else {
                device = checkAndReturnDeviceFromProvenance(deviceDAO, provDAO, data);
                provenanceToDevice.put(provenanceURI,device);
            }
          
            if (device != null) {
                
                if (!variableIsAssociatedToDevice(device, data.getVariable())) {
                    addVariableToDevice(device,data.getVariable()); // add variable/device
                }
                
                if (rootDeviceTypes == null) {
                    rootDeviceTypes = getRootDeviceTypes();
                }

                DataProvenanceModel provMod = data.getProvenance();
                List<ProvEntityModel> agents = null;
                if (provMod.getProvWasAssociatedWith() == null) {
                    agents = new ArrayList<>();
                } else {
                    agents = provMod.getProvWasAssociatedWith();
                }

                ProvEntityModel agent = new ProvEntityModel();
                agent.setUri(device.getUri());
                URI rootType = rootDeviceTypes.get(device.getType()); 
                agent.setType(rootType);
                agents.add(agent);
                provMod.setProvWasAssociatedWith(agents);
                data.setProvenance(provMod);
            } else {
                if(!hasTarget) {
                   throw new DeviceOrTargetToDataException(data);
                }
            } 
           
        } else {
            boolean deviceIsChecked = variableCheckedDevice.containsKey(deviceFromProvWasAssociated) && variableCheckedDevice.get(deviceFromProvWasAssociated) == data.getVariable() ;
            if(!deviceIsChecked){
                if (!variableIsAssociatedToDevice(deviceFromProvWasAssociated, data.getVariable())) {
                    addVariableToDevice(deviceFromProvWasAssociated,data.getVariable()); // add variable/device
                }
                variableCheckedDevice.put(deviceFromProvWasAssociated,data.getVariable());
                
            }
        }
    }
    
    /** 
     * check and return Device from Provenance if no ambiguity
     *
     * @param deviceDAO
     * @param provDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromProvenance(DeviceDAO deviceDAO, ProvenanceDaoV2 provDAO, DataModel data) throws Exception {

       ProvenanceModel provenance = provDAO.get(data.getProvenance().getUri());

       DeviceModel deviceToReturn = null ;
       List<DeviceModel> devices = new ArrayList<>();
       List<DeviceModel> linkedDevices = new ArrayList<>();
                               
       if (provenance.getAgents() != null && !provenance.getAgents().isEmpty()) {
           
            for (AgentModel agent : provenance.getAgents()) {
                if(agent.getRdfType() == null) {
                     throw new ProvenanceAgentTypeException(agent.getUri().toString());
                }
                if (deviceDAO.isDeviceType(agent.getRdfType())) {
                    DeviceModel device = deviceDAO.getDeviceByURI(agent.getUri(), user);
                    if (device != null) {
                        devices.add(device);
                       
                        if(variableIsAssociatedToDevice(device, data.getVariable())){
                            linkedDevices.add(device);
                        }
                    }
                }
            }
           
            switch (linkedDevices.size()) {
                case 0:
                    if (devices.size() > 1) {
                        throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());
                    } else {
                        if (!devices.isEmpty()) {
                            deviceToReturn = devices.get(0);
                        }
                    }
                    break;
                case 1:
                    deviceToReturn = linkedDevices.get(0);
                    break;
                default :
                    throw new DeviceProvenanceAmbiguityException(provenance.getUri().toString());

            }    
            
        }
        return deviceToReturn;
    }

    /** 
     * check and return Device from Data Provenance if no ambiguity
     * Exception if two devices as provenance agent
     *
     * @param deviceDAO
     * @param data
     * @throws Exception
     */
    private DeviceModel checkAndReturnDeviceFromDataProvenance(DataModel data, DeviceDAO deviceDAO) throws Exception{

       boolean deviceIsLinked = false; // to test if there are 2 devices
       URI agentToReturn = null;
       DeviceModel device = null;
       if(data.getProvenance().getProvWasAssociatedWith()!= null && !data.getProvenance().getProvWasAssociatedWith().isEmpty()){
            for (ProvEntityModel agent : data.getProvenance().getProvWasAssociatedWith()) {
                
                    if(agent.getType() == null) {
                        throw new ProvenanceAgentTypeException(agent.getUri().toString());
                    }
                    
                    if (deviceDAO.isDeviceType(agent.getType())) {
                        if(!deviceIsLinked) {
                            deviceIsLinked = true;
                            agentToReturn = agent.getUri();

                        } else {
                            throw new DeviceProvenanceAmbiguityException(data.getProvenance().getUri().toString());
                        }
                    }
            }
            if(agentToReturn != null){
                device = deviceDAO.getDeviceByURI(agentToReturn, user);
            }
        }
        return device;
    }
    
    /** 
     * check if variable is associated to device
     * @param device
     * @param variable
     * @throws Exception
     */
    private boolean variableIsAssociatedToDevice(DeviceModel device, URI variable){
        List<SPARQLModelRelation> variables = device.getRelations(Oeso.measures).collect(Collectors.toList());

        if (!variables.isEmpty()) {
            if (variables.stream().anyMatch(var -> (SPARQLDeserializers.compareURIs(var.getValue(), variable.toString())))) {
                return true;
            }

        }
        return false;
        
    }

    public void addVariableToDevice(DeviceModel device, URI variable) {
        
        if (!variablesToDevices.containsKey(device)) {
            List<URI> list = new ArrayList<>();
            list.add(variable);
            variablesToDevices.put(device, list);
        } else {
            if (!variablesToDevices.get(device).contains(variable)) {
                variablesToDevices.get(device).add(variable);
            }
        }
    }

    
    /**
     * Check variable data list before creation
     * Complete the prov_was_associated_with provenance attribut
     *
     * @param dataList
     * @throws Exception
     */
    private List<DataModel> validData(List<DataModel> dataList) throws Exception {

        VariableDAO variableDAO = new VariableDAO(sparql,nosql,fs);

        Map<URI, VariableModel> variableURIs = new HashMap<>();
        Set<URI> notFoundedVariableURIs = new HashSet<>();
        Set<URI> targetURIs = new HashSet<>();
        Set<URI> notFoundedTargetURIs = new HashSet<>();
        Set<URI> provenanceURIs = new HashSet<>();
        Set<URI> notFoundedProvenanceURIs = new HashSet<>();
        Set<URI> expURIs = new HashSet<>();
        Set<URI> notFoundedExpURIs = new HashSet<>();
        Map<DeviceModel, URI> variableCheckedDevice =  new HashMap<>();
        Map<URI, DeviceModel> provenanceToDevice =  new HashMap<>();
        
        List<DataModel> validData = new ArrayList<>();
        int dataIndex = 0;
        for (DataModel data : dataList) {
            
            boolean hasTarget = false;
            // check variable uri and datatype
            if (data.getVariable() != null) {  // and if null ?
                VariableModel variable = null;
                URI variableURI = data.getVariable();
                if (!variableURIs.containsKey(variableURI)) {
                    variable = variableDAO.get(variableURI);  
                    if (variable == null) {
                        notFoundedVariableURIs.add(variableURI);
                    } else {
                        if (variable.getDataType() == null) {
                            throw new NoVariableDataTypeException(variableURI);
                        }
                        variableURIs.put(variableURI, variable);
                    }
                } else {
                    variable = variableURIs.get(variableURI);
                    
                }
                if(!notFoundedVariableURIs.contains(variableURI)) {
                    setDataValidValue(variable, data);
                    dataIndex++;
                }
                
              
            }
            

            //check targets uri
            if (data.getTarget() != null) {
                hasTarget = true ;
                if (!targetURIs.contains(data.getTarget())) {
                    targetURIs.add(data.getTarget());
                    if (!sparql.uriExists((Node) null, data.getTarget())) {
                        hasTarget = false ;
                        notFoundedTargetURIs.add(data.getTarget()); 
                    }
                }
            }

            //check provenance uri and variables device association
            ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
            if (!provenanceURIs.contains(data.getProvenance().getUri())) {
                provenanceURIs.add(data.getProvenance().getUri());
                if (!provDAO.exists(data.getProvenance().getUri())) {
                    notFoundedProvenanceURIs.add(data.getProvenance().getUri());
                } 
            }
            
            if(!notFoundedProvenanceURIs.contains(data.getProvenance().getUri())){
                variablesDeviceAssociation(provDAO, data, hasTarget, variableCheckedDevice, provenanceToDevice);
            }

            // check experiments uri
            if (data.getProvenance().getExperiments() != null) {
                for (URI exp : data.getProvenance().getExperiments()) {
                    if (!expURIs.contains(exp)) {
                        expURIs.add(exp);
                        if (!sparql.uriExists(ExperimentModel.class, exp)) {
                            notFoundedExpURIs.add(exp);
                        }
                    }
                }
            }
            
            validData.add(data);

        }

        if (!notFoundedVariableURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong variable uris: ", new ArrayList<>(notFoundedVariableURIs));// NOSQL Exception ? come from sparql request
        }
        if (!notFoundedTargetURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong target uris", new ArrayList<>(notFoundedTargetURIs)); // NOSQL Exception ? come from sparql request
        }
        if (!notFoundedProvenanceURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong provenance uris: ", new ArrayList<>(notFoundedProvenanceURIs)); 
        }
        if (!notFoundedExpURIs.isEmpty()) {
            throw new NoSQLInvalidUriListException("wrong experiments uris: ", new ArrayList<>(notFoundedExpURIs)); // NOSQL Exception ? come from sparql request
        }
        
        return validData;

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
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);

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
        List<DataModel> resultList = new ArrayList<>();
        dao.searchAsStreamWithPagination(filter).forEach(resultList::add);

        Instant data = Instant.now();
        LOGGER.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Response prepareCSVExport = null;

        if (csvFormat.equals("long")) {
            prepareCSVExport = prepareCSVLongExportResponse(resultList, user, withRawData);
        } else {
            prepareCSVExport = prepareCSVWideExportResponse(resultList, user, withRawData);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Export data " + Long.toString(timeElapsed) + " milliseconds elapsed");

        return prepareCSVExport;
    }

    public Response prepareCSVWideExportResponse(List<DataModel> resultList, AccountModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        Set<URI> dateVariables = getAllDateVariables();

        List<URI> variables = new ArrayList<>();

        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();

        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }

            if (!variables.contains(dataModel.getVariable())) {
                variables.add(dataModel.getVariable());
            }

            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }

            if (!dataByIndexAndInstant.containsKey(dataModel.getDate())) {
                dataByIndexAndInstant.put(dataModel.getDate(), new HashMap<>());
            }

            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }

                    ExportDataIndex exportDataIndex = new ExportDataIndex(
                            exp,
                            dataModel.getProvenance().getUri(),
                            dataModel.getTarget()
                    );

                    if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                        dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                    }
                    dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, exp, dateVariables));
                }
            } else {
                ExportDataIndex exportDataIndex = new ExportDataIndex(
                        null,
                        dataModel.getProvenance().getUri(),
                        dataModel.getTarget()
                );

                if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                    dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
                }
                dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataExportDTO.fromModel(dataModel, null, dateVariables));

            }

        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

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

        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(variables);

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

        Instant variableTime = Instant.now();
        LOGGER.debug("Get " + variables.size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);

        // Provides the experiment as context if there is only one
        URI context = null;
        if (experiments.size() == 1) {
            context = experiments.keySet().stream().findFirst().get();
        }

        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), context);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
        List<ProvenanceModel> listByURIs = provenanceDao.findByUris(provenances.keySet().parallelStream(), provenances.size());
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

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
            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataExportDTO>>> instantProvUriDataEntry : dataByIndexAndInstant.entrySet()) {
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
                                experiment = experiments.get(dataGetDTO.getExperiment());
                            }

                            if (experiment != null) {
                                csvRow.add(experiment.getName());
                            } else {
                                csvRow.add("");
                            }

                            // target
                            SPARQLNamedResourceModel target = null;
                            if(dataGetDTO.getTarget() != null){
                                target = objects.get(dataGetDTO.getTarget());
                            }

                            if(target != null){
                                csvRow.add(target.getName());
                            }else{
                                csvRow.add("");
                            }

                            // date
                            csvRow.add(dataGetDTO.getDate());

                            // write blank columns for value and rawData
                            for (int i = 0; i < variables.size(); i++) {
                                csvRow.add("");
                                if (withRawData) {
                                    csvRow.add("");
                                }
                            }

                            // provenance
                            if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                                csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
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
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=" + fileName )
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    public Response prepareCSVLongExportResponse(List<DataModel> resultList, AccountModel user, boolean withRawData) throws Exception {
        Instant data = Instant.now();

        Set<URI> dateVariables = getAllDateVariables();

        Map<URI, VariableModel> variables = new HashMap<>();
        Map<URI, SPARQLNamedResourceModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<URI, ExperimentModel> experiments = new HashMap();

        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();
        for (DataModel dataModel : resultList) {
            if (dataModel.getTarget() != null && !objects.containsKey(dataModel.getTarget())) {
                objects.put(dataModel.getTarget(), null);
            }
            if (!variables.containsKey(dataModel.getVariable())) {
                variables.put(dataModel.getVariable(), null);
            }
            if (!provenances.containsKey(dataModel.getProvenance().getUri())) {
                provenances.put(dataModel.getProvenance().getUri(), null);
            }
            if (!dataByInstant.containsKey(dataModel.getDate())) {
                dataByInstant.put(dataModel.getDate(), new ArrayList<>());
            }
            dataByInstant.get(dataModel.getDate()).add(DataGetDTO.getDtoFromModel(dataModel, dateVariables));
            if (dataModel.getProvenance().getExperiments() != null) {
                for (URI exp:dataModel.getProvenance().getExperiments()) {
                    if (!experiments.containsKey(exp)) {
                        experiments.put(exp, null);
                    }
                }
            }
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

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

        Instant variableTime = Instant.now();
        List<VariableModel> variablesModelList = new VariableDAO(sparql,nosql,fs).getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(new URI(SPARQLDeserializers.getShortURI(variableModel.getUri())), variableModel);
        }
        LOGGER.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        List<SPARQLNamedResourceModel> objectsList = ontologyDao.getURILabels(objects.keySet(), user.getLanguage(), null);
        for (SPARQLNamedResourceModel obj : objectsList) {
            objects.put(obj.getUri(), obj);
        }
        Instant targetTime = Instant.now();
        LOGGER.debug("Get " + objectsList.size() + " target(s) " + Long.toString(Duration.between(variableTime, targetTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
        List<ProvenanceModel> listByURIs = provenanceDao.findByUris(provenances.keySet().parallelStream(), provenances.size());
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(targetTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        List<ExperimentModel> listExp = sparql.getListByURIs(ExperimentModel.class, new ArrayList<>(experiments.keySet()), user.getLanguage());
        for (ExperimentModel exp : listExp) {
            experiments.put(exp.getUri(), exp);
        }
        Instant expTime = Instant.now();
        LOGGER.debug("Get " + listExp.size() + " experiment(s) " + Long.toString(Duration.between(variableTime, expTime).toMillis()) + " milliseconds elapsed");

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
            for (Map.Entry<Instant, List<DataGetDTO>> entry : dataByInstant.entrySet()) {
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
                            experiment = experiments.get(dataGetDTO.getProvenance().getExperiments().get(j));
                        }

                        if (experiment != null) {
                            csvRow.add(experiment.getName());
                        } else {
                            csvRow.add("");
                        }

                        SPARQLNamedResourceModel target = null;
                        if(dataGetDTO.getTarget() != null){
                            target = objects.get(dataGetDTO.getTarget());
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
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getName());
                        // method
                        if (variables.get(dataGetDTO.getVariable()).getMethod() != null) {
                            csvRow.add(variables.get(dataGetDTO.getVariable()).getMethod().getName());
                        } else {
                            csvRow.add("");
                        }
                        // unit
                        csvRow.add(variables.get(dataGetDTO.getVariable()).getUnit().getName());
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
                        if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                            csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
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
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

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
        return searchUsedProvenances(experiments, objects, variables, devices);
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
        return searchUsedProvenances(experiments, targets, variables, devices);
    }

    private Response searchUsedProvenances(
            List<URI> experiments,
            List<URI> targets,
            List<URI> variables,
            List<URI> devices) throws Exception {

        DataDaoV2 dataDAO = new DataDaoV2(sparql, nosql, fs);
        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setTargets(targets);
        filter.setVariables(variables);
        filter.setDevices(devices);

        List<URI> provenanceURIs = dataDAO.distinct(null, "provenance.uri", URI.class, filter);
        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>();

        if(!provenanceURIs.isEmpty()){
            ProvenanceDaoV2 provenanceDao = new ProvenanceDaoV2(nosql.getServiceV2());
            List<ProvenanceModel> resultList = provenanceDao.findByUris(provenanceURIs.stream(), provenanceURIs.size());

            resultList.forEach(result -> {
                resultDTOList.add(ProvenanceGetDTO.fromModel(result));
            });
        }

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
        DataDaoV2 dataDAO = new DataDaoV2(sparql, nosql, fs);
        DataSearchFilter filter = new DataSearchFilter();
        filter.setUser(user);
        filter.setExperiments(experiments);
        filter.setTargets(objects);
        filter.setProvenances(provenances);
        filter.setDevices(devices);

        List<VariableGetDTO> dtoList = dataDAO.distinct(null, DataModel.VARIABLE_FIELD, VariableModel.class, filter)
                .stream()
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
        DataDaoV2 dao = new DataDaoV2(sparql, nosql, fs);
        AnnotationDAO annotationDAO = new AnnotationDAO(sparql);

        // test prov
        ProvenanceModel provenanceModel = null;
        ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }
        // test exp
        if(experiment != null) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
            xpDAO.validateExperimentAccess(experiment, user);
        }

        DataCSVValidationModel validation;
        
        validation = validateWholeCSV(provenanceModel, experiment, file, user);

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setInsertionStep(true);
        validation.setValidCSV(!validation.hasErrors()); 
        validation.setNbLinesToImport(validation.getData().size()); 

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            List<DataModel> data = new ArrayList<>(validation.getData().keySet());
            try {
                new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session->{
                    data.forEach(dataModel -> dataModel.setPublisher(user.getUri()));
                    dao.create(session, data);
                    if(!validation.getVariablesToDevices().isEmpty()){
                        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
                        for (Map.Entry variablesToDevice : validation.getVariablesToDevices().entrySet() ){
                            deviceDAO.associateVariablesToDevice((DeviceModel) variablesToDevice.getKey(),(List<URI>)variablesToDevice.getValue(), user );
                        }
                    }
                    validation.setNbLinesImported(data.size());
                    //If the data import was successful, post the annotations on objects
                    annotationDAO.create(validation.getAnnotationsOnObjects());
                    return 0;
                });

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

        // test prov
        ProvenanceModel provenanceModel = null;

        ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        }

        // test exp
        if(experiment != null) {
            ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
            xpDAO.validateExperimentAccess(experiment, user);
        }
        DataCSVValidationModel validation;

        Instant start = Instant.now();
        validation = validateWholeCSV(provenanceModel, experiment, file, user);
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Validation " + Long.toString(timeElapsed) + " milliseconds elapsed");

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setValidCSV(!validation.hasErrors());
        validation.setValidationStep(true);
        validation.setNbLinesToImport(validation.getData().size());
        csvValidation.setDataErrors(validation);

        return new SingleObjectResponse<>(csvValidation).getResponse();
    }
    
    private final String expHeader = "experiment";
    private final String targetHeader = "target";
    private final String dateHeader = "date";
    private final String deviceHeader = "device";
    private final String rawdataHeader = "raw_data";
    private final String soHeader = "scientific_object";
    private final String annotationHeader = "object_annotation";

    private DataCSVValidationModel validateWholeCSV(ProvenanceModel provenance, URI experiment, InputStream file, AccountModel currentUser) throws Exception {
        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
        List<String> notExistingTargets = new ArrayList<>();
        List<String> duplicatedTargets = new ArrayList<>();
        
        ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
        Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
        List<String> notExistingExperiments = new ArrayList<>();
        List<String> duplicatedExperiments = new ArrayList<>();


        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);
        Map<String, SPARQLNamedResourceModel> nameURIScientificObjectsInXp = new HashMap<>();
        List<String> scientificObjectsNotInXp = new ArrayList<>();

        
        DeviceDAO deviceDAO = new DeviceDAO(sparql, nosql, fs);
        Map<String,DeviceModel> nameURIDevices = new HashMap<>();
        List<String> notExistingDevices = new ArrayList<>();
        List<String> duplicatedDevices = new ArrayList<>();
        
        List<AgentModel> agents = provenance.getAgents();
        boolean sensingDeviceFoundFromProvenance = false;
        if (agents !=  null) {
            for (AgentModel agent:agents) {
                if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) { 
                    sensingDeviceFoundFromProvenance = true;
                    break;
                }
            }
        }
        
        Map<String,DeviceModel> variableCheckedProvDevice =  new HashMap<>();
        List<String> checkedVariables = new ArrayList<>();
        
        Map<String, DeviceModel> variableCheckedDevice = new HashMap<>();

        Map<Integer, String> headerByIndex = new HashMap<>();

        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>();

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import data - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            // Line 1
            String[] ids = csvReader.parseNext();
            Set<String> headers = Arrays.stream(ids).filter(Objects::nonNull).map(id -> id.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
            if (!headers.contains(deviceHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader) && !sensingDeviceFoundFromProvenance) {
                csvValidation.addMissingHeaders(Arrays.asList(deviceHeader + " or " + targetHeader + " or " + soHeader));
            }
            // Check that there is an soHeader or a targetHeader if there is an annotationHeader otherwise create error
            if(headers.contains(annotationHeader) && !headers.contains(targetHeader) && !headers.contains(soHeader)){
                csvValidation.addMissingHeaders(Arrays.asList(targetHeader + " or " + soHeader));
            }
            
            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
            VariableDAO dao = new VariableDAO(sparql,nosql,fs);

            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    String header = ids[i];
                    if (header == null) {
                        csvValidation.addEmptyHeader(i+1);
                    } else {                       
                    
                        if (header.equalsIgnoreCase(expHeader) || header.equalsIgnoreCase(targetHeader) 
                            || header.equalsIgnoreCase(dateHeader) || header.equalsIgnoreCase(deviceHeader) || header.equalsIgnoreCase(soHeader)
                            || header.equalsIgnoreCase(rawdataHeader) || header.equalsIgnoreCase(annotationHeader)) {
                            headerByIndex.put(i, header);                            
                        
                        } else {                        
                            try {
                                if (!URIDeserializer.validateURI(header)) {
                                    csvValidation.addInvalidHeaderURI(i, header);
                                } else {                                    
                                    VariableModel var = dao.get(URI.create(header));
                                    // boolean uriExists = sparql.uriExists(VariableModel.class, URI.create(header));
                                    if (var == null) {
                                        csvValidation.addInvalidHeaderURI(i, header);
                                    } else {
                                        mapVariableUriDataType.put(var.getUri(), var.getDataType());
                                        // TODO : Validate duplicate variable colonne
                                        headerByIndex.put(i, header);
                                    }                                                                               
                                }
                            } catch (URISyntaxException e) {
                                csvValidation.addInvalidHeaderURI(i, ids[i]);
                            }
                        }
                    }
                }

                // 1.1 return error variables
                if (csvValidation.hasErrors()) {
                    return csvValidation;
                }
                csvValidation.setHeadersFromArray(ids);

                int rowIndex = 0;
                String[] values;

                // Line 2
                String[] headersLabels = csvReader.parseNext();
                csvValidation.setHeadersLabelsFromArray(headersLabels);

                // Line 3
                csvReader.parseNext();
                // Line 4
                int nbError = 0;
                boolean validateCSVRow = false;
                while ((values = csvReader.parseNext()) != null) {
                    try {
                        validateCSVRow = validateCSVRow(
                                provenance,
                                experiment,
                                sensingDeviceFoundFromProvenance,
                                variableCheckedDevice,
                                variableCheckedProvDevice,
                                checkedVariables,
                                values, 
                                rowIndex, 
                                csvValidation, 
                                headerByIndex, 
                                xpDAO,
                                notExistingExperiments,
                                duplicatedExperiments,
                                nameURIExperiments,                                
                                ontologyDAO,
                                notExistingTargets,
                                duplicatedTargets,
                                nameURITargets,
                                scientificObjectDAO,
                                nameURIScientificObjectsInXp,
                                scientificObjectsNotInXp,
                                deviceDAO,
                                notExistingDevices,
                                duplicatedDevices,
                                nameURIDevices,
                                mapVariableUriDataType, 
                                duplicateDataByIndex);
                    } catch (CSVDataTypeException e) {
                        csvValidation.addInvalidDataTypeError(e.getCsvCell());
                    }
                    rowIndex++;
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= ExperimentAPI.CSV_NB_ERRORS_MAX) {
                        break;
                    }
                }
            }
        }

        if (csvValidation.getData().keySet().size() >  DataAPI.SIZE_MAX) {
            csvValidation.setTooLargeDataset(true);
        }
        
        return csvValidation;
    }

    private boolean validateCSVRow(
            ProvenanceModel provenance,
            URI experiment,
            boolean sensingDeviceFoundFromProvenance,
            Map<String, DeviceModel> variableCheckedDevice,
            Map<String,DeviceModel> variableCheckedProvDevice,
            List<String> checkedVariables,
            String[] values, 
            int rowIndex, 
            DataCSVValidationModel csvValidation, 
            Map<Integer, String> headerByIndex, 
            ExperimentDAO xpDAO,
            List<String> notExistingExperiments,
            List<String> duplicatedExperiments,
            Map<String, ExperimentModel> nameURIExperiments,
            OntologyDAO ontologyDAO, 
            List<String> notExistingTargets,
            List<String> duplicatedTargets,
            Map<String, SPARQLNamedResourceModel> nameURITargets,
            ScientificObjectDAO scientificObjectDAO,
            Map<String, SPARQLNamedResourceModel> nameURIScientificObjects,
            List<String> scientificObjectsNotInXp,
            DeviceDAO deviceDAO, 
            List<String> notExistingDevices,
            List<String> duplicatedDevices,
            Map<String, DeviceModel> nameURIDevices,
            HashMap<URI, URI> mapVariableUriDataType, 
            List<ImportDataIndex> duplicateDataByIndex) 
        throws CSVDataTypeException, TimezoneAmbiguityException, TimezoneException, URISyntaxException, Exception {
        
        boolean validRow = true;

        ParsedDateTimeMongo parsedDateTimeMongo = null;

        List<URI> experiments = new ArrayList<>();
        SPARQLNamedResourceModel target = null;
        
        Boolean missingTargetOrDevice = false;
        int targetColIndex = 0;
        int deviceColIndex = 0;

        AnnotationModel annotationFromAnnotationColumn = null;
        int annotationIndex = 0;

        DeviceModel deviceFromDeviceColumn = null;
        SPARQLNamedResourceModel object = null;
        if( experiment != null) {
            experiments.add(experiment);
        }

        //Set to remember which columns to do at end of row iteration (in case required columns like target are at the end).
        Set<Integer> colsToDoAtEnd = new HashSet<>();

        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (headerByIndex.get(colIndex).equalsIgnoreCase(expHeader)) {
                //check experiment column
                ExperimentModel exp = null;
                String expNameOrUri = values[colIndex];
                // test in uri list
                if (!StringUtils.isEmpty(expNameOrUri)) {
                    if (nameURIExperiments.containsKey(expNameOrUri)) {
                        exp = nameURIExperiments.get(expNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedExperiments.contains(expNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addDuplicateExperimentError(cell);
                            validRow = false;
                        } else if (!notExistingExperiments.contains(expNameOrUri)) {
                            try {
                                exp = getExperimentByNameOrURI(xpDAO, expNameOrUri);
                                if (exp == null) {
                                    if (!notExistingExperiments.contains(expNameOrUri)) {
                                        notExistingExperiments.add(expNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                                    csvValidation.addInvalidExperimentError(cell);
                                    validRow = false;
                                } else {
                                    nameURIExperiments.put(expNameOrUri, exp);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                                csvValidation.addDuplicateExperimentError(cell);
                                duplicatedExperiments.add(expNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, expNameOrUri, "EXPERIMENT_ID");
                            csvValidation.addInvalidExperimentError(cell);
                            validRow = false;

                        }
                    }
                }
                if (exp != null) {
                    experiments.add(exp.getUri());
                }


            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(targetHeader)) {
                //check target column
                String targetNameOrUri = values[colIndex];
                targetColIndex = colIndex;

                if (!StringUtils.isEmpty(targetNameOrUri)){
                    if (nameURITargets.containsKey(targetNameOrUri)) {
                        target = nameURITargets.get(targetNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedTargets.contains(targetNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addDuplicateTargetError(cell);
                            validRow = false;
                        } else if (!notExistingTargets.contains(targetNameOrUri)) {
                            try {
                                target = getTargetByNameOrURI(ontologyDAO, targetNameOrUri);
                                if (target == null) {
                                    if (!notExistingTargets.contains(targetNameOrUri)) {
                                        notExistingTargets.add(targetNameOrUri);
                                    }

                                    CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                                    csvValidation.addInvalidTargetError(cell);
                                    validRow = false;
                                } else {
                                    nameURITargets.put(targetNameOrUri, target);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                                csvValidation.addDuplicateTargetError(cell);
                                duplicatedTargets.add(targetNameOrUri);
                                validRow = false;
                            }

                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, targetNameOrUri, "TARGET_ID");
                            csvValidation.addInvalidTargetError(cell);
                            validRow = false;
                        }

                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(soHeader)) {

                String objectNameOrUri = values[colIndex];
                // check if the object name/uri has been previously referenced -> if so, no need to re-perform a check with the Dao
                if (!StringUtils.isEmpty(objectNameOrUri) && nameURIScientificObjects.containsKey(objectNameOrUri)) {
                    object = nameURIScientificObjects.get(objectNameOrUri);
                } else {

                    SPARQLNamedResourceModel existingOs = null;
                    Node experimentNode = experiment == null ? null : SPARQLDeserializers.nodeURI(experiment);

                    // check if the object has been previously referenced as unknown, if not, then performs a check with Dao
                    if (!StringUtils.isEmpty(objectNameOrUri) && !scientificObjectsNotInXp.contains(objectNameOrUri)) {
                        existingOs = testNameOrURI(scientificObjectDAO, csvValidation, rowIndex, colIndex, experimentNode, objectNameOrUri);
                    }

                    if(existingOs == null){
                        validRow = false;
                        scientificObjectsNotInXp.add(objectNameOrUri);
                    }else{
                        object = existingOs;
                        // object exist, put it into name/URI cache
                        nameURIScientificObjects.put(objectNameOrUri,existingOs);
                    }
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(dateHeader)) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }

            } else if (headerByIndex.get(colIndex).equalsIgnoreCase(deviceHeader)){
                // check device column
                String deviceNameOrUri = values[colIndex];
                deviceColIndex = colIndex;

                // test in uri list
                if (!StringUtils.isEmpty(deviceNameOrUri)) {
                    if (nameURIDevices.containsKey(deviceNameOrUri)) {
                        deviceFromDeviceColumn = nameURIDevices.get(deviceNameOrUri);
                    } else {
                        // test not in uri list
                        if (duplicatedDevices.contains(deviceNameOrUri)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addDuplicateDeviceError(cell);
                            validRow = false;
                        } else if (!notExistingDevices.contains(deviceNameOrUri)) {
                            try {
                                deviceFromDeviceColumn = getDeviceByNameOrURI(deviceDAO, deviceNameOrUri);
                                if (deviceFromDeviceColumn == null) {
                                    if (!notExistingDevices.contains(deviceNameOrUri)) {
                                        notExistingDevices.add(deviceNameOrUri);
                                    }
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                                    csvValidation.addInvalidDeviceError(cell);
                                    validRow = false;
                                } else {
                                    nameURIDevices.put(deviceNameOrUri, deviceFromDeviceColumn);
                                }
                            } catch (DuplicateNameException e) {
                                CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                                csvValidation.addDuplicateDeviceError(cell);
                                duplicatedDevices.add(deviceNameOrUri);
                                validRow = false;
                            }
                        } else {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, deviceNameOrUri, "DEVICE_ID");
                            csvValidation.addInvalidDeviceError(cell);
                            validRow = false;
                        }

                    }
                }

            }
            //If we are at the annotation column, and the cell isn't empty, create a new Annotation Model.
            //Set the motivation to commenting, and leave the target for now until we're sure that the target column has already been imported
            else if (headerByIndex.get(colIndex).equalsIgnoreCase(annotationHeader)){
                String annotation = values[colIndex];
                annotationIndex = colIndex;
                if(!StringUtils.isEmpty(annotation)){
                    annotationFromAnnotationColumn = new AnnotationModel();
                    annotationFromAnnotationColumn.setDescription(annotation.trim());
                    annotationFromAnnotationColumn.setPublisher(user.getUri());
                    MotivationModel motivationModel = new MotivationModel();
                    motivationModel.setUri(URI.create(OA.commenting.getURI()));
                    annotationFromAnnotationColumn.setMotivation(motivationModel);
                }
            }else if (!headerByIndex.get(colIndex).equalsIgnoreCase(rawdataHeader)) { // Variable/Value column
                if (headerByIndex.containsKey(colIndex)) {
                    // If value is not blank and null
                    if (!StringUtils.isEmpty(values[colIndex])) {
                        colsToDoAtEnd.add(colIndex);
                    }
                }
            }
        }
        //Do the variable value columns now that we know the target or device is loaded if the user correctly filled it
        for(Integer colIndex : colsToDoAtEnd){
            if (validRow) {
                String variable = headerByIndex.get(colIndex);
                URI varURI = URI.create(variable);
                if (deviceFromDeviceColumn == null && target == null && object == null) {
                    missingTargetOrDevice = true;
                    validRow = false;
                    break;
                }
                if (deviceFromDeviceColumn != null) {
                    boolean variableIsChecked = variableCheckedDevice.containsKey(variable) && variableCheckedDevice.get(variable) == deviceFromDeviceColumn;
                    if (!variableIsChecked) {
                        if (!variableIsAssociatedToDevice(deviceFromDeviceColumn, varURI)) {
                            csvValidation.addVariableToDevice(deviceFromDeviceColumn, varURI);
                        }
                        variableCheckedDevice.put(variable, deviceFromDeviceColumn);
                    }

                } else if (sensingDeviceFoundFromProvenance) {
                    if (!checkedVariables.contains(variable)) { // do it one time but write the error on each row if there is one
                        List<DeviceModel> devices = new ArrayList<>();
                        List<DeviceModel> linkedDevice = new ArrayList<>();
                        DeviceModel dev = null;
                        for (AgentModel agent : provenance.getAgents()) {
                            if (agent.getRdfType() != null && deviceDAO.isDeviceType(agent.getRdfType())) {
                                dev = deviceDAO.getDeviceByURI(agent.getUri(), user);
                                if (dev != null) {

                                    if (variableIsAssociatedToDevice(dev, varURI)) {
                                        linkedDevice.add(dev);
                                    }
                                    devices.add(dev);
                                }
                            }
                        }
                        switch (linkedDevice.size()) {
                            case 0:
                                if (devices.size() > 1) {
                                    //which device to choose ?
                                    CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                                    csvValidation.addDeviceChoiceAmbiguityError(cell);
                                    validRow = false;
                                    break;
                                } else {
                                    if (!devices.isEmpty()) {
                                        csvValidation.addVariableToDevice(devices.get(0), varURI);
                                        variableCheckedProvDevice.put(variable, devices.get(0));
                                    } else {
                                        if (target == null) {
                                            missingTargetOrDevice = true;
                                            validRow = false;
                                            break;
                                        }
                                    }
                                }
                                break;
                            case 1:

                                variableCheckedProvDevice.put(variable, linkedDevice.get(0));
                                break;

                            default:
                                //which device to choose ?
                                CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID"); // add specific exception
                                csvValidation.addDeviceChoiceAmbiguityError(cell);
                                validRow = false;
                                break;
                        }
                        checkedVariables.add(variable);
                    } else {
                        if (!variableCheckedProvDevice.containsKey(variable)) {
                            CSVCell cell = new CSVCell(rowIndex, colIndex, provenance.getUri().toString(), "DEVICE_AMBIGUITY_ID");  // add specific exception
                            csvValidation.addDeviceChoiceAmbiguityError(cell);
                            break;
                        }
                    }
                }
                if(validRow) {
                    DataModel dataModel = new DataModel();
                    DataProvenanceModel provenanceModel = new DataProvenanceModel();
                    provenanceModel.setUri(provenance.getUri());

                    if (!experiments.isEmpty()) {
                        provenanceModel.setExperiments(experiments);
                    }

                    if (deviceFromDeviceColumn != null) {
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = getRootDeviceTypes();
                        }
                        URI rootType = rootDeviceTypes.get(deviceFromDeviceColumn.getType());
                        agent.setType(rootType);
                        agent.setUri(deviceFromDeviceColumn.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    } else if (sensingDeviceFoundFromProvenance) {

                        DeviceModel checkedDevice = variableCheckedProvDevice.get(variable);
                        ProvEntityModel agent = new ProvEntityModel();
                        if (rootDeviceTypes == null) {
                            rootDeviceTypes = getRootDeviceTypes();
                        }
                        URI rootType = rootDeviceTypes.get(checkedDevice.getType());
                        agent.setType(rootType);
                        agent.setUri(checkedDevice.getUri());
                        provenanceModel.setProvWasAssociatedWith(Collections.singletonList(agent));

                    }

                    dataModel.setDate(parsedDateTimeMongo.getInstant());
                    dataModel.setOffset(parsedDateTimeMongo.getOffset());
                    dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());

                    if (object != null) {
                        dataModel.setTarget(object.getUri());
                    }
                    if (target != null) {
                        dataModel.setTarget(target.getUri());
                    }
                    dataModel.setProvenance(provenanceModel);
                    dataModel.setVariable(varURI);
                    DataValidateUtils.checkAndConvertValue(dataModel, varURI, values[colIndex].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex, csvValidation);

                    if (colIndex + 1 < values.length) {
                        if (headerByIndex.get(colIndex + 1).equalsIgnoreCase(rawdataHeader) && values[colIndex + 1] != null) {
                            dataModel.setRawData(DataValidateUtils.returnValidRawData(varURI, values[colIndex + 1].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex + 1, csvValidation));
                        }
                    }

                    // check for duplicate data
                    URI targetUri = null;
                    URI deviceUri = null;
                    if (target != null) {
                        targetUri = target.getUri();
                    }
                    if (object != null) {
                        targetUri = object.getUri();
                    }
                    if(deviceFromDeviceColumn != null) {
                        deviceUri = deviceFromDeviceColumn.getUri();
                    }
                    ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(), varURI, provenance.getUri(), targetUri, deviceUri);
                    if (!duplicateDataByIndex.contains(importDataIndex)) {
                        duplicateDataByIndex.add(importDataIndex);
                    } else {
                        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
                        CSVCell duplicateCell = new CSVCell(rowIndex, colIndex, values[colIndex].trim(), variableName);
                        csvValidation.addDuplicatedDataError(duplicateCell);
                    }
                    csvValidation.addData(dataModel, rowIndex);

                }

            }
        }
        // If an AnnotationModel was created on this row as well as a target, we need to set the Annotation's target
        if( annotationFromAnnotationColumn != null ){
            if(target == null && object == null){
                CSVCell annotationCell = new CSVCell(rowIndex, annotationIndex, annotationFromAnnotationColumn.getDescription(), annotationHeader);
                csvValidation.addInvalidAnnotationError(annotationCell);
                validRow = false;
            }else{
                if(validRow){
                    annotationFromAnnotationColumn.setTargets(Collections.singletonList( target==null ? object.getUri() : target.getUri()));
                    csvValidation.addToAnnotationsOnObjects(annotationFromAnnotationColumn);
                }
            }
        }

        if (missingTargetOrDevice) {
            //the device or the target is mandatory if there is no device in the provenance
            CSVCell cell1 = new CSVCell(rowIndex, deviceColIndex, null, deviceHeader);
            CSVCell cell2 = new CSVCell(rowIndex, targetColIndex, null, targetHeader);
            csvValidation.addMissingRequiredValue(cell1);
            csvValidation.addMissingRequiredValue(cell2);
        }
        
        return validRow;
    }



    private ExperimentModel getExperimentByNameOrURI(ExperimentDAO xpDAO, String expNameOrUri) throws Exception {
        ExperimentModel exp = null;
        if (URIDeserializer.validateURI(expNameOrUri)) {
            URI expUri = URI.create(expNameOrUri);
            try {
                exp = xpDAO.get(expUri, user);
            } catch (Exception ex) {

            }
        } else {
            exp = xpDAO.getByName(expNameOrUri);
        }
        return exp;
    }

    private DeviceModel getDeviceByNameOrURI(DeviceDAO deviceDAO, String deviceNameOrUri) throws Exception {
        DeviceModel device;
        if (URIDeserializer.validateURI(deviceNameOrUri)) {
            URI deviceURI = URI.create(deviceNameOrUri);
            device = deviceDAO.getDeviceByURI(deviceURI, user);
        } else {
            try {
                device = deviceDAO.getByName(deviceNameOrUri);
            } catch (Exception ex) {
                throw ex;
            }

        }
        return device;
    }

    private SPARQLNamedResourceModel<?> getTargetByNameOrURI(OntologyDAO dao, String targetNameOrUri) throws Exception {
        SPARQLNamedResourceModel<?> target = new SPARQLNamedResourceModel<>();
        if (URIDeserializer.validateURI(targetNameOrUri)) {
            URI targetUri = URI.create(targetNameOrUri);
            if (sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(targetUri), RDFS.label, "?label")
            )) {
                target.setUri(targetUri);
            } else {
                target = null;
            }
        } else {
            List<SPARQLNamedResourceModel> results = dao.getByName(targetNameOrUri);
            if (results.size()>1) {
                throw new DuplicateNameException(targetNameOrUri);
            } else {
                if(!results.isEmpty()) {
                    target = results.get(0);
                } else {
                    target = null ;
                }
            }
        }
        return target;
    }


    private SPARQLNamedResourceModel testNameOrURI(ScientificObjectDAO dao, CSVValidationModel validation, int rowIndex, int colIndex, Node experiment, String nameOrUri) throws Exception {

        // check if object exist by URI inside experiment
        if (URIDeserializer.validateURI(nameOrUri)) {
            URI objectUri = URI.create(nameOrUri);

            SPARQLNamedResourceModel existingObject = sparql.getByURI(experiment,ScientificObjectModel.class,objectUri,null);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_ID"));
                return null;
            }
            return existingObject;

        // check if object exist by name inside experiment
        } else if (experiment != null) {
            SPARQLNamedResourceModel existingObject = dao.getUriByNameAndGraph(experiment, nameOrUri);
            if (existingObject == null) {
                validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_ID"));
                return null;
            }
            return existingObject;
        } else {
            // ambiguity about name inside global OS graph, indeed, several OS can have the same name inside the global graph,
            // so there are no guarantee that a unique OS URI will be found with this name
            validation.addInvalidValueError(new CSVCell(rowIndex, colIndex, nameOrUri, "OBJECT_NAME_AMBIGUITY_IN_GLOBAL_CONTEXT"));
            return null;
        }
    }

    // Map who associate each type with its root type
    private Map<URI, URI> getRootDeviceTypes() throws URISyntaxException, Exception {

        SPARQLTreeListModel<ClassModel> treeList = SPARQLModule.getOntologyStoreInstance().searchSubClasses(new URI(Oeso.Device.toString()), null, user.getLanguage(), true);
        List<ResourceTreeDTO> treeDtos = ResourceTreeDTO.fromResourceTree(treeList);

        Map<URI, URI> map = new HashMap<>();

        for (ResourceTreeDTO tree : treeDtos) {
            URI agentRootType = tree.getUri();
            List<ResourceTreeDTO> children = tree.getChildren();
            if (!children.isEmpty()) {
                childrenToRoot(children, map, agentRootType);
            }

            // Push root type inside map
            // It allow to recognize device with a type included inside the root types list
            map.put(tree.getUri(),tree.getUri());
        }

        return map;
    }
    private void childrenToRoot( List<ResourceTreeDTO> children,Map<URI, URI> map, URI agentRootType){
        for (ResourceTreeDTO subTree : children) {
            map.put(subTree.getUri(), agentRootType);
            List<ResourceTreeDTO> child = subTree.getChildren();
            if (!child.isEmpty()) {
                childrenToRoot(child, map, agentRootType);
            }
        }
    }

    /**
     * Create a DataSimpleProvenanceGetDTO with uri and name from a data provenance model.
     * @detail
     * Analyze the provenance from the data model and do as follows:
     *  if there is one agent (device or operator), retrieve the uri and name of the agent
     *  otherwise, take the uri and name from the provenance model
     * @param dataProvModel
     * @return a simple data provenance with uri and name attributes
     * @throws Exception
     */
    private DataSimpleProvenanceGetDTO createDataSimpleProvenance(DataProvenanceModel dataProvModel)
            throws Exception {
        DataSimpleProvenanceGetDTO dto = new DataSimpleProvenanceGetDTO();

        List<ProvEntityModel> provEntityList = dataProvModel.getProvWasAssociatedWith();

        if (provEntityList != null && provEntityList.size() == 1) {
            OntologyDAO ontologyDao = new OntologyDAO(sparql);
            URI uri = provEntityList.get(0).getUri();
            dto.setUri(uri);
            dto.setName(ontologyDao.getURILabel(uri, user.getLanguage()));
        }
        else {
            ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
            ProvenanceModel provModel = provDAO.get(dataProvModel.getUri());
            dto.setUri(provModel.getUri());
            dto.setName(provModel.getName());
        }

        return dto;
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

        DataDaoV2 dataDAO = new DataDaoV2(sparql, nosql, fs);
        VariableDAO variableDAO = new VariableDAO(sparql, nosql, fs);

        Instant start, end;

        Instant startInstant = (startDate != null) ? Instant.parse(startDate) : null;
        Instant endInstant = (endDate != null) ? Instant.parse(endDate) : Instant.now();

        VariableDetailsDTO variable = new VariableDetailsDTO(variableDAO.get(variableUri));
        DataVariableSeriesGetDTO dto = new DataVariableSeriesGetDTO(variable);

        /// Get last stored data
        DataSearchFilter getLastFoundDataFilter = new DataSearchFilter();
        getLastFoundDataFilter.setUser(user);
        getLastFoundDataFilter.setTargets(Collections.singletonList(facilityUri));
        getLastFoundDataFilter.setVariables(Collections.singletonList(variableUri));

        DataComputedGetDTO lastData = dataDAO.getLastDataFound(getLastFoundDataFilter);
        dto.setLastData(lastData);

        /// Retrieve median series
        start = Instant.now();
        List<DataComputedModel> dataModels = dataDAO.computeAllMediansPerHour(
                user,
                facilityUri,
                variableUri,
                startInstant,
                endInstant);
        end = Instant.now();
        LOGGER.debug(dataModels.size() + " data retrieved from mongo : " + Long.toString(Duration.between(start, end).toMillis()) + " milliseconds elapsed");

        Map<DataProvenanceModel, List<DataComputedModel>> provenancesMap;

        List<DataSerieGetDTO> dataSeriesDTOs = new ArrayList<>();
        List<DataComputedModel> medians = new ArrayList<>();

        provenancesMap = dataModels.stream().collect(Collectors.groupingBy(DataComputedModel::getProvenance));

        for (Map.Entry<DataProvenanceModel, List<DataComputedModel>> entryProv : provenancesMap.entrySet()) {

            List<DataComputedModel> medianSerie = entryProv.getValue()
                    .stream()
                    .sorted(Comparator.comparing(DataComputedModel::getDate))
                    .collect(Collectors.toList());

            // adjust datetime for median data by setting it to the middle of the hour it represents
            medianSerie.forEach(data -> {
                Instant middleDate = data.getDate().truncatedTo(ChronoUnit.HOURS);
                data.setDate(middleDate.plus(30, ChronoUnit.MINUTES));
            });

            medians.addAll(medianSerie);

            DataSimpleProvenanceGetDTO provenance = createDataSimpleProvenance(entryProv.getKey());

            List<DataComputedGetDTO> medianSerieDTO = medianSerie.stream()
                    .map(DataComputedGetDTO::getDtoFromModel)
                    .collect(Collectors.toList());
            DataSerieGetDTO dataSerie = new DataSerieGetDTO(provenance, medianSerieDTO);
            dataSeriesDTOs.add(dataSerie);
        }

        if (!calculatedOnly) {
            dto.setDataSeries(dataSeriesDTOs);
        }
        else if (dataSeriesDTOs.size() == 1) {
            dto.setCalculatedSeries(dataSeriesDTOs);
        }

        /// Compute calculated series

        if (dataSeriesDTOs.size() > 1) {

            List<DataSerieGetDTO> dataCalculatedSeriesDTOs = new ArrayList<>();

            DataSimpleProvenanceGetDTO provMedian = new DataSimpleProvenanceGetDTO();
            provMedian.setName("median_per_hour");

            List<DataComputedModel> medianOfMedians = computeMedianPerHour(medians);
            List<DataComputedGetDTO> medianOfMediansDTO = medianOfMedians.stream()
                    .map(DataComputedGetDTO::getDtoFromModel)
                    .collect(Collectors.toList());
            dataCalculatedSeriesDTOs.add(new DataSerieGetDTO(provMedian, medianOfMediansDTO));

            DataSimpleProvenanceGetDTO provAverage = new DataSimpleProvenanceGetDTO();
            provAverage.setName("mean_per_day");

            List<DataComputedModel> averageSerie = dataDAO.computeAllMeanPerDay(
                    user,
                    facilityUri,
                    variableUri,
                    startInstant,
                    endInstant);
            List<DataComputedGetDTO> averageSerieDtos = averageSerie
                    .stream()
                    .map((d) -> DataComputedGetDTO.getDtoFromModel(d))
                    .sorted(Comparator.comparing(DataComputedGetDTO::getDate))
                    .collect(Collectors.toList());
            dataCalculatedSeriesDTOs.add(new DataSerieGetDTO(provAverage, averageSerieDtos));

            dto.setCalculatedSeries(dataCalculatedSeriesDTOs);
        }

        return new SingleObjectResponse<>(dto).getResponse();
    }

    /**
     * Fetches and return all URIs for the variable with the xsd:date datatype.
     *
     * @return
     * @throws Exception
     */
    public Set<URI> getAllDateVariables() throws Exception {
        return new HashSet<>(sparql.searchURIs(VariableModel.class, null, selectBuilder -> {
            Var uriVar = SPARQLQueryHelper.makeVar(VariableModel.URI_FIELD);
            selectBuilder.addWhere(uriVar, Oeso.hasDataType.asNode(), XSD.date.asNode());
        }));
    }

}
