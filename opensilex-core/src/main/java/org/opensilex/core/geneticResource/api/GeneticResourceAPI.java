/*
 * ******************************************************************************
 *                                     GeneticResourceAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  23 March, 2020
 *  Contact: alice.boizet@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.geneticResource.api;

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
import org.opensilex.core.URIsListPostDTO;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geneticResource.bll.GeneticResourceLogic;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
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
@Api(GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_ID)
@Path("/core/geneticResource")
@ApiCredentialGroup(
        groupId = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_ID,
        groupLabelKey = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_LABEL_KEY
)

public class GeneticResourceAPI {

    public static final String CREDENTIAL_GENETIC_RESOURCE_GROUP_ID = "GeneticResource";
    public static final String CREDENTIAL_GENETIC_RESOURCE_GROUP_LABEL_KEY = "credential-groups.geneticResource";

    public static final String CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID = "geneticResource-modification";
    public static final String CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_GENETIC_RESOURCE_DELETE_ID = "geneticResource-delete";
    public static final String CREDENTIAL_GENETIC_RESOURCE_DELETE_LABEL_KEY = "credential.default.delete";

    public static final String GENETIC_RESOURCE_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";
    public static final String GENETIC_RESOURCE_EXAMPLE_TYPE = "http://www.opensilex.org/vocabulary/oeso#Variety";
    public static final String GENETIC_RESOURCE_EXAMPLE_SPECIES = "http://www.phenome-fppn.fr/id/species/zeamays";
    protected static final String GENETIC_RESOURCE_EXAMPLE_VARIETY = "http://opensilex.test/id/geneticResource/variety.huachano";
    protected static final String GENETIC_RESOURCE_EXAMPLE_ACCESSION = "http://opensilex.test/id/geneticResource/accession.v_a_x_v_b";
    protected static final String GENETIC_RESOURCE_EXAMPLE_INSTITUTE = "INRA";
    protected static final String GENETIC_RESOURCE_EXAMPLE_PRODUCTION_YEAR = "2020";
    protected static final String GENETIC_RESOURCE_EXAMPLE_METADATA = "{ \"water_stress\" : \"resistant\",\n" +
                                                        "\"yield\" : \"moderate\"}";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBServiceV2 nosql;

    @CurrentUser
    AccountModel currentUser;

    /**
     *
     * @param geneticResourceDTO
     * @param checkOnly
     * @return
     * @throws Exception
     */
    @POST
    @ApiOperation("Add a geneticResource")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add a geneticResource (variety, accession, plantMaterialLot)", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})

    public Response createGeneticResource(
            @ApiParam("GeneticResource description") @Valid GeneticResourceCreationDTO geneticResourceDTO,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        GeneticResourceLogic geneticResourceBusiness= new GeneticResourceLogic(sparql, nosql, currentUser);
        GeneticResourceModel model = geneticResourceDTO.newModel(sparql, currentUser.getLanguage(), null);

        if (!checkOnly) {

            try {
                GeneticResourceModel geneticResource = geneticResourceBusiness.create(model);
                return new CreatedUriResponse(geneticResource.getUri()).getResponse();
            } catch ( WebApplicationException exception){
                throw exception;
            } catch (Exception e) {
                return new ErrorResponse(e).getResponse();
            }

        } else {
            //raise a Displayable Exception if the geneticResource already exists or is incorrect
            geneticResourceBusiness.checkBeforeCreateOrUpdate(Collections.singletonList(model), false);
            return new ObjectUriResponse().getResponse();
        }

    }

    /**
     *  create and/or update many geneticResources if everything is correct, block everything if there is an error.
     *  JSon validation id disabled to handle manually errors like uri format, it allows to return errors as multiple errors.
     *  Errors are returned with the index of the geneticResource in the list as key.
     * @param geneticResourceDTOs
     * @param checkOnly
     * @return
     * @throws Exception
     */
    @POST
    @Path("import")
    @ApiOperation("Add or update many geneticResources")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "geneticResources are created and/or updated", response = URI.class),
            @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})

    public Response upsertGeneticResources(
            @ApiParam("List of geneticResource description") List<GeneticResourceCreationDTO> geneticResourceDTOs,
            @ApiParam(value = "Checking only", example = "false") @DefaultValue("false") @QueryParam("checkOnly") Boolean checkOnly
    ) throws Exception {
        GeneticResourceLogic geneticResourceBusiness= new GeneticResourceLogic(sparql, nosql, currentUser);
        List<GeneticResourceModel> models;
        MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> relationsErrors = null;

        try {
            models = getGeneticResourceModelsAndValidateRelations(geneticResourceDTOs);
        } catch (InvalidValueException e) {
            relationsErrors = new MultipleErrorObjectList<>("geneticResources errors", new ArrayList<>(), MultipleCreateUpdateErrorObject::new);
            models = getGeneticResourceModelWithoutRelationsAndCollectErrors(geneticResourceDTOs, relationsErrors);
        }

        if (!checkOnly) {

            try {
                List<GeneticResourceModel> geneticResources = geneticResourceBusiness.upsert(models);
                List<URI> uris = geneticResources.stream().map(GeneticResourceModel::getUri).toList();
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
            var multipleErrorObjectList = geneticResourceBusiness.checkBeforeCreateOrUpdate(models, true);

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
     *  This method validates relations of each geneticResourceDTO. It would be better to validate relations in the business layer, but this would require refactoring.
     * @throws MultipleErrorListException
     */
    private List<GeneticResourceModel> getGeneticResourceModelsAndValidateRelations(List<GeneticResourceCreationDTO> geneticResourceDTOs) throws URISyntaxException, SPARQLException {
        List<GeneticResourceModel> models = new ArrayList<>();
        if (CollectionUtils.isEmpty(geneticResourceDTOs)) {
            return models;
        }

        Map<URI, ClassModel> classModels = new HashMap<>();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        //get every different types from DTOs
        List<URI> geneticResourcesTypes = geneticResourceDTOs.stream().map(GeneticResourceCreationDTO::getType).distinct().toList();
        for (URI type : geneticResourcesTypes) {
            classModels.put(type, ontologyDAO.getClassModel(type, new URI(Oeso.GeneticResource.getURI()), currentUser.getLanguage()));
        }

        for (GeneticResourceCreationDTO geneticResourceDTO : geneticResourceDTOs) {
            models.add(geneticResourceDTO.newModel(sparql, currentUser.getLanguage(), classModels.get(geneticResourceDTO.getType())));
        }

        return models;
    }

    /**
     * This method is used to handle geneticResources errors on relations. It would be better to handle relations in the business layer, but this would require refactoring.
     * return geneticResources. For geneticResources with relations errors, we return the geneticResource model without relations and collect errors in the errors list.
     * This method fill the errors list with geneticResource models and errors.
     * @param errors should be initialized with an empty list of models. Models will be added to the list by this method.
     * @return a list of geneticResource models without relations.
     */
    private List<GeneticResourceModel> getGeneticResourceModelWithoutRelationsAndCollectErrors(List<GeneticResourceCreationDTO> geneticResourceDTOs, MultipleErrorObjectList<MultipleCreateUpdateErrorObject, GeneticResourceModel> errors) throws URISyntaxException, SPARQLException {
        List<GeneticResourceModel> models = new ArrayList<>();
        if (CollectionUtils.isEmpty(geneticResourceDTOs)) {
            return models;
        }

        Map<URI, ClassModel> classModels = new HashMap<>();
        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        //get every different types from DTOs
        List<URI> geneticResourcesTypes = geneticResourceDTOs.stream().map(GeneticResourceCreationDTO::getType).distinct().toList();
        for (URI type : geneticResourcesTypes) {
            classModels.put(type, ontologyDAO.getClassModel(type, new URI(Oeso.GeneticResource.getURI()), currentUser.getLanguage()));
        }

        for (GeneticResourceCreationDTO geneticResourceDTO : geneticResourceDTOs) {
            GeneticResourceModel model = null;
            try {
                model = geneticResourceDTO.newModel(sparql, currentUser.getLanguage(), classModels.get(geneticResourceDTO.getType()));
                errors.addModel(model);
            } catch (InvalidValueException e){
                model = geneticResourceDTO.newModelWithoutRelations();
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
    @ApiOperation("Get a geneticResource")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return geneticResource", response = GeneticResourceGetSingleDTO.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "GeneticResource not found", response = ErrorDTO.class)
    })
    public Response getGeneticResource(
            @ApiParam(value = "geneticResource URI", example = GENETIC_RESOURCE_EXAMPLE_SPECIES, required = true) @PathParam("uri") @ValidURI @NotNull URI uri
    ) throws Exception {
        GeneticResourceModel model = new GeneticResourceLogic(sparql, nosql, currentUser).get(uri, true);

        if (model != null) {
            // Return GeneticResourceGetDTO
            GeneticResourceGetSingleDTO dto = GeneticResourceGetSingleDTO.fromModel(model);
            if (Objects.nonNull(model.getPublisher())) {
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException("GeneticResource URI not found : ", uri);
        }
    }

    /**
     * * Return a list of geneticResources corresponding to the given URIs
     *
     * @param uris list of geneticResources uri
     * @return Corresponding list of geneticResources
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("by_uris")
    @ApiOperation("Get a list of geneticResources by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = GeneticResourceGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "GeneticResource not found (if any provided URIs is not found", response = ErrorDTO.class)
    })

    public Response getGeneticResourcesByURI(
            @ApiParam(value = "GeneticResources URIs") List<URI> uris
    ) throws Exception {

        GeneticResourceSearchFilter filter = new GeneticResourceSearchFilter();
        filter.setLang(currentUser.getLanguage());
        filter.setUris(uris);

        boolean isAdmin = currentUser.isAdmin();

        List<GeneticResourceModel> models = new GeneticResourceLogic(sparql, nosql, currentUser)
                .search(filter, false, false)
                .getList();

        if (!models.isEmpty()) {
            List<GeneticResourceGetAllDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> resultDTOList.add(GeneticResourceGetAllDTO.fromModel(result)));

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "GeneticResource not found",
                    "Unknown geneticResource URIs"
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
    @ApiOperation("Get experiments where a geneticResource has been used")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return geneticResource", response = ExperimentGetListDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "GeneticResource not found", response = ErrorDTO.class)
    })
    public Response getGeneticResourceExperiments(
            @ApiParam(value = "geneticResource URI", example = "dev-geneticResource:g01", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Regex pattern for filtering experiments by name", example = ".*") @DefaultValue(".*") @QueryParam("attribute_value") String name,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ListWithPagination<ExperimentModel> experiments = new GeneticResourceLogic(sparql, nosql, currentUser)
                .getExpFromGeneticResource(currentUser, uri, name, orderByList, page, pageSize);

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = experiments.convert(
                ExperimentGetListDTO.class,
                ExperimentGetListDTO::fromModel
        );

        // Return paginated list of exp DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
    /**
     * Return all available geneticResource attributes
     * @return
     * @throws java.lang.Exception
     */
    @GET
    @Path("attributes")
    @ApiOperation("Get attributes of all geneticResource")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return geneticResource attributes", response = String.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "GeneticResource attributes not found", response = ErrorDTO.class)
    })
    public Response getGeneticResourceAttributes() throws Exception {
        Set<String> attributes = new GeneticResourceLogic(sparql, nosql, currentUser)
                .getDistinctGeneticResourceAttributes();
        return new SingleObjectResponse<>(attributes).getResponse();
    }

    @GET
    @Path("attributes/{attribute}")
    @ApiOperation("Get attribute values of all geneticResource for a given attribute")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return geneticResource attributes", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "GeneticResource attributes not found", response = ErrorDTO.class)
    })
    public Response getGeneticResourceAttributeValues(
            @PathParam("attribute") @NotNull @NotEmpty String attributeKey,
            @ApiParam(value = "Regex pattern for filtering attribute value", example = ".*") @QueryParam("attribute_value") String name,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        Set<String> values = new GeneticResourceLogic(sparql, nosql, currentUser)
                .getDistinctGeneticResourceAttributesValues(attributeKey,name,page,pageSize);
        return new SingleObjectResponse<>(values).getResponse();
    }

    /**
     * Searches and returns a paginated list of geneticResources based on various criteria.
     * <p>
     * Available filters include: URI, RDF type, name/synonyms, code, species,
     * variety, accession, institute, related experiment, parents (A, B, or both),
     * production year, group, metadata, and visibility (public/private).
     * <br>
     * Results can be sorted, paginated, and adapted to the language of the current user.
     * </p>
     *
     * @param uri               regex filter on the geneticResource URI
     * @param type              RDF type of the geneticResource
     * @param name              regex on the name or synonyms
     * @param code              regex on the internal code
     * @param productionYear    production year
     * @param species           species of the geneticResource
     * @param variety           variety of the geneticResource
     * @param accession         accession of the geneticResource
     * @param group             group related to the geneticResource
     * @param institute         associated institute or organization
     * @param experiment        related experiment
     * @param parentGeneticResources  list of parent geneticResources (A or B)
     * @param parentGeneticResourcesM maternal parents (type A)
     * @param parentGeneticResourcesF paternal parents (type B)
     * @param metadata          associated metadata
     * @param isPublic          visibility: public ({@code true}) or private ({@code false})
     * @param orderByList       sorting criteria (e.g., "uri=asc", "name=desc")
     * @param page              page number (≥ 0)
     * @param pageSize          page size (≥ 0)
     * @return an HTTP response containing a paginated list of {@link GeneticResourceGetAllDTO}
     * @throws Exception if an error occurs during the search or data access
     */

    @GET
    @ApiOperation("Search geneticResource")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return geneticResource list", response = GeneticResourceGetAllDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response searchGeneticResource(
            @ApiParam(value = "Regex pattern for filtering list by uri", example = GENETIC_RESOURCE_EXAMPLE_URI) @QueryParam("uri") String uri,
            @ApiParam(value = "Search by type", example = GENETIC_RESOURCE_EXAMPLE_TYPE) @QueryParam("rdf_type") URI type,
            @ApiParam(value = "Regex pattern for filtering list by name and synonyms", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
            @ApiParam(value = "Regex pattern for filtering list by code", example = ".*") @DefaultValue(".*") @QueryParam("code") String code,
            @ApiParam(value = "Search by production year", example = GENETIC_RESOURCE_EXAMPLE_PRODUCTION_YEAR) @QueryParam("production_year") Integer productionYear,
            @ApiParam(value = "Search by species", example = GENETIC_RESOURCE_EXAMPLE_SPECIES) @QueryParam("species") URI species,
            @ApiParam(value = "Search by variety", example = GENETIC_RESOURCE_EXAMPLE_VARIETY) @QueryParam("variety") URI variety,
            @ApiParam(value = "Search by accession", example = GENETIC_RESOURCE_EXAMPLE_ACCESSION) @QueryParam("accession") URI accession,
            @ApiParam(value = "Group filter") @QueryParam("group_of_geneticResource") @ValidURI URI group,
            @ApiParam(value = "Search by institute", example = GENETIC_RESOURCE_EXAMPLE_INSTITUTE) @QueryParam("institute") String institute,
            @ApiParam(value = "Search by experiment") @QueryParam("experiment") URI experiment,
            @ApiParam(value = "Search by parent varieties A or B") @QueryParam("parent_geneticResources") List<URI> parentGeneticResources,
            @ApiParam(value = "Search by parent varieties A") @QueryParam("parent_geneticResources_m") List<URI> parentGeneticResourcesM,
            @ApiParam(value = "Search by parent varieties B") @QueryParam("parent_geneticResources_f") List<URI> parentGeneticResourcesF,
            @ApiParam(value = "Search by metadata", example = GENETIC_RESOURCE_EXAMPLE_METADATA) @QueryParam("metadata") String metadata,
            @ApiParam(value = "Search private(false) or public geneticResource (true)") @QueryParam("is_public") Boolean isPublic,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("label=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        GeneticResourceSearchFilter searchFilter = new GeneticResourceSearchFilter()
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
                .setParentGeneticResources(parentGeneticResources)
                .setParentMGeneticResources(parentGeneticResourcesM)
                .setParentFGeneticResources(parentGeneticResourcesF)
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

        ListWithPagination<GeneticResourceModel> resultList = new GeneticResourceLogic(sparql, nosql, currentUser)
                .search(searchFilter, false, false);


        // Conversion DTO
        ListWithPagination<GeneticResourceGetAllDTO> resultDTOList = resultList.convert(
                GeneticResourceGetAllDTO.class,
                GeneticResourceGetAllDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @POST
    @Path("export")
    @ApiOperation("export geneticResource")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_PLAIN})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return a csv file with geneticResource list"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response exportGeneticResource(
            @ApiParam("CSV export configuration") @Valid GeneticResourceSearchFilter searchFilter
    ) throws Exception {
        List<GeneticResourceModel> resultList = new GeneticResourceLogic(sparql, nosql, currentUser)
                .search(searchFilter,false,false).getList();

        return buildCSV(resultList);
    }

    private Response buildCSV(List<GeneticResourceModel> geneticResourceList) throws Exception {
        // Convert list to DTO
        //During this operation count the maximum number of occurrences of each duplicatable column
        List<GeneticResourceGetSingleDTO> resultDTOList = new ArrayList<>();
        int maximumParentAmount = 0;
        int maximumParentMAmount = 0;
        int maximumParentFAmount = 0;
        Set<String> metadataKeys = new HashSet<>();
        for (GeneticResourceModel geneticResource : geneticResourceList) {
            GeneticResourceGetSingleDTO dto = GeneticResourceGetSingleDTO.fromModel(geneticResource);
            List<GeneticResourceGetAllDTO> parents = dto.getHasParentGeneticResource();
            maximumParentAmount = checkMaximumParentListSize(maximumParentAmount, parents);
            List<GeneticResourceGetAllDTO> parentsM = dto.getHasParentGeneticResourceM();
            maximumParentMAmount = checkMaximumParentListSize(maximumParentMAmount, parentsM);
            List<GeneticResourceGetAllDTO> parentsF = dto.getHasParentGeneticResourceF();
            maximumParentFAmount = checkMaximumParentListSize(maximumParentFAmount, parentsF);
            resultDTOList.add(dto);
            Map<String,String> metadata = dto.getMetadata();
            if (metadata != null) {
                 metadataKeys.addAll(metadata.keySet());
            }           
        }

        if (resultDTOList.isEmpty()) {
            resultDTOList.add(new GeneticResourceGetSingleDTO()); // to return an empty table
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
                GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName,
                Oeso.hasParentGeneticResource.getURI(),
                ontologyDao
        );
        giganticResultString = replaceTemporaryDuplicateColumnName(
                giganticResultString,
                maximumParentMAmount,
                GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName,
                Oeso.hasParentGeneticResourceM.getURI(),
                ontologyDao
        );
        giganticResultString = replaceTemporaryDuplicateColumnName(
                giganticResultString,
                maximumParentFAmount,
                GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName,
                Oeso.hasParentGeneticResourceF.getURI(),
                ontologyDao
        );

        LocalDate date = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fileName = "export_geneticResource" + dtf.format(date) + ".csv";

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
        JsonNode parents = objectNode.get(GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName);
        JsonNode parentsM = objectNode.get(GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName);
        JsonNode parentsF = objectNode.get(GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName);
        objectNode.remove(GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName);
        objectNode.remove(GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName);
        objectNode.remove(GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName);

        addTemporaryColsForDuplicateProp(parents, objectNode, GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName);
        addTemporaryColsForDuplicateProp(parentsM, objectNode, GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName);
        addTemporaryColsForDuplicateProp(parentsF, objectNode, GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName);
    }

    private int checkMaximumParentListSize(int oldMaximum, List<GeneticResourceGetAllDTO> nextListToCheck){
        if(!CollectionUtils.isEmpty(nextListToCheck) && nextListToCheck.size() > oldMaximum){
            return nextListToCheck.size();
        }
        return oldMaximum;
    }

    /**
     * @param geneticResourceDTO the geneticResource to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated geneticResource {@link URI}
     */
    @PUT
    @ApiOperation("Update a geneticResource")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "GeneticResource updated", response = URI.class),
        @ApiResponse(code = 400, message = "Bad user request", response = MultipleErrorResponse.class)})
    public Response updateGeneticResource(
            @ApiParam("GeneticResource description") @Valid GeneticResourceUpdateDTO geneticResourceDTO
    ) throws Exception {
        try {
            GeneticResourceModel model = geneticResourceDTO.newModel(sparql, currentUser.getLanguage(), null);
            model = new GeneticResourceLogic(sparql, nosql, currentUser)
                    .update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown GeneticResource URI ", geneticResourceDTO.getUri());
        }
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a geneticResource")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_GENETIC_RESOURCE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_GENETIC_RESOURCE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGeneticResource(
            @ApiParam(value = "GeneticResource URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        new GeneticResourceLogic(sparql, nosql, currentUser).delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    /**
     * Check if geneticResources exist in the database, empty uris or not well-formed URIs are ignored.
     */
    @POST
    @Path("check")
    @ApiOperation("check geneticResources exist")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return existant geneticResource uris", response = URI.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorDTO.class)
    })
    public Response checkGeneticResourcesExist(
            @ApiParam(value = "list of uris to check for existence") URIsListPostDTO uris
            ) throws Exception {
        Collection<URI> existantUris = new GeneticResourceLogic(sparql, nosql, currentUser).getNonExistingUris(uris.getUris());

        return new PaginatedListResponse<>(new ArrayList<>(existantUris)).getResponse();
    }

}
