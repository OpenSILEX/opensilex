package org.opensilex.olga.api;

import io.swagger.annotations.*;
import org.brapi.client.v2.model.exceptions.ApiException;
import org.brapi.client.v2.model.queryParams.germplasm.GermplasmQueryParams;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIStatus;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmListResponse;
import org.brapi.v2.model.germ.response.BrAPIGermplasmSingleResponse;
import org.opensilex.olga.OlgaModule;
import org.opensilex.olga.bll.GermplasmLogic;
import org.opensilex.olga.model.GermplasmDTO;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.PaginatedListResponse;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.opensilex.olga.bll.GermplasmLogic.brapiResponseToSingleObjectResponse;
import static org.opensilex.olga.bll.GermplasmLogic.updateGermplasms;

/**
 * @author Gabriel Besombes
 */
@Api(GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_ID)
@Path(GermplasmAPI.PATH)
@ApiCredentialGroup(
        groupId = GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_ID,
        groupLabelKey = GermplasmAPI.CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY
)
public class GermplasmAPI {

    public static final String PATH = "/olga/germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_ID = "Olga Germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.olga-germplasm";

    public static final String GERMPLASM_EXAMPLE_DBID = "germplasm1";
    public static final String GERMPLASM_DBID = GERMPLASM_EXAMPLE_DBID;

    public static final String GERMPLASM_EXAMPLE_NAME = "germplasm1Name";
    public static final String GERMPLASM_NAME = GERMPLASM_EXAMPLE_DBID;

    @Inject
    private OlgaModule olgaModule;

    private GermplasmApi getAuthenticatedGarmplasmAPIClient() {
        return new GermplasmApi(olgaModule.getAuthenticatedBrAPIClient());
    }

    @GET
    @Path("{germplasmDbId}")
    @ApiOperation("Get olga germplasm by germplasmDbId")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return details of an olga germplasm", response = BrAPIGermplasm.class)
    })
    public Response getGermplasmByDbId(
            @ApiParam(value = GERMPLASM_DBID, example = GERMPLASM_EXAMPLE_DBID, required = true) @PathParam("germplasmDbId") @NotNull String germplasmDbId
    ) throws Exception {

        // Should some of this be in the business layer?
        GermplasmApi germplasmApi = getAuthenticatedGarmplasmAPIClient();
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmSingleResponse> germplasmDetail = germplasmApi.germplasmGermplasmDbIdGet(germplasmDbId);
        germplasmDetail.getBody().getResult().setAdditionalInfo(null);

        BrAPIGermplasm responseObject = germplasmDetail.getBody().getResult();
        BrAPIPagination responsePagintation = germplasmDetail.getBody().getMetadata().getPagination();
        List<BrAPIStatus> responseStatus = germplasmDetail.getBody().getMetadata().getStatus();

        return brapiResponseToSingleObjectResponse(responseObject, responsePagintation, responseStatus).getResponse();
    }

    @GET
    @Path("")
    @ApiOperation("Search germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return matching germplasms", response = GermplasmDTO.class, responseContainer = "List")
    })
    public Response searchGermplasms(
            @ApiParam(value = GERMPLASM_NAME, example = GERMPLASM_EXAMPLE_NAME) String germplasmName
    ) throws Exception {
        return new PaginatedListResponse<>(GermplasmLogic.searchGermplasm(germplasmName)).getResponse();
    }

    // Service to force germplasm harvest for admins
    @GET
    @Path("forceHarvest")
    @ApiOperation("Force harvest of olga germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Harvest olga germplasms")
    })
    public void forceHarvestGermplasms() throws Exception {
        harvestOlgaGermplasms();
    }

    private void harvestOlgaGermplasms() throws Exception {
        GermplasmApi germplasmApi = getAuthenticatedGarmplasmAPIClient();
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmListResponse> germplasms = germplasmApi.germplasmGet(new GermplasmQueryParams());
        updateGermplasms(
                germplasms.getBody().getResult().getData().stream().map(GermplasmDTO::fromBrapiGermplasm).collect(Collectors.toList())
        );
    }

    // Mechanism to automate germplasm harvest every night. Should it be in BLL rather than API?
}
