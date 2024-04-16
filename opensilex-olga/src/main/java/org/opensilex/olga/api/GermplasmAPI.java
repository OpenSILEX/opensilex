package org.opensilex.olga.api;

import io.swagger.annotations.*;
import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIStatus;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmSingleResponse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.olga.BrAPIResponseSerialiser;
import org.opensilex.olga.OlgaConfig;
import org.opensilex.olga.OlgaModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.*;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

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
        GermplasmApi germplasmApi = getAuthenticatedGarmplasmAPIClient();
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmSingleResponse> germplasmDetail = germplasmApi.germplasmGermplasmDbIdGet(germplasmDbId);
        germplasmDetail.getBody().getResult().setAdditionalInfo(null);

        BrAPIGermplasm responseObject = germplasmDetail.getBody().getResult();
        BrAPIPagination responsePagintation = germplasmDetail.getBody().getMetadata().getPagination();
        List<BrAPIStatus> responseStatus = germplasmDetail.getBody().getMetadata().getStatus();

        return BrAPIResponseSerialiser.singleObjectResponseSerialiser(responseObject, responsePagintation, responseStatus).getResponse();
    }

    private void harvestOlgaGermplasms() {

    }
}
