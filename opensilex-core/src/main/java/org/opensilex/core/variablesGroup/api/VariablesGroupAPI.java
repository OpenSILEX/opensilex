//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.net.URI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.UriBuilder;

import org.opensilex.core.CoreModule;
import org.opensilex.core.sharedResource.SharedResourcesFunctions;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupDAO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.core.variable.api.VariableAPI;

import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;

import org.opensilex.server.response.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.service.SPARQLService;

import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
/**
 * @author Hamza IKIOU
 */

@Api(VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID)
@Path(VariablesGroupAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class VariablesGroupAPI {
    
    public static final String PATH = "/core/variables_group";
    
    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;
    
    @POST
    @ApiOperation("Add a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A variables group is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "A variables group with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)   
    public Response createVariablesGroup(@ApiParam("Variables group description") @Valid VariablesGroupCreationDTO dto) throws Exception {
        try {
            VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
            VariablesGroupModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED, shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Variables group already exists", duplicateUriException.getMessage()).getResponse();
        }
    }
    
    
    
    @GET
    @ApiOperation(value = "Search variables groups")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return variables groups", response = VariablesGroupGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVariablesGroups(
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name ,
            @ApiParam(value = "Variable URI") @QueryParam("variableUri") URI variableUri ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        if (resource == null) {
            VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
            ListWithPagination<VariablesGroupModel> resultList = dao.search(
                    name,
                    variableUri,
                    orderByList,
                    page,
                    pageSize
            );

            ListWithPagination<VariablesGroupGetDTO> resultDTOList = resultList.convert(
                    VariablesGroupGetDTO.class,
                    VariablesGroupGetDTO::fromModel
            );
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }else{
            SharedResourcesFunctions sharedResourcesFunctions = SharedResourcesFunctions.getInstance(coreModule);

            UriBuilder url = UriBuilder.fromUri(resource)
                    .path(PATH);
            // récupération des paramètres de la requête pour les recopier dans l'appel au service de recherche des groupes de variables sur la RP sélectionnée
            for (Map.Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()){
                url.queryParam(entry.getKey(),entry.getValue());
            }

            // utilisation du service de recherche des groupes de variables sur la ressource partagée
            String token = sharedResourcesFunctions.getToken(resource.toString());
            String SearchResponse = sharedResourcesFunctions.connectionToService(url.toString(), token);
            ObjectMapper mapperSearch = new ObjectMapper();
            JsonNode jsonResultSearch = mapperSearch.readTree(SearchResponse);

            // conversion du résultat en liste de dtos
            SingleObjectResponse<List<VariablesGroupGetDTO>> getResponse = mapperSearch.convertValue(jsonResultSearch, new TypeReference<SingleObjectResponse<List<VariablesGroupGetDTO>>>() {});
            List<VariablesGroupGetDTO> dtoFromApi = getResponse.getResult();

            // récupération du nombre total de groupes de variables pour la pagination
            MetadataDTO metadata = getResponse.getMetadata();
            long totalCount = metadata.getPagination().getTotalCount();
            int totalVariablesGroups = (int)totalCount;

            ListWithPagination<VariablesGroupGetDTO> resultDTOList = new ListWithPagination<>(dtoFromApi,page,pageSize, totalVariablesGroups);

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }
    }    
    
 
    
   
    @GET
    @Path("{uri}")
    @ApiOperation("Get a variables group")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group retrieved", response = VariablesGroupGetDTO.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroup(@ApiParam(value = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        VariablesGroupModel model = dao.get(uri);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }
        return new SingleObjectResponse<>(VariablesGroupGetDTO.fromModel(model)).getResponse();
    }
    

    
    @GET
    @Path("by_uris")
    @ApiOperation("Get variables groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return variables groups", response = VariablesGroupGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Variables group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getVariablesGroupByURIs(
            @ApiParam(value = "Variables group URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris,
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource
    ) throws Exception {

        if (resource == null) {
            VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
            List<VariablesGroupModel> models = dao.getList(uris);

            if (!models.isEmpty()) {
                List<VariablesGroupGetDTO> resultDTOList = new ArrayList<>(models.size());
                models.forEach(result -> resultDTOList.add(VariablesGroupGetDTO.fromModel(result)));

                return new PaginatedListResponse<>(resultDTOList).getResponse();
            } else {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Variables group not found", "Unknown variables group URIs or variables URIs").getResponse();
            }
        }else{
            SharedResourcesFunctions sharedResourcesFunctions = SharedResourcesFunctions.getInstance(coreModule);

            // construction de l'adresse du service avec l'uri encodé de chaque groupe de variables
            String token = sharedResourcesFunctions.getToken(resource.toString());
            String urlService = resource.toString() + "/core/variables_group/by_uris?";
            Boolean firstUri = true;

            for (URI uri : uris) {
                if (firstUri) {
                    urlService += "uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                    firstUri = false;
                } else {
                    urlService += "&uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                }
            }
            List<VariablesGroupGetDTO> resultDTOList = new ArrayList<>();
            // utilisation du service de recherche des groupes de variables en fonction de leur uri sur la ressource partagée
            String stringResponse = sharedResourcesFunctions.connectionToService(urlService, token);
            JsonNode jsonResult;
            if (stringResponse != null) {
                ObjectMapper mapper = new ObjectMapper();
                jsonResult = mapper.readTree(stringResponse);
                SingleObjectResponse<List<VariablesGroupGetDTO>> getResponse = mapper.convertValue(jsonResult, new TypeReference<SingleObjectResponse<List<VariablesGroupGetDTO>>>() {});
                resultDTOList = getResponse.getResult();

                return new PaginatedListResponse<>(resultDTOList).getResponse();

            }else {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Variables group not found", "Unknown variables group URIs or variables URIs").getResponse();
            }
        }
    }
   
    
    
    
    @PUT
    @ApiOperation("Update a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVariablesGroup(@ApiParam("Variables group description") @Valid VariablesGroupUpdateDTO dto) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        VariablesGroupModel model = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }
    
    
    
    
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a variables group")
    @ApiProtected
    @ApiCredential(
            credentialId = VariableAPI.CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = VariableAPI.CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Variables group deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown variables group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariablesGroup(
            @ApiParam(value = "Variables group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        VariablesGroupDAO dao = new VariablesGroupDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(uri).getResponse();
    }
}
