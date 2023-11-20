package org.opensilex.faidare.api;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.faidare.builder.Faidarev1LocationDTOBuilder;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.faidare.responses.Faidarev1LocationListResponse;
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
 * @author Gabriel Besombes
 */
@Api("faidare")
@Path("/faidare/")
public class LocationsAPI extends FaidareCall {

    @Inject
    private SPARQLService sparql;
    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;


    @GET
    @Path("v1/locations")
    @FaidareVersion("1.3")
    @ApiOperation(value = "Faidarev1CallDTO to retrieve a list of locations available in the system",
            notes = "retrieve locations information")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "retrieve locations information", response = Faidarev1LocationDTO.class)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariablesList(
            @ApiParam(value = "Search by Location") @QueryParam("locationDbId") URI locationDbId,
            @ApiParam(value = "Name of the field to sort by: locationDbId") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "Sort order direction - ASC or DESC") @QueryParam("sortOrder") String sortOrder,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        ArrayList<OrderBy> orderByList = new ArrayList<>();

        if (!StringUtils.isEmpty(sortBy)) {
            if ("locationDbId".equals(sortBy)) {
                sortBy = "uri";
            } else {
                sortBy = "";
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

        Faidarev1LocationDTOBuilder locationDTOBuilder = new Faidarev1LocationDTOBuilder(facilityDAO, organizationDAO);
        if (locationDbId != null && facilityDAO.get(locationDbId, currentUser) == null) {
            throw new NotFoundURIException(locationDbId);
        } else if (locationDbId != null) {
            return new SingleObjectResponse<>(
                    locationDTOBuilder.fromModel(
                            facilityDAO.get(locationDbId, currentUser),
                            currentUser
                    )
            ).getResponse();
        } else {
            FacilitySearchFilter filter = new FacilitySearchFilter()
                    .setUser(currentUser);
            filter.setOrderByList(orderByList)
                    .setPage(page)
                    .setPageSize(pageSize);
            ListWithPagination<FacilityModel> facilities = facilityDAO.search(filter);

            ListWithPagination<Faidarev1LocationDTO> resultDTOList = facilities.convert(
                    Faidarev1LocationDTO.class,
                    facilityModel -> {
                        try {
                            return locationDTOBuilder.fromModel(facilityModel, currentUser);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            return new Faidarev1LocationListResponse(resultDTOList).getResponse();
        }
    }
}
