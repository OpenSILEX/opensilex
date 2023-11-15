package org.opensilex.faidare.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.faidare.builder.Faidarev1LocationDTOBuilder;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.faidare.responses.Faidarev1LocationListResponse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    public Response getVariablesList() throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql, nosql);
        FacilityDAO facilityDAO = new FacilityDAO(sparql, nosql, organizationDAO);

        FacilitySearchFilter filter = new FacilitySearchFilter().setUser(currentUser);
        ListWithPagination<FacilityModel> facilities = facilityDAO.search(filter);

        Faidarev1LocationDTOBuilder locationDTOBuilder = new Faidarev1LocationDTOBuilder(facilityDAO, organizationDAO);
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
