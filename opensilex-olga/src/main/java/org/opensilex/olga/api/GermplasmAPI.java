package org.opensilex.olga.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.*;
import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.JSON;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.brapi.client.v2.model.queryParams.germplasm.GermplasmQueryParams;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmListResponse;
import org.brapi.v2.model.germ.response.BrAPIGermplasmListResponseResult;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.olga.OlgaConfig;
import org.opensilex.olga.OlgaModule;
import org.opensilex.olga.model.GermplasmDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

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
    @Path("")
    @ApiOperation("Get olga germplasms")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return list of olga germplasms", response = GermplasmDTO.class, responseContainer = "List")
    })
    public Response getGermplasms() throws Exception {
        OlgaConfig olgaConfig = olgaModule.getConfig(OlgaConfig.class);
        BrAPIClient brAPIClient = new BrAPIClient(olgaConfig.host(), DEFAULT_TIMEOUT);
        Authentication authorizationToken = brAPIClient.getAuthentication("AuthorizationToken");
        if (authorizationToken instanceof OAuth) {
            ((OAuth)authorizationToken).setAccessToken(olgaConfig.token());
        };
        GermplasmApi germplasmApi = new GermplasmApi(brAPIClient);
        /*org.brapi.client.v2.ApiResponse<BrAPIGermplasmListResponse> germplasms = germplasmApi.germplasmGet(new GermplasmQueryParams());*/

        // TODO : remove this tmp workaround for malformed response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.readValue(
                new File("/home/besombes/Documents/gitlab/opensilex-dev/opensilex-olga/src/main/java/org/opensilex/olga/tmp.json"),
                JsonNode.class
        );
        Type resultType = new TypeToken<BrAPIGermplasmListResponseResult>(){}.getType();

        JSON json = new JSON().setLenientOnJson(true);
        BrAPIGermplasmListResponseResult result = json.deserialize(response.get("result").toString(), resultType);
        // TODO : end of workaround

        return new SingleObjectResponse<>(new GermplasmDTO().setGermplasmDbId("test")).getResponse();
    }
}
