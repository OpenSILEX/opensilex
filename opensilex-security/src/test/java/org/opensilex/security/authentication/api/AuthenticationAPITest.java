package org.opensilex.security.authentication.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.account.api.AccountAPI;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.group.api.GroupAPI;
import org.opensilex.server.response.PaginatedListResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class AuthenticationAPITest extends AbstractSecurityIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    
    protected String path = "/security";
    protected String renewTokenPath = path + "/renew-token";
    protected String logoutPath = path + "/logout";
    protected String credentialsPath = path + "/credentials";

    @Test
    public void testRenew() throws Exception {
        String oldToken = queryAdminToken().getToken();
        Response putResult = getJsonPutResponse(target(renewTokenPath), "");
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + oldToken)
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());

        putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + "")
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());

        putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + "fdgdfgdsfgdsfgd")
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());
    }

    @Test
    public void testLogout() throws Exception {
        TokenGetDTO oldToken = queryAdminToken();
        Response deleteResult = getDeleteJsonResponse(target(logoutPath));
        assertEquals(Response.Status.OK.getStatusCode(), deleteResult.getStatus());

        Response putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + oldToken.getToken())
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResult.getStatus());
    }

    @Test
    public void testCredentials() {
        Response getResult = getJsonGetPublicResponse(target(credentialsPath));

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<CredentialsGroupDTO> getResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<CredentialsGroupDTO>>() {
        });

        List<String> accountCredentialsMap = new ArrayList<String>() {
            {
                add(AccountAPI.CREDENTIAL_ACCOUNT_MODIFICATION_ID);
                add("account-access");
            }
        };

        List<String> groupCredentialsMap = new ArrayList<String>() {
            {
                add(GroupAPI.CREDENTIAL_GROUP_DELETE_ID);
                add(GroupAPI.CREDENTIAL_GROUP_MODIFICATION_ID);
                add("group-access");
            }
        };

        getResponse.getResult().forEach((credentialGroup) -> {
            if (credentialGroup.getGroupId().equals(AccountAPI.CREDENTIAL_GROUP_ACCOUNT_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> {
                    assertTrue(accountCredentialsMap.contains(credential.getId()));
                });
            } else if (credentialGroup.getGroupId().equals(GroupAPI.CREDENTIAL_GROUP_GROUP_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> {
                    assertTrue(groupCredentialsMap.contains(credential.getId()));
                });
            }
        });
    }
}
