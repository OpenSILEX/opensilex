//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.unit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.opensilex.core.CoreModule;
import org.opensilex.core.sharedResource.SharedResourcesFunctions;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.UnitModel;
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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensilex.core.variable.api.VariableAPI.*;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(UnitAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class UnitAPI {

    public static final String PATH = "/core/units";

    @Inject
    private SPARQLService sparql;

    @Inject
    private CoreModule coreModule;

    @Context
    protected HttpServletRequest httpRequest;

    @CurrentUser
    UserModel currentUser;

    @POST
    @ApiOperation("Add an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "An unit is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An unit with the same URI already exists", response = ErrorResponse.class),
    })
    public Response createUnit(
            @ApiParam("Unit description") @Valid UnitCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
            UnitModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Unit already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an unit")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit retrieved", response = UnitDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    public Response getUnit(
            @ApiParam(value = "Unit URI", example = "http://opensilex.dev/set/variables/unit/Centimeter", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
        UnitModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(new UnitDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed units by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return units", response = UnitDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Unit not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getUnitsByURIs(
            @ApiParam(value = "Units URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris,
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource
    ) throws Exception {

        if (resource == null) {
            BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);

            try {
                List<UnitDetailsDTO> resultDTOList = dao.getList(uris)
                        .stream()
                        .map(UnitDetailsDTO::new)
                        .collect(Collectors.toList());

                return new PaginatedListResponse<>(resultDTOList).getResponse();

            } catch (SPARQLInvalidUriListException e) {
                return new ErrorResponse(Response.Status.NOT_FOUND, "Units not found", e.getStrUris()).getResponse();
            }
        }else{
                SharedResourcesFunctions sharedResourcesFunctions = SharedResourcesFunctions.getInstance(coreModule);

                // construction de l'adresse du service avec l'uri encodé de chaque unité
                String token = sharedResourcesFunctions.getToken(resource.toString());
                String urlService = resource.toString() + "/core/units/by_uris?";
                Boolean firstUri = true;

                for (URI uri : uris) {
                    if (firstUri) {
                        urlService += "uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                        firstUri = false;
                    } else {
                        urlService += "&uris=" + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name());
                    }
                }
                List<UnitDetailsDTO> resultDTOList = new ArrayList<>();
                // utilisation du service de recherche des unités en fonction de leur uri sur la ressource partagée
                String stringResponse = sharedResourcesFunctions.connectionToService(urlService, token);
                JsonNode jsonResult;
                if (stringResponse != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    jsonResult = mapper.readTree(stringResponse);
                    SingleObjectResponse<List<UnitDetailsDTO>> getResponse = mapper.convertValue(jsonResult, new TypeReference<SingleObjectResponse<List<UnitDetailsDTO>>>() {});
                    resultDTOList = getResponse.getResult();

                    return new PaginatedListResponse<>(resultDTOList).getResponse();

                }else{
                    return new ErrorResponse(Response.Status.NOT_FOUND, "Units not found", "Unknown unit URIs").getResponse();
                }
        }
    }
    
    
    @PUT
    @ApiOperation("Update an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    public Response updateUnit(
            @ApiParam("Unit description") @Valid UnitUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);

        UnitModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an unit")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Unit deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown unit URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUnit(
            @ApiParam(value = "Unit URI", example = "http://opensilex.dev/set/variables/unit/Centimeter", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }


    @GET
    @ApiOperation("Search units by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return Unit list", response = UnitGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUnits(
            @ApiParam(value = "Name (regex)", example = "Centimeter") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Resource") @QueryParam("resource") URI resource,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        if (resource == null) {
            BaseVariableDAO<UnitModel> dao = new BaseVariableDAO<>(UnitModel.class, sparql);
            ListWithPagination<UnitModel> resultList = dao.search(
                    namePattern,
                    orderByList,
                    page,
                    pageSize
            );

            ListWithPagination<UnitGetDTO> resultDTOList = resultList.convert(
                    UnitGetDTO.class,
                    UnitGetDTO::new
            );
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }else{
            SharedResourcesFunctions sharedResourcesFunctions = SharedResourcesFunctions.getInstance(coreModule);

            UriBuilder url = UriBuilder.fromUri(resource)
                    .path(PATH);
            // récupération des paramètres de la requête pour les recopier dans l'appel au service de recherche d'unité sur la RP sélectionnée
            for (Map.Entry<String, String[]> entry : httpRequest.getParameterMap().entrySet()){
                url.queryParam(entry.getKey(),entry.getValue());
            }

            // utilisation du service de recherche des unités sur la ressource partagée
            String token = sharedResourcesFunctions.getToken(resource.toString());
            String SearchResponse = sharedResourcesFunctions.connectionToService(url.toString(), token);
            ObjectMapper mapperSearch = new ObjectMapper();
            JsonNode jsonResultSearch = mapperSearch.readTree(SearchResponse);

            // conversion du résultat en liste de dtos
            SingleObjectResponse<List<UnitGetDTO>> getResponse = mapperSearch.convertValue(jsonResultSearch, new TypeReference<SingleObjectResponse<List<UnitGetDTO>>>() {});
            List<UnitGetDTO> dtoFromApi = getResponse.getResult();

            // récupération du nombre total de unités pour la pagination
            MetadataDTO metadata = getResponse.getMetadata();
            long totalCount = metadata.getPagination().getTotalCount();
            int totalUnits = (int)totalCount;

            ListWithPagination<UnitGetDTO> resultDTOList = new ListWithPagination<>(dtoFromApi,page,pageSize, totalUnits);

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        }
    }
}
