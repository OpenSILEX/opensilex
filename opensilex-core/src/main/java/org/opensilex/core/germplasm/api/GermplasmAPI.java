/*
 * ******************************************************************************
 *                                     GermplasmAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  23 March, 2020
 *  Contact: alice.boizet@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.service.NoSQLService;
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
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author boizetal
 */
@Api(GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_ID)
@Path("/core/germplasm")
@ApiCredentialGroup(
        groupId = GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_ID,
        groupLabelKey = GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY
)

public class GermplasmAPI {

    public static final String CREDENTIAL_GERMPLASM_GROUP_ID = "Germplasm";
    public static final String CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.germplasm";

    public static final String CREDENTIAL_GERMPLASM_MODIFICATION_ID = "germplasm-modification";
    public static final String CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY = "credential.germplasm.modification";

    public static final String CREDENTIAL_GERMPLASM_DELETE_ID = "germplasm-delete";
    public static final String CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY = "credential.germplasm.delete";

    protected final String GERMPLASM_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    protected static final String GERMPLASM_EXAMPLE_TYPE = "http://www.opensilex.org/vocabulary/oeso#Variety";
    protected static final String GERMPLASM_EXAMPLE_SPECIES = "http://www.phenome-fppn.fr/id/species/zeamays";
    protected static final String GERMPLASM_EXAMPLE_VARIETY = "";
    protected static final String GERMPLASM_EXAMPLE_ACCESSION = "";
    protected static final String GERMPLASM_EXAMPLE_INSTITUTE = "INRA";
    protected static final String GERMPLASM_EXAMPLE_PRODUCTION_YEAR = "2020";

    private static final Cache<Key, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<KeyType, Boolean> cacheType = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final Cache<KeyGermplasm, GermplasmModel> cacheGermplasm = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Inject
    private SPARQLService sparql;

    @Inject
    NoSQLService nosql;

    @CurrentUser
    UserModel currentUser;

    /**
     *
     * @param germplasmDTO
     * @return
     * @throws Exception
     */
    @POST
    @Path("create")
    @ApiOperation("Create a germplasm")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a germplasm (variety, accession, plantMaterialLot)", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 409, message = "A germplasm with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmCreationDTO germplasmDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {

        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        ErrorResponse error = check(germplasmDTO, germplasmDAO, false);
        if (error != null) {
            return error.getResponse();
        }

        if (!checkOnly) {
            germplasmDTO = completeDTO(germplasmDTO, germplasmDAO);
            // create new germplasm
            GermplasmModel model = germplasmDTO.newModel();
            GermplasmModel germplasm = germplasmDAO.create(model, currentUser);
            //return germplasm uri

            return new ObjectUriResponse(Response.Status.CREATED, germplasm.getUri()).getResponse();
        } else {
            return new ObjectUriResponse().getResponse();
        }
    }

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a germplasm by its URI")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile", response = GermplasmGetSingleDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getGermplasm(
            @ApiParam(value = "germplasm URI", example = "dev-users:Admin_OpenSilex", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        GermplasmModel model = germplasmDAO.get(uri, currentUser);

        // Check if germplasm is found
        if (model != null) {
            // Return GermplasmGetDTO
            return new SingleObjectResponse<>(
                    GermplasmGetSingleDTO.fromModel(model)
            ).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Germplasm not found",
                    "Unknown germplasm URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     *
     * @param uri
     * @return
     */
    @GET
    @Path("get/{uri}/experiments")
    @ApiOperation("Get lists of experiments where the germplasm has been used")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile", response = ExperimentGetListDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getGermplasmExperiments(
            @ApiParam(value = "germplasm URI", example = "dev-germplasm:g01", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        ListWithPagination<ExperimentModel> experiments = germplasmDAO.getExpFromGermplasm(currentUser, uri, orderByList, page, pageSize);
        // TODO: filter experiments with those accessible to current user

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = experiments.convert(
                ExperimentGetListDTO.class,
                ExperimentGetListDTO::fromModel
        );

        // Return paginated list of exp DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     *
     * @param uri
     * @return
     * @throws Exception
     */
    @GET
    @Path("get/{experimentUri}/germplasmList")
    @ApiOperation("Get lists of experiments where the germplasm has been used")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return profile", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getExpGermplasm(
            @ApiParam(value = "experiment URI", example = "", required = true) @PathParam("experimentUri") @NotNull URI uri,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        ListWithPagination<GermplasmModel> germplasmList = germplasmDAO.getGermplasmFromExp(currentUser, uri, orderByList, page, pageSize);

// Convert paginated list to DTO
        ListWithPagination<GermplasmGetSingleDTO> resultDTOList = germplasmList.convert(
                GermplasmGetSingleDTO.class,
                GermplasmGetSingleDTO::fromModel
        );

        // Return paginated list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     *
     * @param uri
     * @param rdfType
     * @param label
     * @param species
     * @param variety
     * @param accession
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    @GET
    @Path("search")
    @ApiOperation("Search germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm list", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchGermplasmList(
            @ApiParam(value = "Search by uri", example = GERMPLASM_EXAMPLE_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by type", example = GERMPLASM_EXAMPLE_TYPE) @QueryParam("type") URI type,
            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "Search by species", example = GERMPLASM_EXAMPLE_SPECIES) @QueryParam("species") URI species,
            @ApiParam(value = "Search by variety", example = GERMPLASM_EXAMPLE_VARIETY) @QueryParam("variety") URI variety,
            @ApiParam(value = "Search by accession", example = GERMPLASM_EXAMPLE_ACCESSION) @QueryParam("accession") URI accession,
            @ApiParam(value = "Search by institute", example = GERMPLASM_EXAMPLE_INSTITUTE) @QueryParam("institute") String institute,
            @ApiParam(value = "Search by productionYear", example = GERMPLASM_EXAMPLE_PRODUCTION_YEAR) @QueryParam("productionYear") Integer productionYear,
            @ApiParam(value = "Search by experiment") @QueryParam("experiment") URI experiment,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Search germplasm with germplasm DAO
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        ListWithPagination<GermplasmModel> resultList = dao.search(
                currentUser,
                uri,
                type,
                name,
                species,
                variety,
                accession,
                institute,
                productionYear,
                experiment,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<GermplasmGetAllDTO> resultDTOList = resultList.convert(
                GermplasmGetAllDTO.class,
                GermplasmGetAllDTO::fromModel
        );

        // Return paginated list of profiles DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     *
     * @param germplasmSearchDTO
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    @POST
    @Path("search")
    @ApiOperation("Search germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm list", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchGermplasm(
            @ApiParam("Germplasm search form") GermplasmSearchDTO germplasmSearchDTO,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        // Search germplasm with germplasm DAO
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        ListWithPagination<GermplasmModel> resultList = dao.search(
                currentUser,
                germplasmSearchDTO.getUri(),
                germplasmSearchDTO.getType(),
                germplasmSearchDTO.getName(),
                germplasmSearchDTO.getFromSpecies(),
                germplasmSearchDTO.getFromVariety(),
                germplasmSearchDTO.getFromAccession(),
                germplasmSearchDTO.getInstitute(),
                germplasmSearchDTO.getProductionYear(),
                germplasmSearchDTO.getExperiment(),
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<GermplasmGetAllDTO> resultDTOList = resultList.convert(
                GermplasmGetAllDTO.class,
                GermplasmGetAllDTO::fromModel
        );

        // Return paginated list of factor DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    /**
     *
     * @param germplasmSearchDTO
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    @POST
    @Path("exportCSV")
    @ApiOperation("export germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with germplasm list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportGermplasm(
            @ApiParam("Germplasm search form") GermplasmSearchDTO germplasmSearchDTO
            ) throws Exception {

        // Search germplasm with germplasm DAO
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        List<GermplasmModel> resultList = dao.searchForExport(
                currentUser,
                germplasmSearchDTO.getUri(),
                germplasmSearchDTO.getType(),
                germplasmSearchDTO.getName(),
                germplasmSearchDTO.getFromSpecies(),
                germplasmSearchDTO.getFromVariety(),
                germplasmSearchDTO.getFromAccession(),
                germplasmSearchDTO.getInstitute(),
                germplasmSearchDTO.getProductionYear(),
                germplasmSearchDTO.getExperiment()
        );

        // Convert list to DTO
        List<GermplasmGetExportDTO> resultDTOList = new ArrayList<>();
        for (GermplasmModel germplasm:resultList) {
            GermplasmGetExportDTO dto = GermplasmGetExportDTO.fromModel(germplasm);
            resultDTOList.add(dto);
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);
        
        Builder csvSchemaBuilder = CsvSchema.builder();
	JsonNode firstObject = jsonTree.elements().next();
        firstObject.fieldNames().forEachRemaining(fieldName -> {csvSchemaBuilder.addColumn(fieldName);} );
	CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        StringWriter str = new StringWriter();
        
        CsvMapper csvMapper = new CsvMapper();
	csvMapper.writerFor(JsonNode.class)
	  .with(csvSchema)
	  .writeValue(str, jsonTree);
        
        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_germplasm" + dtf.format(date);

        return Response.ok(str.toString(), MediaType.TEXT_PLAIN_TYPE)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
        
    }

    /**
     * @param germplasmDTO the germplasm to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated germplasm {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update a germplasm")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Germplasm updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Germplasm URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response updateGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmCreationDTO germplasmDTO
    ) {
        try {
            GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
            if (SPARQLDeserializers.compareURIs(germplasmDTO.getType().toString(), Oeso.Species.getURI())) {
                return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The germplasm is a species",
                    "You can't update a species"
                ).getResponse();
            }
            ErrorResponse error = check(germplasmDTO, germplasmDAO, true);
            if (error != null) {
                return error.getResponse();
            }

            germplasmDTO = completeDTO(germplasmDTO, germplasmDAO);

            GermplasmModel model = germplasmDTO.newModel();
            germplasmDAO.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Experiment URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a germplasm")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GERMPLASM_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGermplasm(
            @ApiParam(value = "Germplasm URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);

        //check
        GermplasmModel germplasmToDelete = dao.get(uri, currentUser);

        if (germplasmToDelete == null) {
            throw new SPARQLInvalidURIException(uri);
        }

        boolean linkedGermplasm;

        if (SPARQLDeserializers.compareURIs(germplasmToDelete.getType().toString(), Oeso.Species.getURI())) {
            linkedGermplasm = dao.hasRelation(uri, Oeso.fromSpecies);
        } else if (SPARQLDeserializers.compareURIs(germplasmToDelete.getType().toString(), Oeso.Variety.getURI())) {
            linkedGermplasm = dao.hasRelation(uri, Oeso.fromVariety);
        } else if (SPARQLDeserializers.compareURIs(germplasmToDelete.getType().toString(), Oeso.Accession.getURI())) {
            linkedGermplasm = dao.hasRelation(uri, Oeso.fromAccession);
        } else {
            linkedGermplasm = false;
        }

        if (linkedGermplasm) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The germplasm is linked to another germplasm",
                    "You can't delete a germplasm linked to another one"
            ).getResponse();
        } else {
            dao.delete(uri);
            return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
        }

    }

    private ErrorResponse check(GermplasmCreationDTO germplasmDTO, GermplasmDAO germplasmDAO, boolean update) throws Exception {

        if (!update) {
            // check if germplasm URI already exists
            if (sparql.uriExists(GermplasmModel.class, germplasmDTO.getUri())) {
                // Return error response 409 - CONFLICT if URI already exists
                return new ErrorResponse(
                        Response.Status.CONFLICT,
                        "Germplasm URI already exists",
                        "Duplicated URI: " + germplasmDTO.getUri()
                );
            }

            // check if germplasm label already exists
            boolean exists = germplasmDAO.labelExistsCaseSensitiveBySpecies(germplasmDTO);
            //boolean exists = germplasmDAO.labelExistsCaseInsensitive(germplasmDTO.getLabel(),germplasmDTO.getRdfType());
            if (exists) {
                // Return error response 409 - CONFLICT if label already exists
                return new ErrorResponse(
                        Response.Status.PRECONDITION_FAILED,
                        "Germplasm label already exists for this species",
                        "Duplicated label: " + germplasmDTO.getName()
                );
            }
        }

        // check rdfType
        boolean isType = cacheType.get(new KeyType(germplasmDTO.getType()), this::checkType);
        //boolean isType = germplasmDAO.isGermplasmType(germplasmDTO.getRdfType()); 
        if (!isType) {
            // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "wrong rdfType: " + germplasmDTO.getType().toString()
            );
        }
        
        //Check that the given fromAccession, fromVariety or fromSpecies exist in DB
        if (germplasmDTO.getSpecies() != null) {
           if (!sparql.uriExists(new URI(Oeso.Species.getURI()), germplasmDTO.getSpecies())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given species doesn't exist in the database",
                        "unknown species : " + germplasmDTO.getSpecies().toString()
                );
            } 
        }
         
        if (germplasmDTO.getVariety() != null) {
            if (!sparql.uriExists(new URI(Oeso.Variety.getURI()), germplasmDTO.getVariety())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given variety doesn't exist in the database",
                        "unknown variety : " + germplasmDTO.getVariety().toString()
                );
            }
        }
        
        if (germplasmDTO.getAccession() != null) {
            if (!sparql.uriExists(new URI(Oeso.Accession.getURI()), germplasmDTO.getAccession())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given accession doesn't exist in the database",
                        "unknown accession : " + germplasmDTO.getAccession().toString()
                );
            }        
        }
        
        // check that fromAccession, fromVariety or fromSpecies are given
        boolean missingLink = true;
        String message = new String();
        if (SPARQLDeserializers.compareURIs(germplasmDTO.getType().toString(), Oeso.Species.getURI())) {
            missingLink = false;
        } else if (SPARQLDeserializers.compareURIs(germplasmDTO.getType().toString(), Oeso.Variety.getURI())) {
            message = "fromSpecies";
            if (germplasmDTO.getSpecies() != null) {
                missingLink = false;               
            }
        } else if (SPARQLDeserializers.compareURIs(germplasmDTO.getType().toString(), Oeso.Accession.getURI())) {
            message = "fromVariety or fromSpecies";
            if (germplasmDTO.getSpecies() != null || germplasmDTO.getVariety() != null) {
                missingLink = false;
            }
        } else {
            message = "fromAccession, fromVariety or fromSpecies";
            if (germplasmDTO.getSpecies() != null || germplasmDTO.getVariety() != null || germplasmDTO.getAccession() != null) {
                missingLink = false;                
            }
        }

        if (missingLink) {
            // Return error response 409 - CONFLICT if link fromSpecies, fromVariety or fromAccession is missing
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "missing attribute",
                    "you have to fill " + message
            );
        }

        // check coherence between species, variety and accession
        boolean isRelated;
        if (germplasmDTO.getSpecies() != null && germplasmDTO.getVariety() != null) {
            //check coherence between variety and species
            isRelated = cache.get(new Key(germplasmDTO), this::checkVarietySpecies);
            //isRelated = checkVarietySpecies(germplasmDTO.getSpecies(), germplasmDTO.getFromVariety());
            if (!isRelated) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given species doesn't match with the given variety",
                        "wrong species : " + germplasmDTO.getSpecies().toString()
                );
            }

        }

        if (germplasmDTO.getSpecies() != null && germplasmDTO.getAccession() != null) {
            //check coherence between accession and species
            isRelated = cache.get(new Key(germplasmDTO), this::checkAccessionSpecies);
            if (!isRelated) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given species doesn't match with the given variety",
                        "wrong species : " + germplasmDTO.getSpecies().toString()
                );
            }
        }

        if (germplasmDTO.getVariety() != null && germplasmDTO.getAccession() != null) {
            //check coherence between variety and accession
            isRelated = cache.get(new Key(germplasmDTO), this::checkAccessionVariety);
            if (!isRelated) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given species doesn't match with the given variety",
                        "wrong species : " + germplasmDTO.getSpecies().toString()
                );
            }
        }

        return null;
    }

    private GermplasmCreationDTO completeDTO(GermplasmCreationDTO germplasmDTO, GermplasmDAO germplasmDAO) throws Exception {
        if (germplasmDTO.getSpecies() == null && germplasmDTO.getVariety() != null) {
            GermplasmModel variety = cacheGermplasm.get(new KeyGermplasm(germplasmDTO.getVariety()), this::getGermplasm);
            if (variety != null) {
                germplasmDTO.setSpecies(variety.getSpecies().getUri());
            }

        }
        if (germplasmDTO.getAccession() != null && germplasmDTO.getVariety() == null && germplasmDTO.getSpecies() == null) {
            GermplasmModel accession = cacheGermplasm.get(new KeyGermplasm(germplasmDTO.getAccession()), this::getGermplasm);
            if (accession != null) {
                germplasmDTO.setVariety(accession.getVariety().getUri());
                germplasmDTO.setSpecies(accession.getSpecies().getUri());
            }
        }
        return germplasmDTO;
    }

    private boolean checkVarietySpecies(Key key) {
        return checkVarietySpecies(key.species, key.variety);
    }

    public boolean checkVarietySpecies(URI speciesURI, URI varietyURI) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        try {
            GermplasmModel variety = dao.get(varietyURI, currentUser);
            if (SPARQLDeserializers.compareURIs(variety.getSpecies().getUri().toString(), speciesURI.toString())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return true; //the variety doesn't exist in the database yet
        }
    }

    private boolean checkAccessionSpecies(Key key) {
        return checkAccessionSpecies(key.species, key.variety);
    }

    private boolean checkAccessionSpecies(URI speciesURI, URI accessionURI) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        try {
            GermplasmModel accession = dao.get(accessionURI, currentUser);
            if (SPARQLDeserializers.compareURIs(accession.getSpecies().getUri().toString(), speciesURI.toString())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }

    private boolean checkAccessionVariety(Key key) {
        return checkAccessionVariety(key.species, key.variety);
    }

    private boolean checkAccessionVariety(URI varietyURI, URI accessionURI) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        try {
            GermplasmModel accession = dao.get(accessionURI, currentUser);
            if (SPARQLDeserializers.compareURIs(accession.getVariety().getUri().toString(), varietyURI.toString())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return true; //the accession doesn't exist in the database yet
        }
    }

    private static final class Key {

        final URI species;
        final URI variety;

        public Key(URI species, URI variety) {
            this.species = species;
            this.variety = variety;
        }

        private Key(GermplasmCreationDTO germplasmDTO) {
            species = germplasmDTO.getSpecies();
            variety = germplasmDTO.getVariety();
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(this.species));
            hash = 97 * hash + Objects.hashCode(SPARQLDeserializers.formatURI(this.variety));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if (!SPARQLDeserializers.compareURIs(this.species.toString(), other.species.toString())) {
                return false;
            }
            if (!SPARQLDeserializers.compareURIs(this.variety.toString(), other.variety.toString())) {
                return false;
            }
            return true;
        }

    }

    private static final class KeyType {

        final URI type;

        public KeyType(URI type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KeyType other = (KeyType) obj;
            if (!SPARQLDeserializers.compareURIs(this.type.toString(), other.type.toString())) {
                return false;
            }
            return true;
        }
    }

    private boolean checkType(KeyType key) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        try {
            return dao.isGermplasmType(key.type);
        } catch (Exception e) {
            return false;
        }
    }

    private static class KeyGermplasm {

        final URI uri;

        public KeyGermplasm(URI uri) {
            this.uri = uri;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Objects.hashCode(this.uri);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final KeyGermplasm other = (KeyGermplasm) obj;
            if (!SPARQLDeserializers.compareURIs(this.uri.toString(), other.uri.toString())) {
                return false;
            }
            return true;
        }
    }

    private GermplasmModel getGermplasm(KeyGermplasm key) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        try {
            GermplasmModel germplasm = dao.get(key.uri, currentUser);
            return germplasm;
        } catch (Exception e) {
            return null;
        }
    }
}
