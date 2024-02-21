//******************************************************************************
//                          StudiesAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.faidare.builder.Faidarev1StudyDTOBuilder;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.faidare.responses.Faidarev1StudyListResponse;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.exceptions.NotFoundURIException;
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
import java.util.Collections;
import java.util.Objects;

/**
 * @author Gabriel Besombes
 */
@Api("faidare")
@Path("/faidare/")
public class StudiesAPI extends FaidareCall {

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;


    @GET
    @Path("v1/studies")
    @FaidareVersion("1.3")
    @ApiOperation(value = "Retrieve studies information", notes = "Retrieve studies information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve studies information", response = Faidarev1StudyListResponse.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudiesList(
            @ApiParam(value = "Search by studyDbId") @QueryParam("studyDbId") URI studyDbId,
            @ApiParam(value = "Filter active status true/false") @QueryParam("active") String active,
            @ApiParam(value = "Name of the field to sort by: studyDbId, active") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);

        ArrayList<OrderBy> orderByList = new ArrayList<>();

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

        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);
        Faidarev1StudyDTOBuilder studyDTOBuilder = new Faidarev1StudyDTOBuilder(facilityDAO, organizationDAO);
        ListWithPagination<ExperimentModel> resultList;
        if (studyDbId != null) {
            ExperimentModel model = xpDao.get(studyDbId, currentUser);
            if (Objects.isNull(model)) {
                throw new NotFoundURIException(studyDbId);
            } else {
                resultList = new ListWithPagination<>(Collections.singletonList(model), 0, 0, 0);
            }
        } else {
            ExperimentSearchFilter filter = new ExperimentSearchFilter()
                    .setEnded(isEnded)
                    .setUser(currentUser);
            filter.setOrderByList(orderByList)
                    .setPage(page)
                    .setPageSize(pageSize);

            resultList = xpDao.search(filter);
        }

        ListWithPagination<Faidarev1StudyDTO> resultDTOList = resultList.convert(
                Faidarev1StudyDTO.class,
                experimentModel -> {
                    try {
                        return studyDTOBuilder.fromModel(
                                experimentModel,
                                currentUser);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return new Faidarev1StudyListResponse(resultDTOList).getResponse();
    }
}
