/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.core.infrastructure.dal.InfrastructureDAO;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author vidalmor
 */
@Api(InfrastructureAPI.CREDENTIAL_GROUP_INFRASTRUCTURE_ID)
@Path("/core/infrastructure")
public class InfrastructureAPI {

    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_ID = "Infrastructures";
    public static final String CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY = "credential-groups.infrastructures";

    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID = "infrastructure-modification";
    public static final String CREDENTIAL_INFRASTRUCTURE_MODIFICATION_LABEL_KEY = "credential.infrastructure.modification";

    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_ID = "infrastructure-delete";
    public static final String CREDENTIAL_INFRASTRUCTURE_DELETE_LABEL_KEY = "credential.infrastructure.delete";

    public static final String CREDENTIAL_INFRASTRUCTURE_READ_ID = "infrastructure-read";
    public static final String CREDENTIAL_INFRASTRUCTURE_READ_LABEL_KEY = "credential.infrastructure.read";

    private final SPARQLService sparql;

    /**
     * Inject SPARQL service
     */
    @Inject
    public InfrastructureAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    @GET
    @Path("search")
    @ApiOperation("Get a group by it's URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_GROUP_INFRASTRUCTURE_ID,
            groupLabelKey = CREDENTIAL_GROUP_INFRASTRUCTURE_LABEL_KEY,
            credentialId = CREDENTIAL_INFRASTRUCTURE_READ_ID,
            credentialLabelKey = CREDENTIAL_INFRASTRUCTURE_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Return Infrastructure list", response = InfrastructureGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)        
    })
    public Response searchInfrastructures(
            @ApiParam(value = "Regex pattern for filtering list by names", example = ".*") @DefaultValue(".*") @QueryParam("pattern") String pattern,
            @ApiParam(value = "Parent infrastructure URI") @QueryParam("parent") URI parent,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number") @QueryParam("page") int page,
            @ApiParam(value = "Page size") @QueryParam("pageSize") int pageSize,
            @Context SecurityContext securityContext
    ) throws Exception {
        UserModel user = (UserModel) securityContext.getUserPrincipal();
        InfrastructureDAO dao = new InfrastructureDAO(sparql);
        
        ListWithPagination<InfrastructureModel> model = dao.search(
                pattern,
                parent,
                user.getUri(),
                user.getLang(),
                orderByList,
                page,
                pageSize
        );

        return new PaginatedListResponse<>(model).getResponse();
    }

}
