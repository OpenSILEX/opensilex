//******************************************************************************
//                          StudiesSearchAPI.java
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
import org.apache.commons.lang3.StringUtils;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.Call;
import org.opensilex.brapi.model.StudyDTO;
import org.opensilex.brapi.model.StudyDetailsDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.URL;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * @see Brapi documentation V1.2 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.2
 * @author Alice Boizet
 */
@Api("BRAPI")
@Path("/brapi/v1")
public class StudiesSearchAPI implements BrapiCall {
    
    @Inject
    private SPARQLService sparql;    
    
    @Inject
    private MongoDBService nosql;
        
    @Inject
    private FileStorageService fs;
    
    @CurrentUser
    UserModel currentUser;

    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.2");
        Call call1 = new Call("studies-search", calldatatypes, callMethods, callVersions);
        calls.add(call1);
        return calls;
    }
    
    @GET
    @Path("studies-search")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = StudyDTO.class, responseContainer = "List")})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudiesSearch(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
            @ApiParam(value = "Name of the field to sort by: studyDbId or seasonDbId") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "pageSize") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "page") @QueryParam("page") @DefaultValue("0") @Min(0) int page
    ) throws Exception {

        ArrayList<OrderBy> orderByList = new ArrayList();
        
        if (!StringUtils.isEmpty(sortBy)) {
            if (null == sortBy) {
                sortBy = "";
            } else switch (sortBy) {
                case "studyDbId":
                    sortBy = "uri";
                    break;
                case "seasonDbId":
                    sortBy = "campaign";
                    break;
                default:
                    sortBy = "";
                    break;
            }
            String orderByStr;
            if (!StringUtils.isEmpty(sortOrder)) {
                orderByStr = sortBy + "=" + sortOrder;
            } else {
                orderByStr = sortBy + "=" + "desc";
            }
            OrderBy order = new OrderBy(orderByStr);
            orderByList.add(order);
        }

        Boolean isEnded = !StringUtils.isEmpty(active) ? !Boolean.parseBoolean(active) : null;
        
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        if (studyDbId != null) {
            ExperimentModel model = xpDao.get(studyDbId, currentUser);  
            if (model != null) {
                return new SingleObjectResponse<>(StudyDetailsDTO.fromModel(model)).getResponse();
            } else {
                throw new NotFoundURIException(studyDbId);
            }
        } else {
            ExperimentSearchFilter filter = new ExperimentSearchFilter()
                    .setEnded(isEnded)
                    .setUser(currentUser);
            filter.setOrderByList(orderByList)
                    .setPage(page)
                    .setPageSize(pageSize);

            ListWithPagination<ExperimentModel> resultList = xpDao.search(filter);

            ListWithPagination<StudyDTO> resultDTOList = resultList.convert(StudyDTO.class, StudyDTO::fromModel);
            return new BrapiPaginatedListResponse<>(resultDTOList).getResponse();
        }
    }
}
