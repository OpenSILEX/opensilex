/*******************************************************************************
 *                         AgroPortalService.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 04/12/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.external.agroportal;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.opensilex.core.CoreConfig;
import org.opensilex.server.exceptions.displayable.DisplayableServiceUnavailableException;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.service.reflection.SelfBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for querying Agroportal.
 *
 * @author Valentin Rigolle
 */
@SelfBound
@Service
public class AgroportalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgroportalService.class);
    private static final String UNAVAILABLE_ERROR_TRANSLATION_KEY = "server.errors.agroportal-instance-connection";
    private static final String AGROPORTAL_NOT_CONFIGURED_ERROR_TRANSLATION_KEY =
            "server.errors.agroportal-not-configured";
    private static final String SEARCH_ENDPOINT = "search";
    private static final String SEARCH_QUERY_PARAMETER = "q";
    private static final String SEARCH_ONTOLOGIES_PARAMETER = "ontologies";
    private static final String ONTOLOGIES_ENDPOINT = "ontologies";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String AUTHORIZATION_HEADER_CONTENT_FORMAT = "apikey token=%s";
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 1000;
    private final String apiKey;
    private final String basePath;
    private final String baseApiPath;
    private final Client httpClient;
    private final ObjectMapper mapper;

    @Inject
    public AgroportalService(CoreConfig config) {
        this.apiKey = config.agroportal().externalAPIKey();
        this.basePath = config.agroportal().basePath();
        this.baseApiPath = config.agroportal().baseAPIPath();

        this.httpClient = ClientBuilder.newClient();
        this.mapper = ObjectMapperContextResolver.getObjectMapper();
    }

    //#region Public methods

    public boolean isEnable() {
        return StringUtils.isNoneEmpty(this.apiKey, this.basePath, this.baseApiPath);
    }

    public boolean ping(Long optionalTimeoutMs) throws DisplayableServiceUnavailableException {
        throwIfNotEnable();
        long nonNullTimeoutMs = Optional.ofNullable(optionalTimeoutMs)
                .orElse(DEFAULT_CONNECTION_TIMEOUT_MS);
        var clientWithCustomTimeout = ClientBuilder.newBuilder()
                .connectTimeout(nonNullTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
        try (var response = clientWithCustomTimeout
                .target(basePath)
                .request()
                .header(AUTHORIZATION_HEADER_NAME, String.format(AUTHORIZATION_HEADER_CONTENT_FORMAT, apiKey))
                .get()) {
            return response.getStatus() == Response.Status.OK.getStatusCode();
        }
    }

    public List<OntologyAgroportalModel> getOntologies() throws DisplayableServiceUnavailableException {
        throwIfNotEnable();
        var target = httpClient.target(baseApiPath)
                .path(ONTOLOGIES_ENDPOINT);
        return get(target,
                null,
                mapper.getTypeFactory().constructParametricType(List.class, OntologyAgroportalModel.class));
    }

    public AgroportalSearchResultModel search(String query, List<String> ontologies) throws DisplayableServiceUnavailableException {
        throwIfNotEnable();
        var target = httpClient.target(baseApiPath)
                .path(SEARCH_ENDPOINT);
        var params = new HashMap<String, List<String>>();
        params.put(SEARCH_QUERY_PARAMETER, Collections.singletonList(query));
        if (CollectionUtils.isNotEmpty(ontologies)) {
            params.put(SEARCH_ONTOLOGIES_PARAMETER, Collections.singletonList(
                    formatOntologiesParameter(ontologies)
            ));
        }
        return get(target, params, mapper.getTypeFactory().constructType(AgroportalSearchResultModel.class));
    }

    //#endregion

    //#region Private methods

    private <T> T get(WebTarget target, Map<String, List<String>> queryParameters, JavaType responseType) {
        if (queryParameters != null) {
            for (var param : queryParameters.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue().toArray());
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query to {}\nExpected result type : {}", target.getUri(), responseType.toString());
        }

        try (var response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION_HEADER_NAME, String.format(AUTHORIZATION_HEADER_CONTENT_FORMAT, apiKey))
                .get()) {
            var jsonResponse = response.readEntity(JsonNode.class);
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error("Error while trying to query {}. Response :\n{}", target.getUri(),
                        jsonResponse.toPrettyString());
                throw new DisplayableServiceUnavailableException("Error while trying to query " + target.getUri(),
                        UNAVAILABLE_ERROR_TRANSLATION_KEY,
                        null);
            }
            return mapper.convertValue(jsonResponse, responseType);
        }
    }

    /**
     * The `ontologies` parameter of the Agroportal search service only takes one parameter with all ontology
     * acronyms separated by colons.
     *
     * @param ontologies The list of ontology acronyms
     * @return A single string with all ontology acronyms separated by colons
     */
    private String formatOntologiesParameter(List<String> ontologies) {
        return ontologies.stream()
                .reduce((result, element) -> result + "," + element)
                .orElse("");
    }

    private void throwIfNotEnable() throws DisplayableServiceUnavailableException {
        if (!this.isEnable()) {
            throw new DisplayableServiceUnavailableException("Agroportal is not configured for this instance",
                    AGROPORTAL_NOT_CONFIGURED_ERROR_TRANSLATION_KEY,
                    null);
        }
    }

    //#endregion
}
