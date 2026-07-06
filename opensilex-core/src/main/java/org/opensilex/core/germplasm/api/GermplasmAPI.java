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
import io.swagger.annotations.*;
import org.apache.commons.collections.CollectionUtils;
import org.opensilex.core.utils.StringURIsListDTO;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.bll.GermplasmLogic;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.group.dal.GroupDAO;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.multipleError.MultipleCreateUpdateErrorObject;
import org.opensilex.server.exceptions.multipleError.MultipleErrorListException;
import org.opensilex.server.exceptions.multipleError.MultipleErrorObject;
import org.opensilex.server.exceptions.multipleError.MultipleErrorObjectList;
import org.opensilex.server.response.*;
import org.opensilex.server.response.multipleError.MultipleErrorResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.response.CreatedUriResponse;
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
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    protected static final String GERMPLASM_EXAMPLE_VARIETY = "http://opensilex.test/id/germplasm/variety.huachano";
    protected static final String GERMPLASM_EXAMPLE_ACCESSION = "http://opensilex.test/id/germplasm/accession.v_a_x_v_b";
    protected static final String GERMPLASM_EXAMPLE_INSTITUTE = "INRA";
    protected static final String GERMPLASM_EXAMPLE_PRODUCTION_YEAR = "2020";
    protected static final String GERMPLASM_EXAMPLE_METADATA = "{ \"water_stress\" : \"resistant\",\n" +
                                                        "\"yield\" : \"moderate\"}";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBServiceV2 nosql;

    @CurrentUser
    AccountModel currentUser;

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
        @ApiResponse(code = 201, message = "Add a germplasm (variety, accession, plantMaterialLot)", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})

    public Response createGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmCreationDTO germplasmDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        GermplasmLogic germplasmBusiness= new GermplasmLogic(sparql, nosql, currentUser);
        GermplasmModel model = germplasmDTO.newModel(sparql, currentUser.getLanguage(), null);
        model.setPublisher(currentUser.getUri());

        if (!checkOnly) {

            try {
                GermplasmModel germplasm = germplasmBusiness.create(model);
                return new CreatedUriResponse(germplasm.getUri()).getResponse();
            } catch ( WebApplicationException exception){
                throw exception;
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }

        } else {
            //raise a Displayable Exception if the germplasm already exists or is incorrect
            germplasmBusiness.checkBeforeCreateOrUpdate(Collections.singletonList(model), false);
            return new ObjectUriResponse().getResponse();
        }

    }

    /**
     *  create and/or update many germplasms if everything is correct, block everything if there is an error.
     *  JSon validation id disabled to handle manually errors like uri format, it allows to return errors as multiple errors.
     *  Errors are returned with the index of the germplasm in the list as key.
     * @param germplasmDTOs
     * @param checkOnly
     * @return
     * @throws Exception
     */
    @POST
    @Path("import")
    @ApiOperation("Add or update many germplasms")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "germplasms are created and/or updated", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})

    public Response upsertGermplasms(
            @ApiParam("List of germplasm description") List<GermplasmCreationDTO> germplasmDTOs,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        GermplasmLogic germplasmBusiness= new GermplasmLogic(sparql, nosql, currentUser);
        List<GermplasmModel> models;
        MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> relationsErrors = null;

        try {
            models = getGermplasmModelsAndValidateRelations(germplasmDTOs);
        } catch (InvalidValueException e) {
            relationsErrors = new MultipleErrorObjectList<>("germplasms errors", new ArrayList<>(), MultipleCreateUpdateErrorObject::new);
            models = getGermplasmModelWithoutRelationsAndCollectErrors(germplasmDTOs, relationsErrors);
        }

        if (!checkOnly) {

            try {
                List<GermplasmModel> germplasms = germplasmBusiness.upsert(models);
                List<URI> uris = germplasms.stream().map(GermplasmModel::getUri).toList();
                return new CreatedUriResponse(uris).getResponse();
            } catch (MultipleErrorListException exception){
                if (relationsErrors != null && relationsErrors.hasErrors()) {
                    exception.getMultipleErrorObjectList().mergeErrors(relationsErrors);
                }
                return exception.getResponse();
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }

        } else {
            var multipleErrorObjectList = germplasmBusiness.checkBeforeCreateOrUpdate(models, true);

            if (relationsErrors != null && relationsErrors.hasErrors()) {
                multipleErrorObjectList.mergeErrors(relationsErrors);
            }

            if (multipleErrorObjectList.hasErrors()) {
                return new MultipleErrorResponse(multipleErrorObjectList.toDTO()).getResponse();
            }
            return new ObjectUriResponse().getResponse();
        }

    }

    /**
     *  get models from DTOs and validate relations.
     *  This method validates relations of each germplasmDTO. It would be better to validate relations in the business layer, but this would require refactoring.
     * @throws MultipleErrorListException
     */
    private List<GermplasmModel> getGermplasmModelsAndValidateRelations(List<GermplasmCreationDTO> germplasmDTOs) throws SPARQLException {
        //this object allow to catch all uri syntax error and return them all in once.
        MultipleErrorObjectList<MultipleErrorObject, GermplasmCreationDTO> errors = new MultipleErrorObjectList<>(
                "some uris cannot be parsed due to syntax error",
                germplasmDTOs,
                MultipleErrorObject::new
        );

        List<GermplasmModel> models = new ArrayList<>();
        if (CollectionUtils.isEmpty(germplasmDTOs)) {
            return models;
        }

        Map<URI, ClassModel> classModels = new HashMap<>();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        //get every different types from DTOs
        List<URI> germplasmsTypes = germplasmDTOs.stream().map(GermplasmCreationDTO::getRdfType).distinct().toList();
        for (URI type : germplasmsTypes) {
            classModels.put(type, ontologyDAO.getClassModel(type, URI.create(Oeso.Germplasm.getURI()), currentUser.getLanguage()));
        }

        for (GermplasmCreationDTO germplasmDTO : germplasmDTOs) {
            try {
                models.add(germplasmDTO.newModel(sparql, currentUser.getLanguage(), classModels.get(germplasmDTO.getRdfType())));
            } catch ( URISyntaxException e ) {
                errors.addError(germplasmDTO, e.getMessage());
            }
        }

        if (errors.hasErrors()) {
            throw new MultipleErrorListException(errors.toDTO().title, errors);
        }

        return models;
    }

    /**
     * This method is used to handle germplasms errors on relations. It would be better to handle relations in the business layer, but this would require refactoring.
     * return germplasms. For germplasms with relations errors, we return the germplasm model without relations and collect errors in the errors list.
     * This method fill the errors list with germplasm models and errors.
     * @param errors should be initialized with an empty list of models. Models will be added to the list by this method.
     * @return a list of germplasm models without relations.
     */
    private List<GermplasmModel> getGermplasmModelWithoutRelationsAndCollectErrors(List<GermplasmCreationDTO> germplasmDTOs, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GermplasmModel> errors) throws URISyntaxException, SPARQLException {
        List<GermplasmModel> models = new ArrayList<>();
        if (CollectionUtils.isEmpty(germplasmDTOs)) {
            return models;
        }

        Map<URI, ClassModel> classModels = new HashMap<>();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        //get every different types from DTOs
        List<URI> germplasmsTypes = germplasmDTOs.stream().map(GermplasmCreationDTO::getRdfType).distinct().toList();
        for (URI type : germplasmsTypes) {
            classModels.put(type, ontologyDAO.getClassModel(type, new URI(Oeso.Germplasm.getURI()), currentUser.getLanguage()));
        }

        for (GermplasmCreationDTO germplasmDTO : germplasmDTOs) {
            GermplasmModel model = null;
            try {
                model = germplasmDTO.newModel(sparql, currentUser.getLanguage(), classModels.get(germplasmDTO.getRdfType()));
                errors.addModel(model);
            } catch (InvalidValueException e){
                model = germplasmDTO.newModelWithoutRelations();
                errors.addModel(model);
                errors.addError(model, e.getErrorMessage());
            } finally {
                models.add(model);
            }
        }

        return models;

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
            @ApiParam(value = "germplasm URI", example = GERMPLASM_EXAMPLE_SPECIES, required = true) @PathParam("uri") @ValidURI @NotNull URI uri
    ) throws Exception {
        GermplasmModel model = new GermplasmLogic(sparql, nosql, currentUser).get(uri, true);

        if (model != null) {
            // Return GermplasmGetDTO
            GermplasmGetSingleDTO dto = GermplasmGetSingleDTO.fromModel(model);
            if (Objects.nonNull(model.getPublisher())) {
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException("Germplasm URI not found : ", uri);
        }
    }

    /**
     * @return  a list of germplasms corresponding to the given URIs
     *
     * @param uris list of germplasms uri
     */
    @POST
    @Path("by_uris")
    @ApiOperation("Get a list of germplasms by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = GermplasmGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Germplasm not found (if any provided URIs is not found)", response = ErrorDTO.class)
    })

    public Response searchGermplasmsByURIs(
            @ApiParam(value = "Germplasms URIs") List<URI> uris
    ) throws Exception {

        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setLang(currentUser.getLanguage());
        filter.setUris(uris);

        boolean isAdmin = currentUser.isAdmin();

        List<GermplasmModel> models = new GermplasmLogic(sparql, nosql, currentUser)
                .search(filter, false, false)
                .getList();

        if (!models.isEmpty()) {
            List<GermplasmGetAllDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(GermplasmGetAllDTO.fromModel(result)));

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
            @ApiParam(value = "Regex pattern for filtering experiments by name", example = ".*") @DefaultValue(".*") @QueryParam("attribute_value") String name,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ListWithPagination<ExperimentModel> experiments = new GermplasmLogic(sparql, nosql, currentUser)
                .getExpFromGermplasm(currentUser, uri, name, orderByList, page, pageSize);

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
        @ApiResponse(code = 200, message = "Return germplasm attributes", response = String.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "Germplasm attributes not found", response = ErrorDTO.class)
    })
    public Response getGermplasmAttributes() throws Exception {
        Set<String> attributes = new GermplasmLogic(sparql, nosql, currentUser)
                .getDistinctGermplasmAttributes();
        return new SingleObjectResponse<>(attributes).getResponse();
    }

    @GET
    @Path("attributes/{attribute}")
    @ApiOperation("Get attribute values of all germplasm for a given attribute")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return germplasm attributes", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Germplasm attributes not found", response = ErrorDTO.class)
    })
    public Response getGermplasmAttributeValues(
            @PathParam("attribute") @NotNull @NotEmpty String attributeKey,
            @ApiParam(value = "Regex pattern for filtering attribute value", example = ".*") @QueryParam("attribute_value") String name,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        Set<String> values = new GermplasmLogic(sparql, nosql, currentUser)
                .getDistinctGermplasmAttributesValues(attributeKey,name,page,pageSize);
        return new SingleObjectResponse<>(values).getResponse();
    }

    /**
     * Searches and returns a paginated list of germplasms based on various criteria.
     * <p>
     * Available filters include: URI, RDF type, name/synonyms, code, species,
     * variety, accession, institute, related experiment, parents (A, B, or both),
     * production year, group, metadata, and visibility (public/private).
     * <br>
     * Results can be sorted, paginated, and adapted to the language of the current user.
     * </p>
     *
     * @param uri               regex filter on the germplasm URI
     * @param type              RDF type of the germplasm
     * @param name              regex on the name or synonyms
     * @param code              regex on the internal code
     * @param productionYear    production year
     * @param species           species of the germplasm
     * @param variety           variety of the germplasm
     * @param accession         accession of the germplasm
     * @param group             group related to the germplasm
     * @param institute         associated institute or organization
     * @param experiment        related experiment
     * @param parentGermplasms  list of parent germplasms (A or B)
     * @param parentGermplasmsM maternal parents (type A)
     * @param parentGermplasmsF paternal parents (type B)
     * @param metadata          associated metadata
     * @param isPublic          visibility: public ({@code true}) or private ({@code false})
     * @param orderByList       sorting criteria (e.g., "uri=asc", "name=desc")
     * @param page              page number (≥ 0)
     * @param pageSize          page size (≥ 0)
     * @return an HTTP response containing a paginated list of {@link GermplasmGetAllDTO}
     * @throws Exception if an error occurs during the search or data access
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
            @ApiParam(value = "Group filter") @QueryParam("group_of_germplasm") @ValidURI URI group,
            @ApiParam(value = "Search by institute", example = GERMPLASM_EXAMPLE_INSTITUTE) @QueryParam("institute") String institute,
            @ApiParam(value = "Search by experiment") @QueryParam("experiment") URI experiment,
            @ApiParam(value = "Search by parent varieties A or B") @QueryParam("parent_germplasms") List<URI> parentGermplasms,
            @ApiParam(value = "Search by parent varieties A") @QueryParam("parent_germplasms_m") List<URI> parentGermplasmsM,
            @ApiParam(value = "Search by parent varieties B") @QueryParam("parent_germplasms_f") List<URI> parentGermplasmsF,
            @ApiParam(value = "Search by metadata", example = GERMPLASM_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Search private(false) or public germplasm (true)") @QueryParam("is_public") Boolean isPublic,
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
                .setPublic(isPublic)
                .setInstitute(institute)
                .setProductionYear(productionYear)
                .setExperiment(experiment)
                .setMetadata(metadata)
                .setGroup(group)
                .setParentGermplasms(parentGermplasms)
                .setParentMGermplasms(parentGermplasmsM)
                .setParentFGermplasms(parentGermplasmsF)
                .setUser(currentUser);

        // get user groups
        URI userURI = currentUser.getUri();
        GroupDAO dao = new GroupDAO(sparql);
        List<GroupModel> userGroups = dao.getUserGroups(userURI);

        // extraction of URIs
                List<URI> groupURIs = userGroups.stream()
                        .map(GroupModel::getUri)
                        .collect(Collectors.toList());

        // inject into the filter
        searchFilter.setGroups(groupURIs);


        searchFilter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        ListWithPagination<GermplasmModel> resultList = new GermplasmLogic(sparql, nosql, currentUser)
                .search(searchFilter, false, false);


        // Conversion DTO
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
        searchFilter.setUser(currentUser);
        List<GermplasmModel> resultList = new GermplasmLogic(sparql, nosql, currentUser)
                .search(searchFilter,false,false).getList();

        return buildCSV(resultList);
    }

    private Response buildCSV(List<GermplasmModel> germplasmList) throws Exception {
        // Convert list to DTO
        //During this operation count the maximum number of occurrences of each duplicatable column
        List<GermplasmGetSingleDTO> resultDTOList = new ArrayList<>();
        int maximumParentAmount = 0;
        int maximumParentMAmount = 0;
        int maximumParentFAmount = 0;
        Set<String> metadataKeys = new HashSet<>();
        for (GermplasmModel germplasm : germplasmList) {
            GermplasmGetSingleDTO dto = GermplasmGetSingleDTO.fromModel(germplasm);
            List<GermplasmGetAllDTO> parents = dto.getHasParentGermplasm();
            maximumParentAmount = checkMaximumParentListSize(maximumParentAmount, parents);
            List<GermplasmGetAllDTO> parentsM = dto.getHasParentGermplasmM();
            maximumParentMAmount = checkMaximumParentListSize(maximumParentMAmount, parentsM);
            List<GermplasmGetAllDTO> parentsF = dto.getHasParentGermplasmF();
            maximumParentFAmount = checkMaximumParentListSize(maximumParentFAmount, parentsF);
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
        //Handle the creation of duplicated columns here
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        JsonNode jsonTree = mapper.convertValue(resultDTOList, JsonNode.class);

        List<JsonNode> list = new ArrayList<>();
        if(jsonTree.isArray()) {
            for(JsonNode jsonNode : jsonTree) {
                ObjectNode objectNode = jsonNode.deepCopy();
                divideParentColumnsIntoMultiple(objectNode);
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

        var columnNameSet = new LinkedHashSet<String>();
        for (var node : arrayNode) {
            node.fieldNames().forEachRemaining(columnNameSet::add);
        }
        columnNameSet.forEach(csvSchemaBuilder::addColumn);

        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        StringWriter stringWriter = new StringWriter();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class)
                .with(csvSchema)
                .writeValue(stringWriter, arrayNode);

        //Modify final exported gigantic string to replace our temporary duplicated column names with their real name
        //Remove any white spaces from these real names, as it seems to replace white spaces with a new column symbol
        String giganticResultString = stringWriter.toString();
        OntologyDAO ontologyDao = new OntologyDAO(sparql);
        giganticResultString = replaceTemporaryDuplicateColumnName(
                giganticResultString,
                maximumParentAmount,
                GermplasmGetExportDTO.hasParentGermplasmFieldName,
                Oeso.hasParentGermplasm.getURI(),
                ontologyDao
        );
        giganticResultString = replaceTemporaryDuplicateColumnName(
                giganticResultString,
                maximumParentMAmount,
                GermplasmGetExportDTO.hasParentMGermplasmFieldName,
                Oeso.hasParentGermplasmM.getURI(),
                ontologyDao
        );
        giganticResultString = replaceTemporaryDuplicateColumnName(
                giganticResultString,
                maximumParentFAmount,
                GermplasmGetExportDTO.hasParentFGermplasmFieldName,
                Oeso.hasParentGermplasmF.getURI(),
                ontologyDao
        );

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_germplasm" + dtf.format(date) + ".csv";

        return Response.ok(giganticResultString, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    //Some private functions used in the the above buildCsv function

    private String replaceTemporaryDuplicateColumnName(String csvString, int temporaryColumnQuantity, String dtoField, String propertyUri, OntologyDAO ontologyDao) throws URISyntaxException, SPARQLException {
        for(int duplicatedIndex = 0; duplicatedIndex < temporaryColumnQuantity; duplicatedIndex++){
            csvString = csvString.replaceAll(
                    dtoField + duplicatedIndex,
                    ontologyDao.getURILabel(new URI(propertyUri), currentUser.getLanguage()).replaceAll(" ", "")
            );
        }
        return csvString;
    }

    private void addTemporaryColsForDuplicateProp(JsonNode propValueAsListJsonNode, ObjectNode objectNode, String dtoFieldName){
        int i = 0;
        Iterator<JsonNode> parentsIterator = propValueAsListJsonNode.elements();
        while(parentsIterator.hasNext()){
            JsonNode parent = parentsIterator.next();
            String nextParentUri = parent.get("uri").asText();
            objectNode.put(dtoFieldName + i, nextParentUri);
            i++;
        }
    }

    private void divideParentColumnsIntoMultiple(ObjectNode objectNode){
        //Capture parent information, remove then add duplicated nodes with a suffix
        JsonNode parents = objectNode.get(GermplasmGetExportDTO.hasParentGermplasmFieldName);
        JsonNode parentsM = objectNode.get(GermplasmGetExportDTO.hasParentMGermplasmFieldName);
        JsonNode parentsF = objectNode.get(GermplasmGetExportDTO.hasParentFGermplasmFieldName);
        objectNode.remove(GermplasmGetExportDTO.hasParentGermplasmFieldName);
        objectNode.remove(GermplasmGetExportDTO.hasParentMGermplasmFieldName);
        objectNode.remove(GermplasmGetExportDTO.hasParentFGermplasmFieldName);

        addTemporaryColsForDuplicateProp(parents, objectNode, GermplasmGetExportDTO.hasParentGermplasmFieldName);
        addTemporaryColsForDuplicateProp(parentsM, objectNode, GermplasmGetExportDTO.hasParentMGermplasmFieldName);
        addTemporaryColsForDuplicateProp(parentsF, objectNode, GermplasmGetExportDTO.hasParentFGermplasmFieldName);
    }

    private int checkMaximumParentListSize(int oldMaximum, List<GermplasmGetAllDTO> nextListToCheck){
        if(!CollectionUtils.isEmpty(nextListToCheck) && nextListToCheck.size() > oldMaximum){
            return nextListToCheck.size();
        }
        return oldMaximum;
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
        @ApiResponse(code = 200, message = "Germplasm updated", response = URI.class),
        @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})
    public Response updateGermplasm(
            @ApiParam("Germplasm description") @Valid GermplasmUpdateDTO germplasmDTO
    ) throws Exception {
        try {
            GermplasmModel model = germplasmDTO.newModel(sparql, currentUser.getLanguage(), null);
            model = new GermplasmLogic(sparql, nosql, currentUser)
                    .update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown Germplasm URI ", URI.create(germplasmDTO.getUri()));
        } catch (Exception e) {
            throw e;
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
        new GermplasmLogic(sparql, nosql, currentUser).delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Check if germplasms exist in the database, empty uris or not well-formed URIs are ignored.
     */
    @POST
    @Path("check")
    @ApiOperation("check germplasms exist")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return existant germplasm uris", response = URI.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response checkGermplasmsExist(
            @ApiParam(value = "list of uris to check for existence") StringURIsListDTO uris
            ) throws Exception {
        Collection<URI> existantUris = new GermplasmLogic(sparql, nosql, currentUser).getExistingUrisFromString(uris.getUris());

        return new PaginatedListResponse<>(new ArrayList<>(existantUris)).getResponse();
    }

}
