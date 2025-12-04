//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.geneticResourceGroup.api;

import io.swagger.annotations.*;
import org.opensilex.core.geneticResource.api.GeneticResourceAPI;
import org.opensilex.core.geneticResource.api.GeneticResourceGetAllDTO;
import org.opensilex.core.geneticResource.api.GeneticResourceSearchFilter;
import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupDAO;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
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
import java.util.Objects;

import static org.opensilex.core.geneticResourceGroup.api.GeneticResourceGroupApi.PATH;


/**
 * @author Maximilian HART
 */

@Api(GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_ID)
@Path(PATH)
@ApiCredentialGroup(
        groupId = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_ID,
        groupLabelKey = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_GROUP_LABEL_KEY
)
public class GeneticResourceGroupApi {
    public static final String PATH = "/core/geneticResource_group";

    public static final String GROUP_EXAMPLE_URI = "opensilex-sandbox:id/geneticResourceGroup/test";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @POST
    @ApiOperation("Add a geneticResource group")
    @ApiProtected
    @ApiCredential(
            credentialId = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID,
            credentialLabelKey = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A geneticResource group is created", response = URI.class),
            @ApiResponse(code = 409, message = "A geneticResource group with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGeneticResourceGroup(@ApiParam("GeneticResource group description") @Valid GeneticResourceGroupCreationDTO dto) throws Exception {
        try {
            GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
            GeneticResourceGroupModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED, shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "GeneticResource group already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @POST
    @Path("search")
    @ApiOperation(value = "Search geneticResource groups")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return geneticResource groups", response = GeneticResourceGroupGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchGeneticResourceGroups(
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name ,
            @ApiParam(value = "GeneticResource URIs", example = "http://aims.fao.org/aos/agrovoc/c_1066") @QueryParam("geneticResource") @ValidURI List<URI> geneticResource,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        GeneticResourceDAO geneticResourceDAO = new GeneticResourceDAO(sparql, nosql);
        AccountDAO accountDAO = new AccountDAO(sparql);
        ListWithPagination<GeneticResourceGroupModel> resultList = dao.search(
                name,
                geneticResource,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<GeneticResourceGroupGetDTO> resultDTOList = resultList.convert(
                GeneticResourceGroupGetDTO.class,
                (model)->{
                    GeneticResourceGroupGetDTO next = GeneticResourceGroupGetDTO.fromModel(model);
                    try {
                        next.setGeneticResourceCount(geneticResourceDAO.countInGroup(next.getUri(), currentUser.getLanguage()));
                        if (Objects.nonNull(model.getPublisher())) {
                            next.setPublisher(UserGetDTO.fromModel(accountDAO.get(model.getPublisher())));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return next;
                }
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get a geneticResource group")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "GeneticResource group retrieved", response = GeneticResourceGroupGetDTO.class),
            @ApiResponse(code = 404, message = "Unknown geneticResource group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneticResourceGroup(
            @ApiParam(value = "GeneticResource group URI", required = true) @PathParam("uri") @NotNull URI uri
            ) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        GeneticResourceDAO geneticResourceDAO = new GeneticResourceDAO(sparql, nosql);
        AccountDAO accountDAO = new AccountDAO(sparql);
        GeneticResourceGroupModel model = dao.get(uri, currentUser.getLanguage(), false);
        if(model!=null){
            GeneticResourceGroupGetDTO result = GeneticResourceGroupGetDTO.fromModel(model);
            try {
                result.setGeneticResourceCount(geneticResourceDAO.countInGroup(result.getUri(), currentUser.getLanguage()));
                if (Objects.nonNull(model.getPublisher())) {
                    result.setPublisher(UserGetDTO.fromModel(accountDAO.get(model.getPublisher())));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new SingleObjectResponse<>(result).getResponse();
        }else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("with-geneticResource/{uri}")
    @ApiOperation("Get a geneticResource group with nested geneticResource details")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "GeneticResource group retrieved", response = GeneticResourceGroupGetWithDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown geneticResource group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneticResourceGroupWithGeneticResources(
            @ApiParam(value = "GeneticResource group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        GeneticResourceGroupModel model = dao.get(uri, currentUser.getLanguage(), true);
        if(model!=null){
            return new SingleObjectResponse<>(GeneticResourceGroupGetWithDetailsDTO.fromModel(model)).getResponse();
        }else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("{uri}/geneticResource")
    @ApiOperation("Get a geneticResource group's geneticResource, paginated")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "GeneticResource group content retrieved", response = GeneticResourceGetAllDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown geneticResource group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneticResourceGroupContent(
            @ApiParam(value = "GeneticResource group URI", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GeneticResourceDAO geneticResourceDAO = new GeneticResourceDAO(sparql, nosql);
        GeneticResourceSearchFilter filter = new GeneticResourceSearchFilter();
        filter = filter.setGroup(uri);

        filter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        ListWithPagination<GeneticResourceModel> resultList = geneticResourceDAO.search(
                filter,false,  false
        );

        // Convert paginated list to DTO
        ListWithPagination<GeneticResourceGetAllDTO> resultDTOList = resultList.convert(
                GeneticResourceGetAllDTO.class,
                GeneticResourceGetAllDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @GET
    @Path("by-uris")
    @ApiOperation("Get geneticResource groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return geneticResource groups", response = GeneticResourceGroupGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "GeneticResource group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeneticResourceGroupByURIs(
            @ApiParam(value = "GeneticResource group URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        GeneticResourceDAO geneticResourceDAO = new GeneticResourceDAO(sparql, nosql);
        List<GeneticResourceGroupModel> models = dao.getList(uris);
        if (!models.isEmpty()) {
            List<GeneticResourceGroupGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                GeneticResourceGroupGetDTO next = GeneticResourceGroupGetDTO.fromModel(result);
                try {
                    next.setGeneticResourceCount(geneticResourceDAO.countInGroup(next.getUri(), currentUser.getLanguage()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                resultDTOList.add(next);
            });
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "GeneticResource group not found", "Unknown geneticResource group URI").getResponse();
        }
    }
    
    
    @PUT
    @ApiOperation("Update a geneticResource group")
    @ApiProtected
    @ApiCredential(
            credentialId = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID,
            credentialLabelKey = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "GeneticResource group updated", response = URI.class),
            @ApiResponse(code = 404, message = "Unknown geneticResource group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGeneticResourceGroup(@ApiParam("GeneticResource group description") @Valid GeneticResourceGroupUpdateDTO dto) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        GeneticResourceGroupModel model = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }
    
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a geneticResource group")
    @ApiProtected
    @ApiCredential(
            credentialId = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_DELETE_ID,
            credentialLabelKey = GeneticResourceAPI.CREDENTIAL_GENETIC_RESOURCE_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "GeneticResource group deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown geneticResource group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGeneticResourceGroup(
            @ApiParam(value = "GeneticResource group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        GeneticResourceGroupDAO dao = new GeneticResourceGroupDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(uri).getResponse();
    }
}



    

