package org.opensilex.olga;

import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.opensilex.OpenSilexModule;
import org.opensilex.core.CoreModule;
import org.opensilex.server.extensions.APIExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlgaModule extends OpenSilexModule implements APIExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    public static final int DEFAULT_TIMEOUT = 20000;

    @Override
    public Class<?> getConfigClass() {
        return OlgaConfig.class;
    }

    @Override
    public String getConfigId() {
        return "olga";
    }

    public BrAPIClient getAuthenticatedBrAPIClient(){
        OlgaConfig olgaConfig = this.getConfig(OlgaConfig.class);
        BrAPIClient authenticatedBrAPIClient = new BrAPIClient(olgaConfig.host(), DEFAULT_TIMEOUT);
        Authentication authorizationToken = authenticatedBrAPIClient.getAuthentication("AuthorizationToken");
        if (authorizationToken instanceof OAuth) {
            ((OAuth)authorizationToken).setAccessToken(olgaConfig.token());
        }
        return authenticatedBrAPIClient;
    }
}