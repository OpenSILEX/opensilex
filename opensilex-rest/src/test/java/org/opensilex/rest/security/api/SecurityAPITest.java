package org.opensilex.rest.security.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.junit.Test;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.group.api.GroupAPI;
import org.opensilex.rest.profile.api.ProfileAPI;
import org.opensilex.rest.user.api.UserAPI;
import org.opensilex.server.response.PaginatedListResponse;

public class SecurityAPITest extends AbstractIntegrationTest {

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
        String oldToken = getToken().getToken();
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
        TokenGetDTO oldToken = getToken();
        Response deleteResult = getDeleteJsonResponse(target(logoutPath));
        assertEquals(Response.Status.OK.getStatusCode(), deleteResult.getStatus());

        Response putResult = target(renewTokenPath).request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + oldToken.getToken())
                .put(Entity.entity("", MediaType.APPLICATION_JSON_TYPE));
//        Response putResult = getJsonPutResponse(target(renewTokenPath), "");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResult.getStatus());
    }

    @Test
    public void testCredentials() {
        Response getResult = getJsonGetPublicResponse(target(credentialsPath));

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<CredentialsGroupDTO> getResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<CredentialsGroupDTO>>() {
        });

        List<String> userCredentialsMap = new ArrayList<String>() {
            {
                add(UserAPI.CREDENTIAL_USER_DELETE_ID);
                add(UserAPI.CREDENTIAL_USER_MODIFICATION_ID);
                add(UserAPI.CREDENTIAL_USER_READ_ID);
            }
        };

        List<String> profileCredentialsMap = new ArrayList<String>() {
            {
                add(ProfileAPI.CREDENTIAL_PROFILE_DELETE_ID);
                add(ProfileAPI.CREDENTIAL_PROFILE_MODIFICATION_ID);
                add(ProfileAPI.CREDENTIAL_PROFILE_READ_ID);
            }
        };

        List<String> groupCredentialsMap = new ArrayList<String>() {
            {
                add(GroupAPI.CREDENTIAL_GROUP_DELETE_ID);
                add(GroupAPI.CREDENTIAL_GROUP_MODIFICATION_ID);
                add(GroupAPI.CREDENTIAL_GROUP_READ_ID);
            }
        };

        getResponse.getResult().forEach((credentialGroup) -> {
            if (credentialGroup.getGroupId().equals(UserAPI.CREDENTIAL_GROUP_USER_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> {
                    assertTrue(userCredentialsMap.contains(credential.getId()));
                });
            } else if (credentialGroup.getGroupId().equals(ProfileAPI.CREDENTIAL_GROUP_PROFILE_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> {
                    assertTrue(profileCredentialsMap.contains(credential.getId()));
                });
            } else if (credentialGroup.getGroupId().equals(GroupAPI.CREDENTIAL_GROUP_GROUP_ID)) {
                credentialGroup.getCredentials().forEach((credential) -> {
                    assertTrue(groupCredentialsMap.contains(credential.getId()));
                });
            }
        });
    }
}
