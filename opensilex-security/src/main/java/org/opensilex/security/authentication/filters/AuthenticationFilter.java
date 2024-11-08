//******************************************************************************
//                      AuthenticationFilter.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************n the template in the editor.
package org.opensilex.security.authentication.filters;

import org.opensilex.security.authentication.AuthenticationService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.exceptions.UnauthorizedException;
import org.opensilex.server.exceptions.UnexpectedErrorException;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.SecurityContextProxy;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.opensilex.log.LogFilter.LOG_TYPE_KEY;

/**
 * <pre>
 * Authentication filter
 * For more information on request filters with Jersey
 * see: <a href="https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html">...</a>
 *
 * For any API service call, check if method is annotated with {@code org.opensilex.server.security.ApiProtected}
 * In that case parse header token to determine current user.
 * If user as "admin" flag, allow access to protected service.
 * Otherwise check if current method is in user credentials.
 * Throw {@code org.opensilex.server.exceptions.UnauthorizedException} if issues occurred during token decoding or no token provided
 * Throw {@code org.opensilex.server.exceptions.ForbiddenException} if user is not found or don't have right credentials
 *
 * If user is identified, it can be accessed in the corresponding API method this way:
 * <code>
 *   ...
 *
 *   &#64;Inject
 *   private AuthenticationService authentication;
 *
 *   ...
 *
 * &#64;GET
 * &#64;Path("api-method") ...
 * &#64;Produces(MediaType.APPLICATION_JSON) public Response apiMethod(
 * &#64;Context SecurityContext securityContext ) throws Exception { AccountModel
 * currentUser = authentication.getCurrentUser(securityContext);
 *
 * ... Do stuff with current user }
 *
 * ...
 * </code>
 * </pre>
 *
 * @author Vincent Migot
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter  {
    private static final String CLIENT_ID = "client-id";
    private static final String HOST_NAME = "host-name";
    private static final String REQUEST_ID = "request-id";
    private static final String USER_ID = "user-id";

    private static final String HTTP_REQUEST_URI = "http_request_uri";
    private static final String HTTP_SERVICE_PATH = "http_request_path";
    private static final String HTTP_REQUEST_METHOD = "http_request_method";
    private static final String HTTP_REQUEST_HEADER = "http_request_header";
    private static final String HTTP_REQUEST_BODY = "http_request_body";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    public static final String HTTP_REQUEST_LOG_TYPE = "http_request";
    public static final String HTTP_REQUEST_HEADERS_TYPE = "http_headers";
    public static final String HTTP_REQUEST_BODY_TYPE = "http_body";

    @Context
    protected HttpServletRequest httpRequest;
 
    @Context
    HttpHeaders headers;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    AuthenticationService authentication;

    @Inject
    OpenSilex opensilex;

    private String defaultClientId() {
        return httpRequest.getRemoteAddr();
    }

    private void logRequest(ContainerRequestContext requestContext){

        // MDC (attributes propagated to each log write inside the current thread)
        String clientId = java.util.Optional.ofNullable(httpRequest.getHeader("X-Forwarded-For"))
                .orElse(defaultClientId());
        String requestUri = requestContext.getUriInfo().getRequestUri().toString();

        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(CLIENT_ID, clientId);
        MDC.put(HOST_NAME, httpRequest.getServerName());
        MDC.put(HTTP_SERVICE_PATH, requestContext.getUriInfo().getPath());
        MDC.put(HTTP_REQUEST_URI, requestUri);

        // Log request properties as key/value
        // No duplicate log of request headers/metadata
        LOGGER.info("http request {} {}",
                kv(HTTP_REQUEST_URI, requestUri),
                kv(HTTP_REQUEST_METHOD, requestContext.getMethod()),
                kv(LOG_TYPE_KEY, HTTP_REQUEST_LOG_TYPE)
        );

        // No more log entry to write, just end
        if (! LOGGER.isDebugEnabled()) {
          return;
        }

        // debug HTTP header
        LOGGER.debug("http request headers",
                kv(LOG_TYPE_KEY, HTTP_REQUEST_HEADERS_TYPE),
                kv(HTTP_REQUEST_HEADER, requestContext.getHeaders()));

        // debug HTTP body
        boolean isJson = requestContext.getHeaders().entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("content-type"))
                .filter(entry -> entry.getValue() != null && entry.getValue().size() == 1)
                .anyMatch(entry ->  MediaType.APPLICATION_JSON.equals(entry.getValue().get(0)));

        if(! isJson){
            return;
        }

        // read body from request stream
        // since the stream is consumed, the entity stream must be reset by reading body content
        try {
            String body = IOUtils.toString(requestContext.getEntityStream(), StandardCharsets.UTF_8);
            ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();

            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
            LOGGER.debug("http request JSON body",
                    kv(LOG_TYPE_KEY, HTTP_REQUEST_BODY_TYPE),
                    kv(HTTP_REQUEST_BODY, json)
            );

            requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Error JSON: " + ex.getMessage());
        }
    }

    private AccountModel checkAccountSecurity(ContainerRequestContext context){

        // Check if API is secured : Get method ApiProtected annotation
        boolean isSecuredAPI = false;
        Method apiMethod = resourceInfo.getResourceMethod();
        if (apiMethod != null) {
            isSecuredAPI = apiMethod.getAnnotation(ApiProtected.class) != null;
        }

        // Get user header token
        String tokenValue = context.getHeaderString(ApiProtected.HEADER_NAME);
        AccountModel user = getAndValidateAccount(tokenValue, isSecuredAPI);

        // Define user to be accessed through SecurityContext
        SecurityContext originalContext = context.getSecurityContext();
        SecurityContext newContext = new SecurityContextProxy(originalContext, user);
        context.setSecurityContext(newContext);
        MDC.put(USER_ID, user.getEmail().toString());

        return user;
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {

        logRequest(context);
        AccountModel user = checkAccountSecurity(context);

        // Lang/local handling
        List<Locale> locales = headers.getAcceptableLanguages();
        Locale locale = locales.isEmpty() ? null : locales.get(0);
        if (locale != null && !locale.toString().equals("*")) {
            user.setLocale(locale);
        }
    }

    private AccountModel getAndValidateAccount(String tokenValue, boolean isSecuredAPI) {AccountModel.getAnonymous();

        if( tokenValue == null){
            return AccountModel.getAnonymous();
        }

        // Decode token
        // Ignore user definition if no token
        try {
            String token = tokenValue.replace(ApiProtected.TOKEN_PARAMETER_PREFIX, "");
            URI userURI = authentication.decodeTokenUserURI(token);

            // Get corresponding user
            if (authentication.hasUserURI(userURI)) {
                return authentication.getUserByUri(userURI);
            } else {
                throw new ForbiddenException("User not found with URI: " + userURI);
            }

        } catch (JWTVerificationException | URISyntaxException ex) {
            LOGGER.debug("Error while decoding and verifying token: {}", ex.getMessage());
            if (isSecuredAPI) {
                throw new UnauthorizedException();
            } else {
                return AccountModel.getAnonymous();
            }
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnexpectedErrorException(ex);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.clear();
    }
}
