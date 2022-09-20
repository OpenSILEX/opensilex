//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.*;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.EntityModel;
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
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensilex.core.variable.api.VariableAPI.*;

@Api(CREDENTIAL_VARIABLE_GROUP_ID)
@Path(EntityAPI.PATH)
@ApiCredentialGroup(
        groupId = VariableAPI.CREDENTIAL_VARIABLE_GROUP_ID,
        groupLabelKey = VariableAPI.CREDENTIAL_VARIABLE_GROUP_LABEL_KEY
)
public class EntityAPI {

    public static final String PATH = "/core/entities";

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel currentUser;

    public static int levenshteinDistance(String x, String y) {
        if (x.isEmpty()) {
            return y.length();
        }

        if (y.isEmpty()) {
            return x.length();
        }

        int substitution = levenshteinDistance(x.substring(1), y.substring(1))
                + (x.charAt(0) == y.charAt(0) ? 0 : 1);
        int insertion = levenshteinDistance(x, y.substring(1)) + 1;
        int deletion = levenshteinDistance(x.substring(1), y) + 1;

        return Math.min(Math.min(substitution, insertion),deletion);
    }

    @POST
    @ApiOperation("Add an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "An entity is created", response = ObjectUriResponse.class),
            @ApiResponse(code = 409, message = "An entity with the same URI already exists", response = ErrorResponse.class),
    })
    public Response createEntity(
            @ApiParam("Entity description") @Valid EntityCreationDTO dto
    ) throws Exception {
        try {
            BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
            EntityModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED,shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Entity already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get an entity")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity retrieved", response = EntityDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown entity URI", response = ErrorResponse.class)
    })
    public Response getEntity(
            @ApiParam(value = "Entity URI", example = "http://opensilex.dev/set/variables/entity/Plant", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {

        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
        EntityModel model = dao.get(uri);
        if (model != null) {
            return new SingleObjectResponse<>(new EntityDetailsDTO(model)).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get detailed entities by uris")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return entities", response = EntityDetailsDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Entity not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getEntitiesByURIs(
            @ApiParam(value = "Entities URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        
        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);

        try {
            List<EntityDetailsDTO> resultDTOList = dao.getList(uris)
                    .stream()
                    .map(EntityDetailsDTO::new)
                    .collect(Collectors.toList());

            return new PaginatedListResponse<>(resultDTOList).getResponse();

        }catch (SPARQLInvalidUriListException e){
            return new ErrorResponse(Response.Status.NOT_FOUND, "Entities not found", e.getStrUris()).getResponse();
        }

    }
    
    
    @PUT
    @ApiOperation("Update an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity updated", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown entity URI", response = ErrorResponse.class)
    })
    public Response updateEntity(
            @ApiParam("Entity description") @Valid EntityUpdateDTO dto
    ) throws Exception {
        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);

        EntityModel model = dto.newModel();
        dao.update(model);
        URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
        return new ObjectUriResponse(Response.Status.OK,shortUri).getResponse();
    }

    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an entity")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_VARIABLE_DELETE_ID,
            credentialLabelKey = CREDENTIAL_VARIABLE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown entity URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEntity(
            @ApiParam(value = "Entity URI", example = "http://opensilex.dev/set/variables/entity/Plant", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }

    @GET
    @ApiOperation("Search entities by name")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return entities", response = EntityGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchEntities(
            @ApiParam(value = "Name (regex)", example = "plant") @QueryParam("name") String namePattern ,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @Min(0) int pageSize
    ) throws Exception {

        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
        ListWithPagination<EntityModel> resultList = dao.search(
                namePattern,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<EntityGetDTO> resultDTOList = resultList.convert(
                EntityGetDTO.class,
                EntityGetDTO::new
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @GET
    @Path("duplicates/{uri}")
    @ApiOperation("Get entity duplicates")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity retrieved", response = VariableDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown entity URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEntityDuplicates(
            @ApiParam(value = "Entity URI", example = "http://purl.obolibrary.org/obo/ENVO_00002005", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "Sensitivity", example = "2", required = true) @QueryParam("sensitivity") @Min(0) int sensitivity
    ) throws Exception {
        BaseVariableDAO<EntityModel> dao = new BaseVariableDAO<>(EntityModel.class, sparql);
        EntityModel model = dao.get(uri);
        List<EntityDuplicatesDTO> resultDTOList = new ArrayList<>();

        if (model != null) {

            // récupération du nom de l'entité en entrée
            String entityName = model.getName().toLowerCase();

            BaseVariableDAO<EntityModel> entityDao = new BaseVariableDAO<>(EntityModel.class, sparql);
            // recherche de toutes les entités de la base de données
            List<EntityModel> resultList = entityDao.searchWithoutPagination(
                    null
            );

            for(EntityModel entity : resultList) {
                String entityNameI = entity.getName().toLowerCase();
                int firstParenthesis = 0;
                int lastParenthesis = 0;
                for(int j = 0 ; j < entityNameI.length() ; j++){
                    if(entityNameI.charAt(j)  == '('){
                        firstParenthesis = j;
                    }
                    if(entityNameI.charAt(j)  == ')'){
                        lastParenthesis = j;
                    }
                }
                if(firstParenthesis != 0 && lastParenthesis != 0){
                    entityNameI = entityNameI.substring(0,firstParenthesis-1);
                }

                int levenshteinDistance = levenshteinDistance(entityNameI,entityName);

                // si le nom est proche ou contient le nom entier de la métadonnée en entrée
                if(levenshteinDistance <= sensitivity){
                    EntityDuplicatesDTO entityDto = new EntityDuplicatesDTO(entity);
                    entityDto.setLevenshtein(levenshteinDistance);
                    resultDTOList.add(entityDto);
                }else{
                    if(entityNameI.contains(entityName)){
                        EntityDuplicatesDTO entityDto = new EntityDuplicatesDTO(entity);
                        entityDto.setContainedIn(true);
                        resultDTOList.add(entityDto);
                    }
                }

            }

            ListWithPagination<EntityDuplicatesDTO> paginatedDTOList = new ListWithPagination<>(resultDTOList);

            return new PaginatedListResponse<>(paginatedDTOList).getResponse();
        } else {
            throw new NotFoundURIException(uri);
        }
    }
}
