/*
 * *****************************************************************************
 *                         LocationsAPI.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 15/07/2024 14:06
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.faidare.builder.Faidarev1LocationDTOBuilder;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.faidare.responses.Faidarev1LocationListResponse;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

/**
 * @author Gabriel Besombes
 */
@Tag(name = CallsAPI.CREDENTIAL_CALLS_GROUP_ID)
@Path("/faidare/")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
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
    @Operation(summary = "Faidarev1CallDTO to retrieve a list of locations available in the system",
            description = "retrieve locations information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "retrieve locations information", content = @Content(schema = @Schema(implementation = Faidarev1LocationListResponse.class)))})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocationsList(
            @Parameter(description = "Search by Location") @QueryParam("locationDbId") URI locationDbId,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        OrganizationDAO organizationDAO = new OrganizationDAO(sparql);
        FacilityLogic facilityLogic = new FacilityLogic(sparql, nosql.getServiceV2());

        Faidarev1LocationDTOBuilder locationDTOBuilder = new Faidarev1LocationDTOBuilder(facilityLogic, organizationDAO);
        if (locationDbId != null && facilityLogic.get(locationDbId, currentUser) == null) {
            throw new NotFoundURIException(locationDbId);
        } else {
            FacilitySearchFilter filter = new FacilitySearchFilter()
                    .setUser(currentUser);
            if (locationDbId != null) {
                filter.setFacilities(Collections.singletonList(locationDbId));
            }
            filter.setPage(page)
                    .setPageSize(pageSize);
            ListWithPagination<FacilityModel> facilities = facilityLogic.search(filter);

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
