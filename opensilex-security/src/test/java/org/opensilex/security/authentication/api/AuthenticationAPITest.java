package org.opensilex.security.authentication.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.opensilex.integration.test.ServiceDescription;
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
import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL;
import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_PASSWORD;

public class AuthenticationAPITest extends AbstractSecurityIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    
    protected static final String path = "/security";
    protected static final ServiceDescription renewToken;
    protected static final ServiceDescription logout;
    protected static final ServiceDescription credentials;

    static {
        try {
            renewToken = new ServiceDescription(
                    AuthenticationAPI.class.getMethod("renewToken", String.class),
                    path + "/renew-token"
            );
            logout = new ServiceDescription(
                    AuthenticationAPI.class.getMethod("logout"),
                    path + "/logout"
            );
            credentials = new ServiceDescription(
                    AuthenticationAPI.class.getMethod("getCredentialsGroups"),
                    path + "/credentials"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRenew() throws Exception {

        // Login as admin and get token
        TokenGetDTO oldToken = authenticateAndRegisterIfNecessary(DEFAULT_SUPER_ADMIN_EMAIL, DEFAULT_SUPER_ADMIN_PASSWORD);

        // Renew token -> OK
        Response putResult = new UserCallBuilder(renewToken).setBody("").buildAdmin().executeCall();
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // Renew token withold but still valid token -> OK
        putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + oldToken.getToken())
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // Renew token without token -> UNAUTHORIZED
        putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());

        putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());

        // Renew token with empty token -> UNAUTHORIZED
        putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX)
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());

        // Renew token with invalid token -> UNAUTHORIZED
        putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + "fdgdfgdsfgdsfgd")
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), putResult.getStatus());
    }

    @Test
    public void testLogout() throws Exception {

        // Login as admin and get token
        TokenGetDTO oldToken = authenticateAndRegisterIfNecessary(DEFAULT_SUPER_ADMIN_EMAIL, DEFAULT_SUPER_ADMIN_PASSWORD);

        // Logout (deletes the token)
        Response deleteResult = new UserCallBuilder(logout).buildAdmin().executeCall();
        assertEquals(Response.Status.OK.getStatusCode(), deleteResult.getStatus());

        // Try to renew token without authorization
        Response putResult = target(renewToken.getPathTemplate()).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + oldToken.getToken())
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResult.getStatus());
    }

    @Test
    public void testCredentials() throws Exception {
        PaginatedListResponse<CredentialsGroupDTO> getResponse = new PublicCallBuilder<>(credentials).build()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<CredentialsGroupDTO>>() {
        }).getDeserializedResponse();

        List<String> accountCredentialsMap = new ArrayList<>() {
            {
                add(AccountAPI.CREDENTIAL_ACCOUNT_MODIFICATION_ID);
                add("account-access");
            }
        };

        List<String> groupCredentialsMap = new ArrayList<>() {
            {
                add(GroupAPI.CREDENTIAL_GROUP_DELETE_ID);
                add(GroupAPI.CREDENTIAL_GROUP_MODIFICATION_ID);
                add("group-access");
            }
        };

        getResponse.getResult().forEach((credentialGroup) -> {
            if (credentialGroup.getGroupId().equals(AccountAPI.CREDENTIAL_GROUP_ACCOUNT_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> assertTrue(accountCredentialsMap.contains(credential.getId())));
            } else if (credentialGroup.getGroupId().equals(GroupAPI.CREDENTIAL_GROUP_GROUP_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> assertTrue(groupCredentialsMap.contains(credential.getId())));
            }
        });
    }
}
