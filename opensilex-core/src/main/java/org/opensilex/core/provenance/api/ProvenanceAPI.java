//******************************************************************************
//                          ProvenanceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import io.swagger.annotations.*;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.dal.DataDaoV2;
import org.opensilex.core.data.dal.DataFileDaoV2;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDaoV2;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.provenance.dal.ProvenanceSearchFilter;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.displayable.DisplayableBadRequestException;
import org.opensilex.server.response.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provenance API
 *
 * @author Alice Boizet
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path(ProvenanceAPI.PATH)
@ApiCredentialGroup(
        groupId = ProvenanceAPI.CREDENTIAL_PROVENANCE_GROUP_ID,
        groupLabelKey = ProvenanceAPI.CREDENTIAL_PROVENANCE_GROUP_LABEL_KEY
)
public class ProvenanceAPI {

    public static final String PATH = "/core/provenances";

    public static final String PROVENANCE_ACTIVITY_TYPE = "http://www.w3.org/ns/prov#Activity";
    public static final String PROVENANCE_EXAMPLE_URI = "http://opensilex.dev/id/provenance/provenancelabel";

    public static final String CREDENTIAL_PROVENANCE_GROUP_ID = "Provenances";
    public static final String CREDENTIAL_PROVENANCE_GROUP_LABEL_KEY = "credential-groups.provenance";

    public static final String CREDENTIAL_PROVENANCE_MODIFICATION_ID = "provenance-modification";
    public static final String CREDENTIAL_PROVENANCE_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_PROVENANCE_DELETE_ID = "provenance-delete";
    public static final String CREDENTIAL_PROVENANCE_DELETE_LABEL_KEY = "credential.default.delete";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private MongoDBService nosql;

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Add a provenance")
    @ApiProtected
    @ApiCredential(
            credentialId = ProvenanceAPI.CREDENTIAL_PROVENANCE_MODIFICATION_ID,
            credentialLabelKey = ProvenanceAPI.CREDENTIAL_PROVENANCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A provenance is created", response = URI.class),
            @ApiResponse(code = 409, message = "A provenance with the same URI already exists", response = ErrorResponse.class)})

    public Response createProvenance(
            @ApiParam("Provenance description") @Valid ProvenanceCreationDTO provDTO
    ) throws Exception {

        if (provDTO.getActivity() != null) {
            ErrorResponse actError = checkActivityTypes(provDTO.getActivity());
            if (actError != null) {
                return actError.getResponse();
            }
        }

        if (provDTO.getAgents() != null) {
            List<AgentModel> agentsWithoutDuplicates = new ArrayList<>(new HashSet<>(provDTO.getAgents()));
            provDTO.setAgents(agentsWithoutDuplicates);

            checkAgents(provDTO.getAgents());
        }

        try {
            ProvenanceDaoV2 provDAO = new ProvenanceDaoV2(nosql.getServiceV2());
            ProvenanceModel model = provDTO.newModel();
            model.setPublisher(currentUser.getUri());
            provDAO.create(model);

            return new CreatedUriResponse(model.getUri()).getResponse();
            
        } catch (NoSQLAlreadyExistingUriException exception) {            
             // Return error response 409 - CONFLICT if experiment URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Provenance already exists",
                    "Duplicated URI: " + exception.getUri()
            ).getResponse();
        }

    }


    @GET
    @Path("{uri}")
    @ApiOperation("Get a provenance")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Provenance retrieved", response = ProvenanceGetDTO.class)
    })
    public Response getProvenance(
            @ApiParam(value = "Provenance URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        ProvenanceDaoV2 dao = new ProvenanceDaoV2(nosql.getServiceV2());
        try {
            ProvenanceModel provenance = dao.get(uri);
            ProvenanceGetDTO dto = ProvenanceGetDTO.fromModel(provenance);
            if (Objects.nonNull(provenance.getPublisher())) {
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(provenance.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown provenance URI ", uri);
        }

    }

    @GET
    @ApiOperation("Get provenances")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenances list", response = ProvenanceGetDTO.class, responseContainer = "List")
    })
    public Response searchProvenance(
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name,
            @ApiParam(value = "Search by description") @QueryParam("description") String description,
            @ApiParam(value = "Search by activity URI") @QueryParam("activity") URI activityUri,
            @ApiParam(value = "Search by activity type") @QueryParam("activity_type") URI activityType,
            @ApiParam(value = "Search by agent URIs") @QueryParam("agent") List<URI> agentURIs,
            @ApiParam(value = "Search by agent type") @QueryParam("agent_type") URI agentType,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=asc") @DefaultValue("date=desc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        ProvenanceDaoV2 dao = new ProvenanceDaoV2(nosql.getServiceV2());
        ProvenanceSearchFilter filter = new ProvenanceSearchFilter()
                .setName(name)
                .setDescription(description)
                .setActivityType(activityType)
                .setActivityUri(activityUri)
                .setAgentType(agentType)
                .setAgentURIs(agentURIs);

        filter.setOrderByList(orderByList).setPage(page).setPageSize(pageSize);

        ListWithPagination<ProvenanceGetDTO> results = dao.searchWithPagination(
                new MongoSearchQuery<ProvenanceModel, ProvenanceSearchFilter, ProvenanceGetDTO>()
                        .setFilter(filter)
                        .setConvertFunction(ProvenanceGetDTO::fromModel)
        );
        return new PaginatedListResponse<>(results).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a provenance that doesn't describe data")
    @ApiProtected
    @ApiCredential(
            credentialId = ProvenanceAPI.CREDENTIAL_PROVENANCE_DELETE_ID,
            credentialLabelKey = ProvenanceAPI.CREDENTIAL_PROVENANCE_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Provenance deleted", response = URI.class)
    })
    public Response deleteProvenance(
            @ApiParam(value = "Provenance URI", example = PROVENANCE_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) throws Exception {
        ProvenanceDaoV2 dao = new ProvenanceDaoV2(nosql.getServiceV2());

        //check if the provenance can be deleted (not linked to data)
        List<URI> provenances = new ArrayList<>();
        provenances.add(uri);

        DataDaoV2 dataDAO = new DataDaoV2(sparql, nosql, null);
        DataFileDaoV2 dataFileDaoV2 = new DataFileDaoV2(nosql, sparql);
        DataSearchFilter dataSearchFilter = new DataSearchFilter();
        dataSearchFilter.setUser(currentUser).setProvenances(provenances);

        long datacount = dataDAO.count(dataSearchFilter);
        long datafilesCount = dataFileDaoV2.count(dataSearchFilter);

        if (datacount > 0 || datafilesCount > 0) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "The provenance is linked to some data",
                    "You can't delete a provenance linked to data"
            ).getResponse();
        } else {
            try {
                dao.delete(uri);
                return new ObjectUriResponse(Response.Status.OK, uri).getResponse();

            } catch (NoSQLInvalidURIException e) {
                throw new NotFoundURIException("Invalid or unknown provenance URI ", uri);
            }
        }
    }

    @PUT
    @ApiProtected
    @ApiOperation("Update a provenance")
    @ApiCredential(
            credentialId = ProvenanceAPI.CREDENTIAL_PROVENANCE_MODIFICATION_ID,
            credentialLabelKey = ProvenanceAPI.CREDENTIAL_PROVENANCE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Provenance updated", response = URI.class)
    })

    public Response updateProvenance(
            @ApiParam("Provenance description") @Valid ProvenanceUpdateDTO dto
    ) throws Exception {
        try {
            if (dto.getActivity() != null) {
                ErrorResponse actError = checkActivityTypes(dto.getActivity());
                if (actError != null) {
                    return actError.getResponse();
                }
            }

            if (dto.getAgents() != null) {
                List<AgentModel> agentsWithoutDuplicates = new ArrayList<>(new HashSet<>(dto.getAgents()));
                dto.setAgents(agentsWithoutDuplicates);

               checkAgents(dto.getAgents());
            }

            ProvenanceDaoV2 dao = new ProvenanceDaoV2(nosql.getServiceV2());
            ProvenanceModel newProvenance = dto.newModel();
            dao.update(newProvenance);
            return new ObjectUriResponse(Response.Status.OK, newProvenance.getUri()).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown provenance URI ", dto.getUri());
        }
    }

    /**
     * * Return a list of provenancess corresponding to the given URIs
     *
     * @param uris list of provenancess uri
     * @return Corresponding list of provenancess
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @ApiOperation("Get a list of provenances by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return provenancess list", response = ProvenanceGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Provenance not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getProvenancesByURIs(
            @ApiParam(value = "Provenances URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        ProvenanceDaoV2 dao = new ProvenanceDaoV2(nosql.getServiceV2());
        List<ProvenanceModel> models = dao.findByUris(uris.stream(), uris.size());

        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>(models.size());
        models.forEach(result -> resultDTOList.add(ProvenanceGetDTO.fromModel(result)));

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    private ErrorResponse checkActivityTypes(List<ActivityCreationDTO> activities) throws Exception {

        Set<URI> checkedActivityTypes = new HashSet<>();
        Set<URI> notActivityTypes = new HashSet<>();

        for (ActivityCreationDTO activity : activities) {
            if (!checkedActivityTypes.contains(activity.getRdfType())) {
                boolean exists = sparql.executeAskQuery(new AskBuilder()
                        .addWhere(SPARQLDeserializers.nodeURI(activity.getRdfType()), Ontology.subClassAny, SPARQLDeserializers.nodeURI(PROVENANCE_ACTIVITY_TYPE))
                );
                if (!exists) {
                    notActivityTypes.add(activity.getRdfType());
                }
                checkedActivityTypes.add(activity.getRdfType());
            }
        }

        if (!notActivityTypes.isEmpty()) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "wrong activity rdf_types",
                    "wrong activity rdf_types: " + notActivityTypes
            );
        } else {
            return null;
        }

    }

    /**
     * @param agentModels is a list of agentModel we want to ckeck before create/update a provenance
     * @throws DisplayableBadRequestException with personalized message if agent rdf typ is not an Oeso:Operator or a subclasse of Oeso:Device
     * or if the uri of the uri of the agent doesn't exist or don't match with the agent type.
     */
    private void checkAgents(List<AgentModel> agentModels) throws Exception {

        List<AgentModel> devices = new ArrayList<>();
        List<AgentModel> operators = new ArrayList<>();

        checkAgentRdfTypeAndSortThemIntoDeviceAndOperator(agentModels, devices, operators);

        Set<URI> notAgentUris = new HashSet<>();

        Collection<URI> devicesUris = devices.stream().map(AgentModel::getUri).collect(Collectors.toList());
        notAgentUris.addAll(sparql.getExistingUris(DeviceModel.class, devicesUris, false));

        Collection<URI> operatorsUris = operators.stream().map(AgentModel::getUri).collect(Collectors.toList());
        notAgentUris.addAll(sparql.getExistingUris(PersonModel.class, operatorsUris, false));

        if (!notAgentUris.isEmpty()) {
            throw new DisplayableBadRequestException(
                    "Agent uris don't exist or given uris are not agents, wrong agent uris: " + notAgentUris,
                    null
            );
        }
    }

    private void checkAgentRdfTypeAndSortThemIntoDeviceAndOperator(List<AgentModel> agentModels, List<AgentModel> devices, List<AgentModel> operators) throws SPARQLException {
        Set<URI> notAgentTypes = new HashSet<>();
        boolean isDevice;
        boolean isOperator;
        for (AgentModel agent : agentModels) {
            isDevice = sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(agent.getRdfType()), Ontology.subClassAny, sparql.getRDFType(DeviceModel.class))
            );

            isOperator = SPARQLDeserializers.compareURIs( agent.getRdfType(), Oeso.Operator.getURI());

            if (isDevice) {
                devices.add(agent);
            } else if (isOperator) {
                operators.add(agent);
            } else {
                notAgentTypes.add(agent.getRdfType());
            }
        }

        if ( !notAgentTypes.isEmpty() ) {
            throw new DisplayableBadRequestException("wrong agent rdfTypes: " + notAgentTypes, null);
        }
    }

}
