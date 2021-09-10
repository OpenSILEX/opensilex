//******************************************************************************
//                          GermplasmAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.brapi.model.Call;
import org.opensilex.brapi.model.GermplasmDTO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

/**
 * @see BrAPI documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * The BrAPI germplasm corresponds to an accession in OpenSILEX
 * @author Alice BOIZET
 */
@Api("BRAPI")
@Path("/brapi/v1/")
public class GermplasmAPI implements BrapiCall {
    
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> call1Methods = new ArrayList<>();
        call1Methods.add("GET");
        ArrayList<String> call1Versions = new ArrayList<>();
        call1Versions.add("1.3");
        Call call1 = new Call("germplasm", calldatatypes, call1Methods, call1Versions);
       
        calls.add(call1);
        
        return calls;
    }    
    
    @Inject
    private SPARQLService sparql;
    
    @Inject
    private MongoDBService nosql;
    
    @CurrentUser
    UserModel currentUser;
    
    @GET
    @Path("germplasm")
    @ApiOperation("Submit a search request for germplasm")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = GermplasmDTO.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response getGermplasmBySearch(
            @ApiParam(value = "Search by germplasmDbId") @QueryParam("germplasmDbId") URI uri,
            @ApiParam(value = "Search by germplasmPUI") @QueryParam("germplasmPUI") URI germplasmPUI,
            @ApiParam(value = "Search by germplasmName") @QueryParam("germplasmName") String germplasmName,
            @ApiParam(value = "Search by commonCropName") @QueryParam("commonCropName") String commonCropName,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        GermplasmDAO germplasmDAO = new GermplasmDAO(sparql, nosql);

        if (germplasmPUI != null && uri == null) {
            uri = germplasmPUI;
        }
        
        ListWithPagination<GermplasmModel> resultList = germplasmDAO.brapiSearch(
                currentUser,
                uri,
                germplasmName,
                commonCropName,
                page,
                pageSize
        );
        
        // Convert paginated list to DTO
        ListWithPagination<GermplasmDTO> resultDTOList = resultList.convert(
                GermplasmDTO.class,
                GermplasmDTO::fromModel
        );
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
    
}
