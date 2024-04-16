//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.germplasmGroup.api;

import io.swagger.annotations.*;
import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.germplasm.api.GermplasmGetAllDTO;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupDAO;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;
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

import static org.opensilex.core.germplasmGroup.api.GermplasmGroupApi.PATH;


/**
 * @author Maximilian HART
 */

@Api(GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_ID)
@Path(PATH)
@ApiCredentialGroup(
        groupId = GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_ID,
        groupLabelKey = GermplasmAPI.CREDENTIAL_GERMPLASM_GROUP_LABEL_KEY
)
public class GermplasmGroupApi {
    public static final String PATH = "/core/germplasm_group";

    public static final String GROUP_EXAMPLE_URI = "opensilex-sandbox:id/germplasmGroup/test";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @POST
    @ApiOperation("Add a germplasm group")
    @ApiProtected
    @ApiCredential(
            credentialId = GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A germplasm group is created", response = URI.class),
            @ApiResponse(code = 409, message = "A germplasm group with the same URI already exists", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGermplasmGroup(@ApiParam("Germplasm group description") @Valid GermplasmGroupCreationDTO dto) throws Exception {
        try {
            GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
            GermplasmGroupModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());

            model = dao.create(model);
            URI shortUri = new URI(SPARQLDeserializers.getShortURI(model.getUri().toString()));
            return new ObjectUriResponse(Response.Status.CREATED, shortUri).getResponse();

        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Germplasm group already exists", duplicateUriException.getMessage()).getResponse();
        }
    }

    @POST
    @Path("search")
    @ApiOperation(value = "Search germplasm groups")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return germplasm groups", response = GermplasmGroupGetDTO.class, responseContainer = "List")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchGermplasmGroups(
            @ApiParam(value = "Regex pattern for filtering by name") @QueryParam("name") String name ,
            @ApiParam(value = "Germplasm URIs", example = "http://aims.fao.org/aos/agrovoc/c_1066") @QueryParam("germplasm") @ValidURI List<URI> germplasm,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        AccountDAO accountDAO = new AccountDAO(sparql);
        ListWithPagination<GermplasmGroupModel> resultList = dao.search(
                name,
                germplasm,
                orderByList,
                page,
                pageSize,
                currentUser.getLanguage()
        );

        ListWithPagination<GermplasmGroupGetDTO> resultDTOList = resultList.convert(
                GermplasmGroupGetDTO.class,
                (model)->{
                    GermplasmGroupGetDTO next = GermplasmGroupGetDTO.fromModel(model);
                    try {
                        next.setGermplasmCount(germplasmDAO.countInGroup(next.getUri(), currentUser.getLanguage()));
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
    @ApiOperation("Get a germplasm group")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Germplasm group retrieved", response = GermplasmGroupGetDTO.class),
            @ApiResponse(code = 404, message = "Unknown germplasm group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGermplasmGroup(
            @ApiParam(value = "Germplasm group URI", required = true) @PathParam("uri") @NotNull URI uri
            ) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        AccountDAO accountDAO = new AccountDAO(sparql);
        GermplasmGroupModel model = dao.get(uri, currentUser.getLanguage(), false);
        if(model!=null){
            GermplasmGroupGetDTO result = GermplasmGroupGetDTO.fromModel(model);
            try {
                result.setGermplasmCount(germplasmDAO.countInGroup(result.getUri(), currentUser.getLanguage()));
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
    @Path("with-germplasm/{uri}")
    @ApiOperation("Get a germplasm group with nested germplasm details")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Germplasm group retrieved", response = GermplasmGroupGetWithDetailsDTO.class),
            @ApiResponse(code = 404, message = "Unknown germplasm group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGermplasmGroupWithGermplasms(
            @ApiParam(value = "Germplasm group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        GermplasmGroupModel model = dao.get(uri, currentUser.getLanguage(), true);
        if(model!=null){
            return new SingleObjectResponse<>(GermplasmGroupGetWithDetailsDTO.fromModel(model)).getResponse();
        }else {
            throw new NotFoundURIException(uri);
        }
    }

    @GET
    @Path("{uri}/germplasm")
    @ApiOperation("Get a germplasm group's germplasm, paginated")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Germplasm group content retrieved", response = GermplasmGetAllDTO.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Unknown germplasm group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGermplasmGroupContent(
            @ApiParam(value = "Germplasm group URI", required = true) @PathParam("uri") @NotNull URI uri,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter = filter.setGroup(uri);

        filter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize)
                .setLang(currentUser.getLanguage());

        ListWithPagination<GermplasmModel> resultList = germplasmDAO.search(
                filter,false, false
        );

        // Convert paginated list to DTO
        ListWithPagination<GermplasmGetAllDTO> resultDTOList = resultList.convert(
                GermplasmGetAllDTO.class,
                GermplasmGetAllDTO::fromModel
        );

        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }


    @GET
    @Path("by-uris")
    @ApiOperation("Get germplasm groups by their URIs")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return germplasm groups", response = GermplasmGroupGetDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Germplasm group not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGermplasmGroupByURIs(
            @ApiParam(value = "Germplasm group URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);
        List<GermplasmGroupModel> models = dao.getList(uris);
        if (!models.isEmpty()) {
            List<GermplasmGroupGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                GermplasmGroupGetDTO next = GermplasmGroupGetDTO.fromModel(result);
                try {
                    next.setGermplasmCount(germplasmDAO.countInGroup(next.getUri(), currentUser.getLanguage()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                resultDTOList.add(next);
            });
            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            return new ErrorResponse(Response.Status.NOT_FOUND, "Germplasm group not found", "Unknown germplasm group URI").getResponse();
        }
    }
    
    
    @PUT
    @ApiOperation("Update a germplasm group")
    @ApiProtected
    @ApiCredential(
            credentialId = GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_ID,
            credentialLabelKey = GermplasmAPI.CREDENTIAL_GERMPLASM_MODIFICATION_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Germplasm group updated", response = URI.class),
            @ApiResponse(code = 404, message = "Unknown germplasm group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGermplasmGroup(@ApiParam("Germplasm group description") @Valid GermplasmGroupUpdateDTO dto) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        GermplasmGroupModel model = dao.update(dto.newModel());
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }
    
    
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a germplasm group")
    @ApiProtected
    @ApiCredential(
            credentialId = GermplasmAPI.CREDENTIAL_GERMPLASM_DELETE_ID,
            credentialLabelKey = GermplasmAPI.CREDENTIAL_GERMPLASM_DELETE_LABEL_KEY
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Germplasm group deleted", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "Unknown germplasm group URI", response = ErrorResponse.class)
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGermplasmGroup(
            @ApiParam(value = "Germplasm group URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        GermplasmGroupDAO dao = new GermplasmGroupDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(uri).getResponse();
    }
}



    

