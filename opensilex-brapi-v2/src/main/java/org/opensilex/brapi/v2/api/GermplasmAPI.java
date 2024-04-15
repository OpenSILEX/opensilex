package org.opensilex.brapi.v2.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.brapi.client.v2.model.queryParams.germplasm.GermplasmQueryParams;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmListResponse;
import org.opensilex.brapi.v2.BrapiV2Config;
import org.opensilex.brapi.v2.BrapiV2Module;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

    public static final String PATH = "/brapi-v2/germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_ID = "BrapiV2 Germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.brapi-v2-germplasm";
    public static final int DEFAULT_TIMEOUT = 20000;

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    @Inject
    private BrapiV2Module brapiV2Module;

    @GET
    @Path("")
    @ApiOperation("Get brapi-v2 germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of brapi-v2 germplasms", response = BrAPIGermplasm.class, responseContainer = "List")
    })
    public org.brapi.client.v2.ApiResponse<BrAPIGermplasmListResponse> getGermplasms() throws Exception {
        BrapiV2Config brapiV2Config = brapiV2Module.getConfig(BrapiV2Config.class);
        BrAPIClient brAPIClient = new BrAPIClient(brapiV2Config.host(), DEFAULT_TIMEOUT);
        Authentication authorizationToken = brAPIClient.getAuthentication("AuthorizationToken");
        if (authorizationToken instanceof OAuth) {
            ((OAuth)authorizationToken).setAccessToken(brapiV2Config.token());
        }
        GermplasmApi germplasmApi = new GermplasmApi(brAPIClient);
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmListResponse> germplasms = germplasmApi.germplasmGet(new GermplasmQueryParams());

        return germplasms;
    }
}
