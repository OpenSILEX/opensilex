//******************************************************************************
//                          ExperimentAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import org.opensilex.core.experiment.utils.ExportDataIndex;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.bulk.BulkWriteError;
import com.opencsv.CSVWriter;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.swagger.annotations.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import static java.lang.Float.NaN;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.Max;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opensilex.core.data.api.DataAPI;
import static org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_MODIFICATION_ID;
import static org.opensilex.core.data.api.DataAPI.CREDENTIAL_DATA_MODIFICATION_LABEL_KEY;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_CONFIDENCE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MAXIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_METADATA;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_MINIMAL_DATE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_OBJECTURI;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_PROVENANCEURI;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_TIMEZONE;
import static org.opensilex.core.data.api.DataAPI.DATA_EXAMPLE_VARIABLEURI;
import org.opensilex.core.data.api.DataCSVValidationDTO;
import static org.opensilex.core.data.api.DataCreationDTO.NAN_VALUES;
import static org.opensilex.core.data.api.DataCreationDTO.NA_VALUES;
import org.opensilex.core.data.api.DataGetDTO;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.data.utils.DataValidateUtils;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.exception.CSVDataTypeException;
import org.opensilex.core.exception.DataTypeException;
import org.opensilex.core.exception.TimezoneAmbiguityException;
import org.opensilex.core.exception.TimezoneException;
import org.opensilex.core.exception.UnableToParseDateException;
import org.opensilex.core.experiment.factor.api.FactorDetailsGetDTO;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.experiment.utils.ImportDataIndex;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityGetDTO;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.api.ProvenanceGetDTO;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
import org.opensilex.nosql.mongodb.MongoDBService;
 import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 * @author Julien BONNEFONT
 */
@Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
@Path("/core/experiments")
@ApiCredentialGroup(
        groupId = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID,
        groupLabelKey = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY
)
public class ExperimentAPI {

    Logger LOGGER = LoggerFactory.getLogger(ExperimentAPI.class);

    public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
    public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";

    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.experiment.modification";

    public static final String CREDENTIAL_EXPERIMENT_DELETE_ID = "experiment-delete";
    public static final String CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY = "credential.experiment.delete";

    public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    

    public static final int CSV_NB_ERRORS_MAX = 100;

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    /**
     * Create an Experiment
     *
     * @param dto the Experiment to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation("Add an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "An experiment is created", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An experiment with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
    ) throws Exception {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            // Return error response 409 - CONFLICT if experiment URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Experiment already exists",
                    "Duplicated URI: " + e.getUri()
            ).getResponse();
        }
    }

    /**
     * @param xpDto the Experiment to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Experiment {@link URI}
     */
    @PUT
    @ApiOperation("Update an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response updateExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);

        ExperimentModel model = xpDto.newModel();
        dao.update(model, currentUser);
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    /**
     * @param xpUri the Experiment URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the {@link ExperimentGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment retrieved", response = ExperimentGetDTO.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response getExperiment(
            @ApiParam(value = "Experiment URI", example = "http://opensilex.dev/set/experiments/ZA17", required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        ExperimentModel model = dao.get(xpUri, currentUser);
        return new SingleObjectResponse<>(ExperimentGetDTO.fromModel(model)).getResponse();
    }

    /**
     * Search experiments
     *
     * @param name
     * @param year
     * @param isEnded
     * @param species
     * @param factorCategories
     * @param projects
     * @param isPublic
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     * @throws java.lang.Exception
     * @see ExperimentDAO
     */
    @GET
    @ApiOperation("Search experiments")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return experiments", response = ExperimentGetListDTO.class, responseContainer = "List")
    })
    public Response searchExperiments(
            @ApiParam(value = "Regex pattern for filtering by name", example = "ZA17") @QueryParam("name") String name,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year")  Integer year,
            @ApiParam(value = "Search ended(false) or active experiments(true)") @QueryParam("is_ended") Boolean isEnded,
            @ApiParam(value = "Search by involved species", example = "http://www.phenome-fppn.fr/id/species/zeamays") @QueryParam("species") List<URI> species,
            @ApiParam(value = "Search by studied effect", example = "http://purl.obolibrary.org/obo/CHEBI_25555") @QueryParam("factors") List<URI> factorCategories,
            @ApiParam(value = "Search by related project uri", example = "http://www.phenome-fppn.fr/projects/ZA17\nhttp://www.phenome-fppn.fr/id/projects/ZA18") @QueryParam("projects") List<URI> projects,
            @ApiParam(value = "Search private(false) or public experiments(true)") @QueryParam("is_public") Boolean isPublic,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);

        ListWithPagination<ExperimentModel> resultList = xpDao.search(
                year,
                name,
                species,
                factorCategories,
                isEnded,
                projects,
                isPublic,
                currentUser,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = resultList.convert(ExperimentGetListDTO.class, ExperimentGetListDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Remove an experiment
     *
     * @param xpUri the experiment URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Experiment {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response deleteExperiment(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        dao.delete(xpUri, currentUser);
        return new ObjectUriResponse(xpUri).getResponse();
    }

    
    @GET
    @Path("{uri}/facilities")
    @ApiOperation("Get facilities involved in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities list", response = InfrastructureFacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getFacilities(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        List<InfrastructureFacilityModel> facilities = xpDao.getFacilities(xpUri, currentUser);

        List<InfrastructureFacilityGetDTO> dtoList = facilities.stream().map(InfrastructureFacilityGetDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }
    
    @GET
    @Path("{uri}/available_facilities")
    @ApiOperation("Get facilities available for an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities list", response = InfrastructureFacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getAvailableFacilities(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        List<InfrastructureFacilityModel> facilities = xpDao.getAvailableFacilities(xpUri, currentUser);

        List<InfrastructureFacilityGetDTO> dtoList = facilities.stream().map(InfrastructureFacilityGetDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/species")
    @ApiOperation("Get species present in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Species list", response = SpeciesDTO.class, responseContainer = "List")
    })
    public Response getAvailableSpecies(
            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        SpeciesDAO dao = new SpeciesDAO(sparql);
        List<SpeciesModel> species = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/factors")
    @ApiOperation("Get factors with their levels associated to an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = FactorDetailsGetDTO.class, responseContainer = "List")
    })
    public Response getAvailableFactors(
            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        FactorDAO dao = new FactorDAO(sparql);
        List<FactorModel> factors = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<FactorDetailsGetDTO> dtoList = factors.stream().map(FactorDetailsGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/variables")
    @ApiOperation("Get variables involved in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables list", response = NamedResourceDTO.class, responseContainer = "List")
    })
    public Response getUsedVariables(
            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "Search by objects uris", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") List<URI> objects
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        DataDAO dao = new DataDAO(nosql, sparql, fs);
        List<URI> experiments = new ArrayList<>();
        experiments.add(xpUri);
        List<VariableModel> variables = dao.getUsedVariables(currentUser, experiments, objects);

        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();

    }
  
    @GET
    @Path("{uri}/data")
    @ApiOperation("Search data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = DataGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchExperimentDataList(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by objects", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") List<URI> objects,
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenances") List<URI> provenances,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }
        }

        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);

        xpDAO.validateExperimentAccess(xpUri, currentUser);
        
        List<URI> experiments = new ArrayList<>();
        experiments.add(xpUri);

        ListWithPagination<DataModel> resultList = dao.search(
                currentUser,
                experiments,
                objects,
                variables,
                provenances,
                startInstant,
                endInstant,
                confidenceMin,
                confidenceMax,
                metadataFilter,
                orderByList,
                page,
                pageSize
        );

        ListWithPagination<DataGetDTO> resultDTOList = resultList.convert(DataGetDTO.class, DataGetDTO::fromModel);

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     *
     * @param xpUri experimentUri
     * @param startDate startDate
     * @param endDate endDate
     * @param timezone timezone
     * @param objects objectUri
     * @param variables variableUri
     * @param confidenceMin confidenceMin
     * @param confidenceMax confidenceMax
     * @param provenanceUri provenanceUri
     * @param metadata metadata json filter
     * @param csvFormat long or wide format
     * @param orderByList orderByList
     * @param page page number
     * @param pageSize page size
     * @return
     * @throws Exception
     */
    @GET
    @Path("{uri}/data/export")
    @ApiOperation("export data")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with data list results in wide or long format"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportExperimentDataList(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri")  @ValidURI @NotNull URI xpUri,
            @ApiParam(value = "Search by minimal date", example = DATA_EXAMPLE_MINIMAL_DATE) @QueryParam("start_date") String startDate,
            @ApiParam(value = "Search by maximal date", example = DATA_EXAMPLE_MAXIMAL_DATE) @QueryParam("end_date") String endDate,
            @ApiParam(value = "Precise the timezone corresponding to the given dates", example = DATA_EXAMPLE_TIMEZONE) @QueryParam("timezone") String timezone,
            @ApiParam(value = "Search by objects", example = DATA_EXAMPLE_OBJECTURI) @QueryParam("scientific_objects") @ValidURI List<URI> objects,
            @ApiParam(value = "Search by variables", example = DATA_EXAMPLE_VARIABLEURI) @QueryParam("variables") @ValidURI List<URI> variables,
            @ApiParam(value = "Search by minimal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("min_confidence") @Min(0) @Max(1) Float confidenceMin,
            @ApiParam(value = "Search by maximal confidence index", example = DATA_EXAMPLE_CONFIDENCE) @QueryParam("max_confidence") @Min(0) @Max(1) Float confidenceMax,
            @ApiParam(value = "Search by provenance uri", example = DATA_EXAMPLE_PROVENANCEURI) @QueryParam("provenance") @ValidURI URI provenanceUri,
            @ApiParam(value = "Search by metadata", example = DATA_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Format wide or long", example = "wide") @DefaultValue("wide") @QueryParam("mode") String csvFormat,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);
        //convert dates
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            try {
                startInstant = DataValidateUtils.getDateInstant(startDate, timezone, Boolean.FALSE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        if (endDate != null) {
            try {
                endInstant = DataValidateUtils.getDateInstant(endDate, timezone, Boolean.TRUE);
            } catch (UnableToParseDateException e) {
                return new ErrorResponse(e).getResponse();
            } catch (ZoneRulesException e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, "WRONG TIMEZONE PARAMETER", e.getMessage())
                        .getResponse();
            }
        }

        Document metadataFilter = null;
        if (metadata != null) {
            try {
                metadataFilter = Document.parse(metadata);
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }
        }

        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);

        xpDAO.validateExperimentAccess(xpUri, currentUser);
        
        List<URI> experiments = new ArrayList<>();
        experiments.add(xpUri);
        
        // test prov
        List<URI> provenancesArrayList = new ArrayList<>();

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        if (provenanceUri != null) {
            try {
                provDAO.get(provenanceUri);
                provenancesArrayList = new ArrayList<>(Arrays.asList(provenanceUri));
            } catch (NoSQLInvalidURIException e) {
                throw new NotFoundURIException("Provenance URI not found: ", provenanceUri);

            }
        } else {
            Set<URI> provenancesByExperiment = dao.getProvenancesByExperiment(currentUser, xpUri);
            if (provenancesByExperiment.size() > 0) {
                provenancesArrayList = new ArrayList<>(provenancesByExperiment);
            }

        }

        if (provenancesArrayList.isEmpty()) {
            try (CSVWriter writer = new CSVWriter(new StringWriter())) {
                LocalDate date = LocalDate.now();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                String fileName = "export_data" + dtf.format(date);

                return Response.ok(writer.toString(), MediaType.TEXT_PLAIN_TYPE)
                        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                        .build();
            }
        }

        Instant start = Instant.now();
        List<DataModel> resultList = dao.search(currentUser, experiments, objects, variables, provenancesArrayList, startInstant, endInstant, confidenceMin, confidenceMax, metadataFilter, orderByList);
        Instant data = Instant.now();
        LOGGER.debug(resultList.size() + " observations retrieved " + Long.toString(Duration.between(start, data).toMillis()) + " milliseconds elapsed");

        Response prepareCSVExport = null;

        if (csvFormat.equals("long")) {
            prepareCSVExport = prepareCSVLongExportResponse(xpUri, resultList);
        } else {
            prepareCSVExport = prepareCSVWideExportResponse(xpUri, resultList);
        }

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        LOGGER.debug("Export data " + Long.toString(timeElapsed) + " milliseconds elapsed");

        return prepareCSVExport;
    }

    private Response prepareCSVWideExportResponse(URI xpUri, List<DataModel> resultList) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        Instant data = Instant.now();

        List<URI> variables = new ArrayList<>();

        Map<URI, ScientificObjectModel> objects = new HashMap<>();
        Map<URI, ProvenanceModel> provenances = new HashMap<>();
        Map<Instant, Map<ExportDataIndex, List<DataGetDTO>>> dataByIndexAndInstant = new HashMap<>();

        for (DataModel dataModel : resultList) {
            if (!objects.containsKey(dataModel.getScientificObjects().get(0))) {
                objects.put(dataModel.getScientificObjects().get(0), null);
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
            // add export data
            // <Instant => Map<ExportDataIndex(Prov,Object) => List<Data>>
            ExportDataIndex exportDataIndex = new ExportDataIndex(dataModel.getProvenance().getUri(),dataModel.getScientificObjects().get(0));

            if (!dataByIndexAndInstant.get(dataModel.getDate()).containsKey(exportDataIndex)) {
                dataByIndexAndInstant.get(dataModel.getDate()).put(exportDataIndex, new ArrayList<>());
            }
            dataByIndexAndInstant.get(dataModel.getDate()).get(exportDataIndex).add(DataGetDTO.fromModel(dataModel));
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();
        
        // first static columns
        defaultColumns.add("Experiment");
        defaultColumns.add("Scientific Object"); 
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

        VariableDAO variableDao = new VariableDAO(sparql);
        List<VariableModel> variablesModelList = variableDao.getList(variables);

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
            variableUriIndex.put(variableModel.getUri(), (defaultColumns.size() - 1));
        }
        // static supplementary columns
        defaultColumns.add("Data Description");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Scientific Object URI"); 
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        LOGGER.debug("Get " + variables.size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        ScientificObjectDAO scientificObjectDao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> listScientificObjectDao = scientificObjectDao.searchByURIs(xpUri, new ArrayList<>(objects.keySet()), currentUser);
        for (ScientificObjectModel scientificObjectModel : listScientificObjectDao) {
            objects.put(scientificObjectModel.getUri(), scientificObjectModel);
        }
        Instant scientificObjectTime = Instant.now();
        LOGGER.debug("Get " + listScientificObjectDao.size() + " scientificObject(s) " + Long.toString(Duration.between(variableTime, scientificObjectTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        ExperimentModel xp = xpDAO.get(xpUri, currentUser);
        // ObjectURI, ObjectName, Factor, Experiment, Date, Confidence, Variable n ...
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
            
            // Search in map indexed by date for prov, object and data
            // <Instant => Map<ExportDataIndex(Prov,Object) => List<Data>>

            for (Map.Entry<Instant, Map<ExportDataIndex, List<DataGetDTO>>> instantProvUriDataEntry : dataByIndexAndInstant.entrySet()) {
                Map<ExportDataIndex, List<DataGetDTO>> mapProvUriData = instantProvUriDataEntry.getValue();
                // Search in map indexed by  prov and data
                for (Map.Entry<ExportDataIndex, List<DataGetDTO>> provUriObjectEntry : mapProvUriData.entrySet()) {
                    List<DataGetDTO> val = provUriObjectEntry.getValue();
                        
                    // Each row is linked to a provenance 
                    ArrayList<String> csvRow = new ArrayList<>();
                    boolean first = true;
                    for (DataGetDTO dataGetDTO : val) {
                        if (first == true) {
                            ScientificObjectModel os = objects.get(dataGetDTO.getScientificObjects().get(0));
                            
                            // experiment
                            csvRow.add(xp.getName());
                            
                            // object
                            csvRow.add(os.getName());
                            
                            // date
                            csvRow.add(dataGetDTO.getDate());
                            // write blank columns
                            for (int i = 0; i < variables.size(); i++) {
                                csvRow.add("");
                            }
                            
                            // provenance
                            if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                                csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                            } else {
                                csvRow.add("");
                            }
                       
                            // experiment URI
                            csvRow.add(xp.getUri().toString());
                                 
                            // object URI
                            csvRow.add(os.getUri().toString());
                            
                            // provenance Uri
                            csvRow.add(dataGetDTO.getProvenance().getUri().toString());
                            
                            first = false;

                        }
                        // value
                        if(dataGetDTO.getValue() == null){
                            csvRow.set(variableUriIndex.get(dataGetDTO.getVariable()), null);
                        }else{
                            csvRow.set(variableUriIndex.get(dataGetDTO.getVariable()), dataGetDTO.getValue().toString());
                        }
                    }
                    
                    
                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);
                }
            }
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_wide_format" + dtf.format(date);

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }

    private Response prepareCSVLongExportResponse(URI xpUri, List<DataModel> resultList) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        Instant data = Instant.now();

        HashMap<URI, VariableModel> variables = new HashMap<>();
        HashMap<URI, ScientificObjectModel> objects = new HashMap<>();
        HashMap<URI, ProvenanceModel> provenances = new HashMap<>();

        HashMap<Instant, List<DataGetDTO>> dataByInstant = new HashMap<>();
        for (DataModel dataModel : resultList) {
            if (!objects.containsKey(dataModel.getScientificObjects().get(0))) {
                objects.put(dataModel.getScientificObjects().get(0), null);
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
            dataByInstant.get(dataModel.getDate()).add(DataGetDTO.fromModel(dataModel));
        }

        Instant dataTransform = Instant.now();
        LOGGER.debug("Data conversion " + Long.toString(Duration.between(data, dataTransform).toMillis()) + " milliseconds elapsed");

        List<String> defaultColumns = new ArrayList<>();
        
        defaultColumns.add("Experiment");
        defaultColumns.add("Scientific Object");
        defaultColumns.add("Date");
        defaultColumns.add("Variable");
        defaultColumns.add("Method");
        defaultColumns.add("Unit");
        defaultColumns.add("Value");
        defaultColumns.add("Data Description");
        defaultColumns.add("");
        defaultColumns.add("Experiment URI");
        defaultColumns.add("Scientific Object URI");
        defaultColumns.add("Variable URI");
        defaultColumns.add("Data Description URI");

        Instant variableTime = Instant.now();
        VariableDAO variableDao = new VariableDAO(sparql);
        List<VariableModel> variablesModelList = variableDao.getList(new ArrayList<>(variables.keySet()));
        for (VariableModel variableModel : variablesModelList) {
            variables.put(variableModel.getUri(), variableModel);
        }
        LOGGER.debug("Get " + variables.keySet().size() + " variable(s) " + Long.toString(Duration.between(dataTransform, variableTime).toMillis()) + " milliseconds elapsed");
        ScientificObjectDAO scientificObjectDao = new ScientificObjectDAO(sparql, nosql);
        List<ScientificObjectModel> listScientificObjectDao = scientificObjectDao.searchByURIs(xpUri, new ArrayList<>(objects.keySet()), currentUser);
        for (ScientificObjectModel scientificObjectModel : listScientificObjectDao) {
            objects.put(scientificObjectModel.getUri(), scientificObjectModel);
        }
        Instant scientificObjectTime = Instant.now();
        LOGGER.debug("Get " + listScientificObjectDao.size() + " scientificObject(s) " + Long.toString(Duration.between(variableTime, scientificObjectTime).toMillis()) + " milliseconds elapsed");

        ProvenanceDAO provenanceDao = new ProvenanceDAO(nosql);
        List<ProvenanceModel> listByURIs = provenanceDao.getListByURIs(new ArrayList<>(provenances.keySet()));
        for (ProvenanceModel prov : listByURIs) {
            provenances.put(prov.getUri(), prov);
        }
        Instant provenancesTime = Instant.now();
        LOGGER.debug("Get " + listByURIs.size() + " provenance(s) " + Long.toString(Duration.between(scientificObjectTime, provenancesTime).toMillis()) + " milliseconds elapsed");

        ExperimentModel xp = xpDAO.get(xpUri, currentUser);
        // See defaultColumns order
        //        Experiment
        //        Object
        //        Date
        //        Variable
        //        Method
        //        Unit
        //        Value
        //        Data Description
        //        
        //        Experiment URI
        //        Object URI
        //        Variable URI
        //        Data Description URI
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(defaultColumns.toArray(new String[defaultColumns.size()]));
            for (Map.Entry<Instant, List<DataGetDTO>> entry : dataByInstant.entrySet()) {
                List<DataGetDTO> val = entry.getValue();
                for (DataGetDTO dataGetDTO : val) {
                    ArrayList<String> csvRow = new ArrayList<>();
                    ScientificObjectModel os = objects.get(dataGetDTO.getScientificObjects().get(0));
                    // experiment
                    csvRow.add(xp.getName());
                    // object
                    csvRow.add(os.getName());
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
                   
                    // provenance
                    if (provenances.containsKey(dataGetDTO.getProvenance().getUri())) {
                        csvRow.add(provenances.get(dataGetDTO.getProvenance().getUri()).getName());
                    } else {
                        csvRow.add("");
                    }
                    csvRow.add("");
                    // experiment uri
                    csvRow.add(xp.getUri().toString());
                    // object uri
                    csvRow.add(os.getUri().toString());
                    // variable uri
                    csvRow.add(dataGetDTO.getVariable().toString());
                    // provenance Uri
                    csvRow.add(dataGetDTO.getProvenance().getUri().toString());
                    
                    String[] row = csvRow.toArray(new String[csvRow.size()]);
                    writer.writeNext(row);
                }
            }
            LocalDate date = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
            String fileName = "export_data_long_format" + dtf.format(date);

            Instant writeCSVTime = Instant.now();
            LOGGER.debug("Write CSV " + Long.toString(Duration.between(provenancesTime, writeCSVTime).toMillis()) + " milliseconds elapsed");

            return Response.ok(sw.toString(), MediaType.TEXT_PLAIN_TYPE)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();

        } catch (Exception e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, e.getMessage(), e).getResponse();

        }
    }
    
    
    @POST
    @Path("{uri}/data/import")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = DataCSVValidationDTO.class)})
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
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull @ValidURI URI xpUri,
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        DataDAO dao = new DataDAO(nosql, sparql, fs);

        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);

        xpDAO.validateExperimentAccess(xpUri, currentUser);

        // test prov
        ProvenanceModel provenanceModel = null;

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql); 
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        } 

        DataCSVValidationModel validation;
        
        
        
        validation = validateWholeCSV(xpUri, provenanceModel, file, currentUser);

        DataCSVValidationDTO csvValidation = new DataCSVValidationDTO();

        validation.setInsertionStep(true);
        validation.setValidCSV(!validation.hasErrors()); 
        validation.setNbLinesToImport(validation.getData().size()); 

        if (validation.isValidCSV()) {
            Instant start = Instant.now();
            try {
                dao.createAll(new ArrayList<>(validation.getData().keySet()));
                validation.setNbLinesImported(validation.getData().keySet().size());
            } catch (NoSQLTooLargeSetException ex) {
                validation.setTooLargeDataset(true);

            } catch (MongoBulkWriteException duplicateError) {
                List<BulkWriteError> bulkErrors = duplicateError.getWriteErrors();
                for (int i = 0; i < bulkErrors.size(); i++) {
                    int index = bulkErrors.get(i).getIndex();
                    ArrayList<DataModel> arrayList = new ArrayList<>(validation.getData().keySet());
                    DataGetDTO fromModel = DataGetDTO.fromModel(arrayList.get(index));
                    int variableIndex = validation.getHeaders().indexOf(fromModel.getVariable().toString());
                    String variableName = validation.getHeadersLabels().get(variableIndex) + '(' + validation.getHeaders().get(variableIndex) + ')';
                    CSVCell csvCell = new CSVCell(validation.getData().get(arrayList.get(index)), validation.getHeaders().indexOf(fromModel.getVariable().toString()), fromModel.getValue().toString(), variableName);
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
    @Path("{uri}/data/import_validation")
    @ApiOperation(value = "Import a CSV file for the given experiment URI and scientific object type.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Data file and metadata saved", response = DataCSVValidationDTO.class)})
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
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull @ValidURI URI xpUri,
            @ApiParam(value = "Provenance URI", example = ProvenanceAPI.PROVENANCE_EXAMPLE_URI) @QueryParam("provenance") @NotNull @ValidURI URI provenance,
            @ApiParam(value = "Data file", required = true, type = "file") @NotNull @FormDataParam("file") InputStream file,
            @FormDataParam("file") FormDataContentDisposition fileContentDisposition) throws Exception {
        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        // test prov
        ProvenanceModel provenanceModel = null;

        ProvenanceDAO provDAO = new ProvenanceDAO(nosql);
        try {
            provenanceModel = provDAO.get(provenance);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Provenance URI not found: ", provenance);
        } 

        DataCSVValidationModel validation;

        Instant start = Instant.now();
        validation = validateWholeCSV(xpUri, provenanceModel, file, currentUser);
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

    private DataCSVValidationModel validateWholeCSV(URI experimentURI, ProvenanceModel provenance, InputStream file, UserModel currentUser) throws Exception {
        DataCSVValidationModel csvValidation = new DataCSVValidationModel();
        ScientificObjectDAO scientificObjectDAO = new ScientificObjectDAO(sparql, nosql);

        Map<String, ScientificObjectModel> nameURIScientificObjectsInXp = new HashMap<>();
        List<String> scientificObjectsNotInXp = new ArrayList<>();

        Map<Integer, String> headerByIndex = new HashMap<>();

        List<ImportDataIndex> duplicateDataByIndex = new ArrayList<>(); 

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import data - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            // Line 1
            String[] ids = csvReader.parseNext();

            // 1. check variables
            HashMap<URI, URI> mapVariableUriDataType = new HashMap<>();
 
            VariableDAO dao = new VariableDAO(sparql);
            if (ids != null) {

                for (int i = 0; i < ids.length; i++) {
                    if (i > 1) {
                        String header = ids[i];
                        try {
                            if (header == null || !URIDeserializer.validateURI(header)) {
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
                        validateCSVRow = validateCSVRow(provenance, values, rowIndex, csvValidation, headerByIndex, experimentURI, scientificObjectDAO, nameURIScientificObjectsInXp, scientificObjectsNotInXp, mapVariableUriDataType,duplicateDataByIndex);
                    } catch (CSVDataTypeException e) {
                        csvValidation.addInvalidDataTypeError(e.getCsvCell());
                    }
                    rowIndex++;
                    if (!validateCSVRow) {
                        nbError++;
                    }
                    if (nbError >= CSV_NB_ERRORS_MAX) {
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

    private boolean validateCSVRow(ProvenanceModel provenance, String[] values, int rowIndex, DataCSVValidationModel csvValidation, Map<Integer, String> headerByIndex, URI experimentURI, ScientificObjectDAO scientificObjectDAO, Map<String, ScientificObjectModel> nameURIScientificObjects, List<String> scientificObjectsNotInXp, HashMap<URI, URI> mapVariableUriDataType,List<ImportDataIndex> duplicateDataByIndex) throws CSVDataTypeException, TimezoneAmbiguityException, TimezoneException {

        boolean validRow = true;
        ScientificObjectModel object = null;

        ParsedDateTimeMongo parsedDateTimeMongo = null;
        for (int colIndex = 0; colIndex < values.length; colIndex++) {
            if (colIndex == 0) {
                String objectNameOrUri = values[colIndex];
                // test in uri list
                if (!StringUtils.isEmpty(objectNameOrUri) && nameURIScientificObjects.containsKey(objectNameOrUri)) {
                    object = nameURIScientificObjects.get(objectNameOrUri);
                } else {
                    // test not in uri list
                    if (!StringUtils.isEmpty(objectNameOrUri) && !scientificObjectsNotInXp.contains(objectNameOrUri)) {
                        object = getObjectByNameOrURI(scientificObjectDAO, experimentURI, objectNameOrUri);
                    }
                    if (object == null) {
                        scientificObjectsNotInXp.add(objectNameOrUri);
                        CSVCell cell = new CSVCell(rowIndex, colIndex, objectNameOrUri, "OBJECT_ID");
                        csvValidation.addInvalidObjectError(cell);
                        validRow = false;
                        break;
                    } else {
                        nameURIScientificObjects.put(objectNameOrUri, object);
                    }
                }
            } else if (colIndex == 1) {
                // check date
                // TODO : Validate timezone ambiguity
                parsedDateTimeMongo = DataValidateUtils.setDataDateInfo(values[colIndex], null);
                if (parsedDateTimeMongo == null) {
                    CSVCell cell = new CSVCell(rowIndex, colIndex, values[colIndex], "DATE");
                    csvValidation.addInvalidDateError(cell);
                    validRow = false;
                    break;
                }
            } else {
                // If value is not blank and null
                if(!StringUtils.isEmpty(values[colIndex])){
                    
                    DataModel dataModel = new DataModel();
                    DataProvenanceModel provenanceModel = new DataProvenanceModel();
                    provenanceModel.setUri(provenance.getUri());
                    List<URI> experiments = new ArrayList<>();
                    experiments.add(experimentURI);
                    provenanceModel.setExperiments(experiments);
                    dataModel.setDate(parsedDateTimeMongo.getInstant());
                    dataModel.setOffset(parsedDateTimeMongo.getOffset());
                    dataModel.setIsDateTime(parsedDateTimeMongo.getIsDateTime());
                    List<URI> listObject = new ArrayList<>();
                    listObject.add(object.getUri());
                    dataModel.setScientificObjects(listObject);
                    dataModel.setProvenance(provenanceModel);
                    URI varURI = URI.create(headerByIndex.get(colIndex));
                    dataModel.setVariable(varURI);
                    dataModel.setValue(returnValidCSVDatum(varURI, values[colIndex].trim(), mapVariableUriDataType.get(varURI), rowIndex, colIndex, csvValidation));
                    csvValidation.addData(dataModel,rowIndex);
                    // check for duplicate data
                    ImportDataIndex importDataIndex = new ImportDataIndex(parsedDateTimeMongo.getInstant(),varURI,experimentURI,object.getUri());
                    if (!duplicateDataByIndex.contains(importDataIndex)) { 
                        duplicateDataByIndex.add(importDataIndex);
                    }else{
                        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';
                        CSVCell duplicateCell = new CSVCell(rowIndex, colIndex, values[colIndex].trim(), variableName);
                        csvValidation.addDuplicatedDataError(duplicateCell);
                    }    
                } 
            } 
        }
        return validRow;
    }

    private ScientificObjectModel getObjectByNameOrURI(ScientificObjectDAO scientificObjectDAO, URI contextUri, String nameOrUri) {
        ScientificObjectModel object = null;
        try {
            object = testNameOrURI(scientificObjectDAO, contextUri, nameOrUri);
        } catch (Exception ex) {
        }
        return object;
    }

    private ScientificObjectModel testNameOrURI(ScientificObjectDAO scientificObjectDAO, URI contextUri, String nameOrUri) throws Exception {
        ScientificObjectModel object;
        if (URIDeserializer.validateURI(nameOrUri)) {
            URI objectUri = URI.create(nameOrUri);

            object = scientificObjectDAO.getObjectByURI(objectUri, contextUri);
        } else {
            object = scientificObjectDAO.getByNameAndContext(nameOrUri, contextUri);
        }

        return object;
    }

    private Object returnValidCSVDatum(URI variable, Object value, URI dataType, int dataIndex, int colIndex, DataCSVValidationModel csvValidation) throws CSVDataTypeException {
        String variableName = csvValidation.getHeadersLabels().get(colIndex) + '(' + csvValidation.getHeaders().get(colIndex) + ')';

        if (Arrays.asList(NAN_VALUES).contains(value)) {
            value = NaN;
        } else if (Arrays.asList(NA_VALUES).contains(value)) {
            return null;
        }

        if (dataType == null) {
            return value;
        } else {
            if (dataType.toString().equals("xsd:integer")) {
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                    throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
                }
            }

        }
        if (dataType.toString().equals("xsd:decimal")) {
            try {
                return Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:boolean")) {
            Boolean toBooleanObject = BooleanUtils.toBooleanObject(value.toString());
            if (Objects.equals(toBooleanObject, Boolean.FALSE) || Objects.equals(toBooleanObject, Boolean.TRUE)) {
                return toBooleanObject;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }

        if (dataType.toString().equals("xsd:date")) {
            if (value instanceof String && DataValidateUtils.isDate(value.toString())) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:datetime")) {
            if (value instanceof String && DataValidateUtils.isDateTime(value.toString())) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }
        if (dataType.toString().equals("xsd:string")) {
            if ((value instanceof String)) {
                return value;
            } else {
                CSVCell errorCell = new CSVCell(dataIndex, colIndex, value.toString(), variableName);
                throw new CSVDataTypeException(variable, value, dataType, dataIndex, errorCell);
            }
        }

        return value;
    }
    
    @GET
    @Path("{uri}/provenances")
    @ApiOperation("Get provenances involved in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return data list", response = ProvenanceGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchExperimentProvenances(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name,
            @ApiParam(value = "Search by description") @QueryParam("description") String description,
            @ApiParam(value = "Search by activity URI") @QueryParam("activity") URI activityUri,
            @ApiParam(value = "Search by activity type") @QueryParam("activity_type") URI activityType,
            @ApiParam(value = "Search by agent URI") @QueryParam("agent") URI agentURI,
            @ApiParam(value = "Search by agent type") @QueryParam("agent_type") URI agentType,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        // test exp
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);
        
        DataDAO dataDAO = new DataDAO(nosql, sparql, fs);
        Set<URI> provenancesURIs = dataDAO.getProvenancesByExperiment(currentUser, xpUri);

        ListWithPagination<ProvenanceGetDTO> provenances = new ListWithPagination(new ArrayList<ProvenanceGetDTO>());
        if (!provenancesURIs.isEmpty()) {
            ProvenanceDAO dao = new ProvenanceDAO(nosql);
            ListWithPagination<ProvenanceModel> resultList = dao.search(provenancesURIs, name, description, activityType, activityUri, agentType, agentURI, orderByList, page, pageSize);

            provenances = resultList.convert(
                    ProvenanceGetDTO.class,
                    ProvenanceGetDTO::fromModel
            );

        }
        
        return new PaginatedListResponse<>(provenances).getResponse();
    }

}
