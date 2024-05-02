/*
 * *****************************************************************************
 *                         GermplasmClient.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 30/04/2024 16:50
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.olga.dal;

import org.brapi.client.v2.ApiResponse;
import org.brapi.client.v2.BrAPIClient;
import org.brapi.client.v2.auth.Authentication;
import org.brapi.client.v2.auth.OAuth;
import org.brapi.client.v2.model.exceptions.ApiException;
import org.brapi.client.v2.model.queryParams.germplasm.GermplasmQueryParams;
import org.brapi.client.v2.modules.germplasm.GermplasmApi;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmListResponse;
import org.brapi.v2.model.germ.response.BrAPIGermplasmSingleResponse;
import org.opensilex.olga.OlgaConfig;
import org.opensilex.olga.OlgaModule;

import java.util.List;

public class GermplasmClient {

    private static final int DEFAULT_TIMEOUT = 20000;

    private static final String AUTH_NAME = "AuthorizationToken";

    private final GermplasmApi germplasmApi;

    public GermplasmClient(OlgaModule olgaModule) {
        OlgaConfig olgaConfig = olgaModule.getConfig(OlgaConfig.class);
        BrAPIClient authenticatedBrAPIClient = new BrAPIClient(olgaConfig.host(), DEFAULT_TIMEOUT);
        Authentication authorizationToken = authenticatedBrAPIClient.getAuthentication(AUTH_NAME);
        if (authorizationToken instanceof OAuth) {
            ((OAuth) authorizationToken).setAccessToken(olgaConfig.token());
        }
        this.germplasmApi = new GermplasmApi(authenticatedBrAPIClient);
    }

    public List<BrAPIGermplasm> harvestOlgaGermplasms() throws ApiException {
        org.brapi.client.v2.ApiResponse<BrAPIGermplasmListResponse> germplasms = germplasmApi.germplasmGet(new GermplasmQueryParams());
        return germplasms.getBody().getResult().getData();
    }

    public ApiResponse<BrAPIGermplasmSingleResponse> getBrAPIGermplasm(String germplasmDbId) throws ApiException {
        return germplasmApi
                .germplasmGermplasmDbIdGet(germplasmDbId);
    }
}
