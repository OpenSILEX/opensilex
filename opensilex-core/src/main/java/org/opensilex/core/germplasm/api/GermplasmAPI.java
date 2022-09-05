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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.annotations.*;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Alice Boizet
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
    public static final String CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_GERMPLASM_DELETE_ID = "germplasm-delete";
    public static final String CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY = "credential.default.delete";

    public static final String GERMPLASM_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    public static final String GERMPLASM_EXAMPLE_TYPE = "http://www.opensilex.org/vocabulary/oeso#Variety";
    public static final String GERMPLASM_EXAMPLE_SPECIES = "http://www.phenome-fppn.fr/id/species/zeamays";
    protected static final String GERMPLASM_EXAMPLE_VARIETY = "";
    protected static final String GERMPLASM_EXAMPLE_ACCESSION = "";
    protected static final String GERMPLASM_EXAMPLE_INSTITUTE = "INRA";
    protected static final String GERMPLASM_EXAMPLE_PRODUCTION_YEAR = "2020";
    protected static final String GERMPLASM_EXAMPLE_METADATA = "{ \"water_stress\" : \"resistant\",\n" +
                                                        "\"yield\" : \"moderate\"}";

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
    private MongoDBService nosql;

    @CurrentUser
    UserModel currentUser;
    
    static final ConcurrentHashMap namesConcurrentMap = new ConcurrentHashMap();
    static final ConcurrentHashMap uriConcurrentMap = new ConcurrentHashMap();

    /**
     *
     * @param germplasmDTO
     * @param checkOnly
     * @return
     * @throws Exception
     */
    @POST
    @ApiOperation("Add a germplasm")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add a germplasm (variety, accession, plantMaterialLot)", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 409, message = "A germplasm with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmCreationDTO germplasmDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        String name = germplasmDTO.getName();
        URI uri = germplasmDTO.getUri();     
              
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        ErrorResponse error = check(germplasmDTO, germplasmDAO, false);
        if (error != null) {
            return error.getResponse();
        }

        if (!checkOnly) {
            try {
                namesConcurrentMap.merge(name, true, (v1, v2)-> {throw new IllegalArgumentException();});
            } catch (IllegalArgumentException e) {
                return new ErrorResponse(
                        Response.Status.CONFLICT,
                        "Germplasm name already exists",
                        "Duplicated name: " + name
                ).getResponse();
            }

            if (germplasmDTO.getUri() != null) {
                try {
                    uriConcurrentMap.merge(uri, true, (v1, v2)-> {throw new IllegalArgumentException();});
                } catch (IllegalArgumentException e) {
                    return new ErrorResponse(
                        Response.Status.CONFLICT,
                        "Germplasm URI already exists",
                        "Duplicated URI: " + uri
                    ).getResponse();
                }
            }

            try {
                germplasmDTO = completeDTO(germplasmDTO, germplasmDAO);
                // create new germplasm
                GermplasmModel model = germplasmDTO.newModel();
                GermplasmModel germplasm = germplasmDAO.create(model);
                return new ObjectUriResponse(Response.Status.CREATED, germplasm.getUri()).getResponse();
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            } finally {
                namesConcurrentMap.remove(name);
                if (uri != null) {
                    uriConcurrentMap.remove(uri);
                }  
            }

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
    @Path("{uri}")
    @ApiOperation("Get a germplasm")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm", response = GermplasmGetSingleDTO.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getGermplasm(
            @ApiParam(value = "germplasm URI", example = GERMPLASM_EXAMPLE_SPECIES, required = true) @PathParam("uri") @NotNull URI uri
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
            throw new NotFoundURIException("Germplasm URI not found : ", uri);
        }
    }

    /**
     * * Return a list of germplasms corresponding to the given URIs
     *
     * @param uris list of germplasms uri
     * @return Corresponding list of germplasms
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get a list of germplasms by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getGermplasmsByURI(
            @ApiParam(value = "Germplasms URIs", required = true) @QueryParam("uris") @NotNull @NotEmpty  List<URI> uris
    ) throws Exception {
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setLang(currentUser.getLanguage());
        filter.setUris(uris);

        List<GermplasmModel> models = germplasmDAO.search(filter,false).getList();

        if (!models.isEmpty()) {
            List<GermplasmGetAllDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(GermplasmGetAllDTO.fromModel(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Germplasm not found",
                    "Unknown germplasm URIs"
            ).getResponse();
        }
    }

    /**
     *
     * @param uri
     * @param name
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("{uri}/experiments")
    @ApiOperation("Get experiments where a germplasm has been used")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm", response = ExperimentGetListDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found", response = ErrorDTO.class)
    })
    public Response getGermplasmExperiments(
            @ApiParam(value = "germplasm URI", example = "dev-germplasm:g01", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Regex pattern for filtering experiments by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        ListWithPagination<ExperimentModel> experiments = germplasmDAO.getExpFromGermplasm(currentUser, uri, name, orderByList, page, pageSize);

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = experiments.convert(
                ExperimentGetListDTO.class,
                ExperimentGetListDTO::fromModel
        );

        // Return paginated list of exp DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    /**
     * Return all available germplasm attributes
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("attributes")
    @ApiOperation("Get attributes of all germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm attributes", response = ExperimentGetListDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Germplasm attributes not found", response = ErrorDTO.class)
    })
    public Response getGermplasmAttributes() throws Exception {
        // Get germplasm from DAO by URI
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        List<String> distinctAttributes = germplasmDAO.getDistinctGermplasAttributes();

        // Return list of
        return new SingleObjectResponse<>(distinctAttributes).getResponse();
    }

    /**
     *
     * @param uri
     * @param type
     * @param name
     * @param code
     * @param productionYear
     * @param species
     * @param variety
     * @param accession
     * @param institute
     * @param experiment
     * @param metadata
     * @param orderByList
     * @param page
     * @param pageSize
     * @return
     * @throws Exception
     */
    @GET
    @ApiOperation("Search germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return germplasm list", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response searchGermplasm(
            @ApiParam(value = "Regex pattern for filtering list by uri", example = GERMPLASM_EXAMPLE_URI) @QueryParam("uri") String uri,
            @ApiParam(value = "Search by type", example = GERMPLASM_EXAMPLE_TYPE) @QueryParam("rdf_type") URI type,
            @ApiParam(value = "Regex pattern for filtering list by name and synonyms", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "Regex pattern for filtering list by code", example = ".*") @DefaultValue(".*") @QueryParam("code") String code,
            @ApiParam(value = "Search by production year", example = GERMPLASM_EXAMPLE_PRODUCTION_YEAR) @QueryParam("production_year") Integer productionYear,
            @ApiParam(value = "Search by species", example = GERMPLASM_EXAMPLE_SPECIES) @QueryParam("species") URI species,
            @ApiParam(value = "Search by variety", example = GERMPLASM_EXAMPLE_VARIETY) @QueryParam("variety") URI variety,
            @ApiParam(value = "Search by accession", example = GERMPLASM_EXAMPLE_ACCESSION) @QueryParam("accession") URI accession,
            @ApiParam(value = "Search by institute", example = GERMPLASM_EXAMPLE_INSTITUTE) @QueryParam("institute") String institute,
            @ApiParam(value = "Search by experiment") @QueryParam("experiment") URI experiment,
            @ApiParam(value = "Search by metadata", example = GERMPLASM_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("label=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

         GermplasmSearchFilter searchFilter = new GermplasmSearchFilter()
                 .setUri(uri)
                 .setType(type)
                 .setName(name)
                 .setSpecies(species)
                 .setVariety(variety)
                 .setAccession(accession)
                 .setInstitute(institute)
                 .setProductionYear(productionYear)
                 .setExperiment(experiment)
                 .setMetadata(metadata);

         searchFilter.setOrderByList(orderByList)
                 .setPage(page)
                 .setPageSize(pageSize)
                 .setLang(currentUser.getLanguage());

        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);

        ListWithPagination<GermplasmModel> resultList = dao.search(
               searchFilter,false
        );

        // Convert paginated list to DTO
        ListWithPagination<GermplasmGetAllDTO> resultDTOList = resultList.convert(
                GermplasmGetAllDTO.class,
                GermplasmGetAllDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @POST
    @Path("export")
    @ApiOperation("export germplasm")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with germplasm list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportGermplasm(
            @ApiParam("CSV export configuration") @Valid GermplasmSearchFilter searchFilter
    ) throws Exception {

        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);
        List<GermplasmModel> resultList = dao.search(searchFilter,true).getList();

        return buildCSV(resultList);
    }
    
    private Response buildCSV(List<GermplasmModel> germplasmList) throws IOException {
                // Convert list to DTO
        List<GermplasmGetSingleDTO> resultDTOList = new ArrayList<>();
        Set<String> metadataKeys = new HashSet<>();
        for (GermplasmModel germplasm : germplasmList) {
            GermplasmGetSingleDTO dto = GermplasmGetSingleDTO.fromModel(germplasm);
            resultDTOList.add(dto);
            Map<String,String> metadata = dto.getMetadata();
            if (metadata != null) {
                 metadataKeys.addAll(metadata.keySet());
            }           
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new GermplasmGetSingleDTO()); // to return an empty table
        }
        
        //Construct manually json with metadata to convert it to csv
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);

        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {
                ObjectNode objectNode = jsonNode.deepCopy();
                JsonNode metadata = objectNode.get("metadata");
                objectNode.remove("metadata");
                JsonNode value = null;
                for (Object key:metadataKeys) {
                    try {
                        value = metadata.get(key.toString());

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (value != null) {
                            objectNode.put(key.toString(), value.asText());
                        } else {
                            objectNode.putNull(key.toString());
                        }                            
                    }
                }
                
                list.add(objectNode);
               
            }
         }
        
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance, list);

        Builder csvSchemaBuilder = CsvSchema.builder();
        JsonNode firstObject = arrayNode.elements().next();
        firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        StringWriter str = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class)
                .with(csvSchema)
                .writeValue(str, arrayNode);

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_germplasm" + dtf.format(date) + ".csv";

        return Response.ok(str.toString(), MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }


    /**
     * @param germplasmDTO the germplasm to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated germplasm {@link URI}
     */
    @PUT
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
        @ApiResponse(code = 400, message = "Invalid or unknown Germplasm URI", response = ErrorResponse.class)})
    public Response updateGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmUpdateDTO germplasmDTO
    ) throws Exception {
        try {
            GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
            ErrorResponse error = check(germplasmDTO, germplasmDAO, true);
            if (error != null) {
                return error.getResponse();
            }

            germplasmDTO = completeDTO(germplasmDTO, germplasmDAO);

            GermplasmModel model = germplasmDTO.newModel();
            germplasmDAO.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown Germplasm URI ", germplasmDTO.getUri());
        }
    }

    @DELETE
    @Path("{uri}")
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
            throw new NotFoundURIException("Invalid or unknown Germplasm URI ", uri);
        }
        
        if (dao.isLinkedToSth(germplasmToDelete)) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The germplasm is linked to another germplasm or a scientific object or an experiment",
                    "You can't delete a germplasm linked to another object"
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
                        "Duplicated URI: " + germplasmDTO.getUri(),
                        "component.germplasms.errors.duplicateUri",
                        new HashMap<String, String>() {{
                            put("uri", germplasmDTO.getUri().toString());
                        }}
                );
            }

            // check if germplasm label already exists
            boolean exists = germplasmDAO.labelExistsCaseSensitiveBySpecies(germplasmDTO);
            //boolean exists = germplasmDAO.labelExistsCaseInsensitive(germplasmDTO.getLabel(),germplasmDTO.getRdfType());
            if (exists) {
                // Return error response 409 - CONFLICT if label already exists
                return new ErrorResponse(
                        Response.Status.CONFLICT,
                        "Germplasm label already exists for this species",
                        "Duplicated label: " + germplasmDTO.getName(),
                        "component.germplasms.errors.duplicateLabel",
                        new HashMap<String, String>() {{
                            put("label", germplasmDTO.getName());
                        }}
                );
            }
        }

        // check rdfType
        boolean isType = cacheType.get(new KeyType(germplasmDTO.getRdfType()), this::checkType);
        //boolean isType = germplasmDAO.isGermplasmType(germplasmDTO.getRdfType()); 
        if (!isType) {
            // Return error response 409 - CONFLICT if rdfType doesn't exist in the ontology
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "rdfType doesn't exist in the ontology",
                    "wrong rdfType: " + germplasmDTO.getRdfType().toString(),
                    "component.germplasms.errors.wrongRdfType",
                    new HashMap<String, String>() {{
                        put("rdfType", germplasmDTO.getRdfType().toString());
                    }}
            );
        }

        //Check that the given fromAccession, fromVariety or fromSpecies exist in DB
        if (germplasmDTO.getSpecies() != null) {
            if (!sparql.uriExists(new URI(Oeso.Species.getURI()), germplasmDTO.getSpecies())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given species doesn't exist in the database",
                        "unknown species : " + germplasmDTO.getSpecies().toString(),
                        "component.germplasms.errors.unknownSpecies",
                        new HashMap<String, String>() {{
                            put("unknownSpecies", germplasmDTO.getSpecies().toString());
                        }}
                );
            }
        }

        if (germplasmDTO.getVariety() != null) {
            if (!sparql.uriExists(new URI(Oeso.Variety.getURI()), germplasmDTO.getVariety())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given variety doesn't exist in the database",
                        "unknown variety : " + germplasmDTO.getVariety().toString(),
                        "component.germplasms.errors.unknownVariety",
                        new HashMap<String, String>() {{
                            put("unknownVariety", germplasmDTO.getVariety().toString());
                        }}
                );
            }
        }

        if (germplasmDTO.getAccession() != null) {
            if (!sparql.uriExists(new URI(Oeso.Accession.getURI()), germplasmDTO.getAccession())) {
                return new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "The given accession doesn't exist in the database",
                        "unknown accession : " + germplasmDTO.getAccession().toString(),
                        "component.germplasms.errors.unknownAccession",
                        new HashMap<String, String>() {{
                            put("unknownAccession", germplasmDTO.getAccession().toString());
                        }}
                );
            }
        }

        // check that fromAccession, fromVariety or fromSpecies are given
        boolean missingLink = true;
        String message = new String();
        if (SPARQLDeserializers.compareURIs(germplasmDTO.getRdfType().toString(), Oeso.Species.getURI())) {
            missingLink = false;
        } else if (SPARQLDeserializers.compareURIs(germplasmDTO.getRdfType().toString(), Oeso.Variety.getURI())) {
            message = "species";
            if (germplasmDTO.getSpecies() != null) {
                missingLink = false;
            }
        } else if (SPARQLDeserializers.compareURIs(germplasmDTO.getRdfType().toString(), Oeso.Accession.getURI())) {
            message = "variety or species";
            if (germplasmDTO.getSpecies() != null || germplasmDTO.getVariety() != null) {
                missingLink = false;
            }
        } else {
            message = "accession, variety or species";
            if (germplasmDTO.getSpecies() != null || germplasmDTO.getVariety() != null || germplasmDTO.getAccession() != null) {
                missingLink = false;
            }
        }

        if (missingLink) {
            final String finalMessage = message;
            // Return error response 409 - CONFLICT if link fromSpecies, fromVariety or fromAccession is missing
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "missing attribute",
                    "you have to fill " + finalMessage,
                    "component.germplasms.errors.missingAttribute",
                    new HashMap<String, String>() {{
                        put("message", finalMessage);
                    }}
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

    private <T extends GermplasmCreationDTO> T completeDTO(T germplasmDTO, GermplasmDAO germplasmDAO) throws Exception {
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
