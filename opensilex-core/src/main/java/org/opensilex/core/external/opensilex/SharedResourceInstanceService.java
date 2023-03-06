/*******************************************************************************
 *                         OpenSilexService.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 16/01/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.external.opensilex;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.jena.shared.PrefixMapping;
import org.opensilex.core.config.SharedResourceInstanceItem;
import org.opensilex.core.ontology.api.OntologyAPI;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.api.AuthenticationAPI;
import org.opensilex.security.authentication.api.AuthenticationDTO;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.server.exceptions.displayable.DisplayableServiceUnavailableException;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.service.SPARQLPrefixMapping;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Service for accessing an external Shared Resource Instance (SRI). The intended use is to query the `search`,
 * `get` and `get_by_uris` services of an SRI.
 * </p>
 * <p>
 * Example use :
 *
 * <pre><code>
 * SharedResourceInstanceService service = new SharedResourceInstanceService(
 *                 coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance), currentUser.getLanguage());
 * VariableDetailsDTO dto = service.getByURI(VariableAPI.PATH, uri, VariableDetailsDTO.class);
 * </code></pre>
 * </p>
 * <p>
 * An instance of {@link SharedResourceInstanceService} manages the connexion token and the URI prefixes. In the example
 * above, the call to {@link #getByURI(String, URI, Class)} will perform the following steps :
 * <ol>
 *     <li>Perform an authentication request to the SRI and store the token</li>
 *     <li>Perform the GET query to the 'variable' API of the SRI</li>
 *     <li>Perform a request to retrieve the namespaces of the SRI and construct a {@link PrefixMapping}</li>
 *     <li>Expand all URIs in the response from the GET query using the prefix mapping</li>
 *     <li>Convert the response to the `VariableDetailsDTO` type</li>
 * </ol>
 * As the token and prefix mapping are stored in the service instance, steps 1 and 3 will only be executed on the first
 * query.
 * </p>
 * <p>
 * The base method to perform a query is {@link #get(WebTarget, Map, JavaType, boolean)}. However it is recommended not
 * to use the method as-is, but instead use one of the available wrappers :
 * <ul>
 *     <li>{@link SharedResourceInstanceService#search(String, Map, Class)}</li>
 *     <li>{@link SharedResourceInstanceService#getByURI(String, URI, Class)}</li>
 *     <li>{@link SharedResourceInstanceService#getListByURI(String, String, Collection, Class)}</li>
 * </ul>
 * </p>
 *
 * @author Valentin Rigolle
 */
public class SharedResourceInstanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedResourceInstanceService.class);

    public static final String AUTHENTICATION_ERROR_TRANSLATION_KEY = "server.errors.shared-resource-instance-connection";

    private final Client httpClient;

    private final ObjectMapper mapper;

    private final JsonNodeFactory jsonFactory;

    private final SharedResourceInstanceItem config;

    private final String authenticationApiPath;
    private final String authenticationServiceEndpoint;

    private final String lang;
    private String token;

    private PrefixMapping prefixMapping;

    public SharedResourceInstanceService(SharedResourceInstanceItem config, String lang) {
        httpClient = ClientBuilder.newClient();
        mapper = ObjectMapperContextResolver.getObjectMapper();
        jsonFactory = new JsonNodeFactory(true);
        this.config = config;
        authenticationApiPath = AuthenticationAPI.PATH;
        authenticationServiceEndpoint = AuthenticationAPI.AUTHENTICATE_PATH;
        this.lang = lang;
    }

    private void authenticate() {
        AuthenticationDTO authenticationDTO = new AuthenticationDTO();
        authenticationDTO.setIdentifier(config.accountName());
        authenticationDTO.setPassword(config.accountPassword());

        try (Response response = httpClient.target(config.uri())
                .path(authenticationApiPath)
                .path(authenticationServiceEndpoint)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(authenticationDTO))) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error("Could not authenticate to SRI : {}", config.uri());
                throw new DisplayableServiceUnavailableException("Could not connect to SRI : " + config.uri(),
                        AUTHENTICATION_ERROR_TRANSLATION_KEY,
                        new HashMap<String, String>() {{
                            put("label", config.label().get(lang));
                            put("uri", config.uri());
                        }});
            }

            SingleObjectResponse<TokenGetDTO> readResponse = response.readEntity(new GenericType<SingleObjectResponse<TokenGetDTO>>() {});
            token = readResponse.getResult().getToken();
        } catch (Exception e) {
            LOGGER.error("Error while connecting to SRI : {}", config.uri(), e);
            throw e;
        }
    }

    private String getToken() {
        if (token == null) {
            authenticate();
        }
        return token;
    }

    private void fetchPrefixMapping() {
        WebTarget target = httpClient.target(config.uri())
                .path(OntologyAPI.PATH)
                .path(OntologyAPI.GET_NAMESPACE_PATH);

        SingleObjectResponse<Map<String, String>> response = get(target, Collections.emptyMap(),
                genericType(SingleObjectResponse.class, genericType(Map.class, javaType(String.class), javaType(String.class))),
                false);
        prefixMapping = new SPARQLPrefixMapping().setNsPrefixes(response.getResult());
    }

    private PrefixMapping getPrefixMapping() {
        if (prefixMapping == null) {
            fetchPrefixMapping();
        }
        return prefixMapping;
    }

    private JavaType javaType(Class<?> rawType) {
        return mapper.constructType(rawType);
    }

    private JavaType genericType(Class<?> rawType, JavaType... typeArguments) {
        return mapper.getTypeFactory().constructParametricType(rawType, typeArguments);
    }

    private URI expandURI(URI shortURI) {
        return URI.create(getPrefixMapping().expandPrefix(shortURI.toString()));
    }

    private JsonNode expandJsonURIs(JsonNode json) {
        if (json.isTextual()) {
            try {
                URI asUri = new URI(json.asText());
                return jsonFactory.textNode(expandURI(asUri).toString());
            } catch (URISyntaxException e) {
                return jsonFactory.textNode(json.asText());
            }
        }
        if (json.isObject()) {
            ObjectNode expandedObject = jsonFactory.objectNode();
            json.fields().forEachRemaining(entry ->
                    expandedObject.set(entry.getKey(), expandJsonURIs(entry.getValue()))
            );
            return expandedObject;
        }
        if (json.isArray()) {
            ArrayNode expandedArray = jsonFactory.arrayNode(json.size());
            json.elements().forEachRemaining(node ->
                    expandedArray.add(expandJsonURIs(node))
            );
            return expandedArray;
        }
        return json;
    }

    //region Main public methods

    public URI getSharedResourceInstanceURI() {
        return URI.create(this.config.uri());
    }

    /**
     * Perform a GET query to the SRI. This method is not intended to be used as-is, but is publicly exposed to allow
     * for custom queries if necessary. If possible, you should use the following methods instead :
     *
     * <ul>
     *     <li>{@link SharedResourceInstanceService#search(String, Map, Class)}</li>
     *     <li>{@link SharedResourceInstanceService#getByURI(String, URI, Class)}</li>
     *     <li>{@link SharedResourceInstanceService#getListByURI(String, String, Collection, Class)}</li>
     * </ul>
     *
     * @param target The target to perform the query on
     * @param queryParameters The map of query parameters
     * @param responseType The expected response type for Jackson to parse the JSON response
     * @param expandURIs If the URIs in the response should be converted to their expanded form. It is recommended to
     *                   always expand the URIs of the responses.
     * @return The response of the query, converted to the specified type
     * @param <T> The expected type of the response. Should correspond to the given `responseType`
     */
    public <T> T get(WebTarget target, Map<String, String[]> queryParameters, JavaType responseType, boolean expandURIs) {
        if (queryParameters != null) {
            for (Map.Entry<String, String[]> param : queryParameters.entrySet()) {
                target = target.queryParam(param.getKey(), (Object[]) param.getValue());
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query to {}\nExpected result type : {}", target.getUri(), responseType.toString());
        }

        try (Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + getToken())
                .get()) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                JsonNode jsonResponse = response.readEntity(JsonNode.class);
                LOGGER.error("Error while trying to query {}. Response :\n{}", target.getUri(), jsonResponse.toPrettyString());
                throw new DisplayableServiceUnavailableException("Error while trying to query " + target.getUri(),
                        AUTHENTICATION_ERROR_TRANSLATION_KEY,
                        new HashMap<String, String>() {{
                            put("label", config.label().get(lang));
                            put("uri", config.uri());
                        }});
            }

            JsonNode json = response.readEntity(JsonNode.class);
            if (expandURIs) {
                JsonNode expanded = expandJsonURIs(json);
                return mapper.convertValue(expanded, responseType);
            }
            return mapper.convertValue(json, responseType);
        }
    }

    //endregion
    //region Short-hand methods for common queries

    public <T> ListWithPagination<T> search(String path, Map<String, String[]> parameters, Class<T> type) {
        WebTarget target = httpClient.target(config.uri())
                .path(path);

        PaginatedListResponse<T> response = get(target, parameters, genericType(PaginatedListResponse.class, javaType(type)), true);
        return response.getResultWithPagination();
    }

    public <T> T getByURI(String path, URI uri, Class<T> type) {
        try {
            WebTarget target = httpClient.target(config.uri())
                    .path(path)
                    .path(URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString()));

            SingleObjectResponse<T> response = get(target, Collections.emptyMap(), genericType(SingleObjectResponse.class, javaType(type)), true);
            return response.getResult();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> ListWithPagination<T> getListByURI(String path, String uriParam, Collection<URI> uriCollection, Class<T> type) {
        if (CollectionUtils.isEmpty(uriCollection)) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        WebTarget target = httpClient.target(config.uri())
                .path(path);

        Map<String, String[]> parameters = Collections.singletonMap(uriParam, uriCollection.stream()
                .map(URI::toString)
                .toArray(String[]::new)
        );

        PaginatedListResponse<T> response = get(target, parameters, genericType(PaginatedListResponse.class, javaType(type)), true);
        return response.getResultWithPagination();
    }

    //endregion
}
