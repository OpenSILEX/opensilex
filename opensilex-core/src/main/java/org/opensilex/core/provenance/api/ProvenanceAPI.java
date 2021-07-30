//******************************************************************************
//                          ProvenanceAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.naming.NamingException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.URITypesModel;
import org.opensilex.core.provenance.dal.AgentModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * Provenance API
 *
 * @author Alice Boizet
 */
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)
@Path("/core/provenances")
@ApiCredentialGroup(
        groupId = DataAPI.CREDENTIAL_DATA_GROUP_ID,
        groupLabelKey = DataAPI.CREDENTIAL_DATA_GROUP_LABEL_KEY
)
public class ProvenanceAPI {

    public static final String PROVENANCE_ACTIVITY_TYPE = "http://www.w3.org/ns/prov#Activity";
    public static final String PROVENANCE_EXAMPLE_URI = "http://opensilex.dev/id/provenance/provenancelabel";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private MongoDBService nosql;

    @Inject
    private SPARQLService sparql;

    @POST
    @ApiOperation("Add a provenance")
    @ApiProtected
    @ApiCredential(
            credentialId = DataAPI.CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "A provenance is created", response = ObjectUriResponse.class),
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
            
            ErrorResponse error = checkAgents(provDTO.getAgents());
            if (error != null) {
                return error.getResponse();
            }
        }
        
        try {
            ProvenanceDAO provDAO = new ProvenanceDAO(nosql, sparql);                    
            ProvenanceModel model = provDTO.newModel();
            ProvenanceModel provenance = provDAO.create(model);

            return new ObjectUriResponse(Response.Status.CREATED, provenance.getUri()).getResponse();
            
        } catch (NoSQLAlreadyExistingUriException exception) {            
             // Return error response 409 - CONFLICT if experiment URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Provenance already exists",
                    "Duplicated URI: " + exception.getUri()
            ).getResponse();            
        } 
        
    }

    /**
     * @param uri
     * @return
     * @throws java.lang.Exception
     */
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

        ProvenanceDAO dao = new ProvenanceDAO(nosql, sparql);
        try {
            ProvenanceModel provenance = dao.get(uri);
            return new SingleObjectResponse<>(ProvenanceGetDTO.fromModel(provenance)).getResponse();
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown provenance URI ", uri);
        }

    }

    /**
     * @param name
     * @param experiment
     * @param activityType
     * @param agentURI
     * @param agentType
     * @param page
     * @param pageSize
     * @return
     * @throws java.lang.Exception
     */
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
            @ApiParam(value = "Search by agent URI") @QueryParam("agent") URI agentURI,
            @ApiParam(value = "Search by agent type") @QueryParam("agent_type") URI agentType,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "date=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        ProvenanceDAO dao = new ProvenanceDAO(nosql, sparql);
        ListWithPagination<ProvenanceModel> resultList = dao.search(null, name, description, activityType, activityUri, agentType, agentURI, orderByList, page, pageSize);
        
        // Convert paginated list to DTO
        ListWithPagination<ProvenanceGetDTO> provenances = resultList.convert(
                ProvenanceGetDTO.class,
                ProvenanceGetDTO::fromModel
        );
        return new PaginatedListResponse<>(provenances).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a provenance that doesn't describe data")
    @ApiProtected
    @ApiCredential(
            credentialId = DataAPI.CREDENTIAL_DATA_DELETE_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Provenance deleted", response = ObjectUriResponse.class)
    })
    public Response deleteProvenance(
            @ApiParam(value = "Provenance URI", example = PROVENANCE_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) throws URISyntaxException, NamingException, IOException, ParseException, Exception {
        ProvenanceDAO dao = new ProvenanceDAO(nosql, sparql);

        //check if the provenance can be deleted (not linked to data)
        List<URI> provenances = new ArrayList();
        provenances.add(uri);
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        ListWithPagination<DataModel> dataList = dataDAO.search(
                currentUser,
                null,
                null,
                null,
                provenances,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                1
        );
        
        ListWithPagination<DataFileModel> datafilesList = dataDAO.searchFiles(
                currentUser,
                null,
                null,
                null,
                provenances,
                null,
                null,
                null,
                null,
                null,
                0,
                1
        );

        if (dataList.getTotal() > 0 || datafilesList.getTotal() > 0) {
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
            credentialId = DataAPI.CREDENTIAL_DATA_MODIFICATION_ID,
            credentialLabelKey = DataAPI.CREDENTIAL_DATA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Provenance updated", response = ObjectUriResponse.class)
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

                ErrorResponse error = checkAgents(dto.getAgents());
                if (error != null) {
                    return error.getResponse();
                }
            }
            ProvenanceDAO dao = new ProvenanceDAO(nosql, sparql);
            ProvenanceModel newProvenance = dto.newModel();
            newProvenance = dao.update(newProvenance);
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
        ProvenanceDAO dao = new ProvenanceDAO(nosql, sparql);
        List<ProvenanceModel> models = dao.getListByURIs(uris);

        List<ProvenanceGetDTO> resultDTOList = new ArrayList<>(models.size());
        models.forEach(result -> {
            resultDTOList.add(ProvenanceGetDTO.fromModel(result));
        });

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    private ErrorResponse checkActivityTypes(List<ActivityCreationDTO> activities) throws URISyntaxException, Exception {
        
        Set<URI> checkedActivityTypes = new HashSet<>();
        Set<URI> notActivityTypes = new HashSet<>();
        
        for (ActivityCreationDTO activity:activities) {
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
                    "wrong activity rdf_types: " + notActivityTypes.toString()
            );
        } else {
            return null;
        }
        
    }

    private ErrorResponse checkAgents(List<AgentModel> dtos) throws URISyntaxException, Exception {
        //TODO replace FOAF.Agent by Oeso.Operator
        List<URI> agentTypes = Arrays.asList(new URI(Oeso.Device.toString()), new URI(FOAF.Agent.toString()));
        
        Set<URI> checkedAgentTypes = new HashSet<>();
        Set<URI> notAgentTypes = new HashSet<>();
        Set<URI> notAgentUris = new HashSet<>();
        List<URI> uris = new ArrayList();
        
        for (AgentModel dto:dtos) {
            uris.add(dto.getUri());
            if (!checkedAgentTypes.contains(dto.getRdfType())) {  
                //TODO replace FOAF.Agent by Oeso.Operator                
                boolean isAgent = sparql.executeAskQuery(new AskBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(dto.getRdfType()), Ontology.subClassAny, SPARQLDeserializers.nodeURI(FOAF.Agent.toString()))
                );

                if (!isAgent) {
                    isAgent = sparql.executeAskQuery(new AskBuilder()
                    .addWhere(SPARQLDeserializers.nodeURI(dto.getRdfType()), Ontology.subClassAny, SPARQLDeserializers.nodeURI(Oeso.Device.toString()))
                    );
                }

                if (!isAgent) {
                    notAgentTypes.add(dto.getRdfType());
                }
                checkedAgentTypes.add(dto.getRdfType());
            }            
        }
        
        if (!notAgentTypes.isEmpty()) {
            return new ErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "wrong agent rdfTypes",
                    "wrong agent rdfTypes: " + notAgentTypes.toString()
            );
        } else {
            if (!uris.isEmpty()) {
                OntologyDAO dao = new OntologyDAO(sparql);
                List<URITypesModel> checkedTypes = dao.checkURIsTypes(uris, agentTypes);

                for (URITypesModel model:checkedTypes) {
                    if (model.getRdfTypes().isEmpty()) {
                        notAgentUris.add(model.getUri());
                    }
                }

                if (!notAgentUris.isEmpty()) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "Agent uris don't exist or given uris are not agents",
                            "wrong agent uris: " + notAgentUris.toString()
                    );
                }
            }
        }
        return null;
    }
    
}
