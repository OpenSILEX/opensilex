//******************************************************************************
//                          StudiesSearchAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.StudyDTO;
import org.opensilex.brapi.model.StudyDetailsDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.2">BrAPI documentation</a>
 * @author Alice Boizet
 */
@Api("BRAPI")
@Path("/brapi/")
public class StudiesSearchAPI extends BrapiCall {
    
    @Inject
    private SPARQLService sparql;    
    
    @Inject
    private MongoDBService nosql;
        
    @Inject
    private FileStorageService fs;
    
    @CurrentUser
    AccountModel currentUser;
    
    @GET
    @Path("v1/studies-search")
    @BrapiVersion("1.2")
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
            switch (sortBy) {
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
