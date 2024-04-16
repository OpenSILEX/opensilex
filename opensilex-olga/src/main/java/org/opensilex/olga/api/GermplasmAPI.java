package org.opensilex.olga.api;

import io.swagger.annotations.*;
import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.germ.response.BrAPIGermplasmSingleResponse;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.olga.OlgaConfig;
import org.opensilex.olga.OlgaModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
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

    public static final String PATH = "/olga/germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_ID = "Olga Germplasm";
    public static final String CREDENTIAL_OLGA_GERMPLASM_GROUP_LABEL_KEY = "credential-groups.olga-germplasm";
    public static final int DEFAULT_TIMEOUT = 20000;

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

    @GET
    @Path("{germplasmDbId}")
    @ApiOperation("Get olga germplasm by germplasmDbId")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return details of an olga germplasm", response = org.brapi.client.v2.ApiResponse.class)
    })
    public org.brapi.client.v2.ApiResponse<BrAPIGermplasmSingleResponse> getGermplasmByDbId(
            @ApiParam(value = GERMPLASM_DBID, example = GERMPLASM_EXAMPLE_DBID, required = true) @PathParam("germplasmDbId") @NotNull String germplasmDbId
    ) throws Exception {
        OlgaConfig olgaConfig = olgaModule.getConfig(OlgaConfig.class);
        BrAPIClient brAPIClient = new BrAPIClient(olgaConfig.host(), DEFAULT_TIMEOUT);
        Authentication authorizationToken = brAPIClient.getAuthentication("AuthorizationToken");
        if (authorizationToken instanceof OAuth) {
            ((OAuth)authorizationToken).setAccessToken(olgaConfig.token());
        }
        GermplasmApi germplasmApi = new GermplasmApi(brAPIClient);
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmSingleResponse> germplasmDetail = germplasmApi.germplasmGermplasmDbIdGet(germplasmDbId);

        return germplasmDetail;
    }
}
