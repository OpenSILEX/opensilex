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
import com.google.common.base.Optional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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

/**
 * <pre>
 * Authentication filter
 * For more information on request filters with Jersey
 * see: https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html
 *
 * For any API service call, check if method is annotated with {@code org.opensilex.server.security.ApiProtected}
 * In that case parse header token to determine current user.
 * If user as "admin" flag, allow access to protected service.
 * Otherwise check if current method is in user credentials.
 * Throw {@code org.opensilex.server.exceptions.UnauthorizedException} if issues occured during token decoding or no token provided
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

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
        return "Direct:" + httpRequest.getRemoteAddr();
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional<String> clientId = Optional.fromNullable(httpRequest.getHeader("X-Forwarded-For"));
        MDC.put(CLIENT_ID, clientId.or(defaultClientId())); 
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(HOST_NAME, httpRequest.getServerName());
        
        LOGGER.debug("Incoming request URI: " + requestContext.getUriInfo().getRequestUri());
        LOGGER.debug("Incoming request method: " + requestContext.getMethod());
        final AtomicBoolean isJSON = new AtomicBoolean(false);
        requestContext.getHeaders().forEach((header, value) -> {
            if (value.size() == 1) {
                if (header.equalsIgnoreCase("content-type") && value.get(0).equals(MediaType.APPLICATION_JSON)) {
                    isJSON.set(true);
                }
            }
            LOGGER.debug("Incoming request header: " + header + " -> " + value);
        });

        try {
            if (isJSON.get()) {
                String body = IOUtils.toString(requestContext.getEntityStream(), Charset.forName("UTF-8"));
                ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
                LOGGER.debug("Incoming request JSON body: \n" + json);
                requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes(Charset.forName("UTF-8"))));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Error JSON: " + ex.getMessage());
        }


        // get user header token
        String tokenValue = requestContext.getHeaderString(ApiProtected.HEADER_NAME);

        boolean isSecuredAPI = false;
        Method apiMethod = resourceInfo.getResourceMethod();

        if (apiMethod != null) {
            // Get method ApiProtected annotation
            ApiProtected securityAnnotation = apiMethod.getAnnotation(ApiProtected.class);

            isSecuredAPI = (securityAnnotation != null);
        }

        // Ignore user definition if no token
        AccountModel user;
        if (tokenValue != null) {

            try {
                // Decode token
                String token = tokenValue.replace(ApiProtected.TOKEN_PARAMETER_PREFIX, "");
                URI userURI = authentication.decodeTokenUserURI(token);

                // Get corresponding user
                if (authentication.hasUserURI(userURI)) {
                    user = authentication.getUserByUri(userURI);
                } else {
                    throw new ForbiddenException("User not found with URI: " + userURI);
                }

            } catch (JWTVerificationException | URISyntaxException ex) {
                LOGGER.debug("Error while decoding and verifying token: " + ex.getMessage());
                if (isSecuredAPI) {
                    throw new UnauthorizedException();
                } else {
                    user = AccountModel.getAnonymous();
                }
            } catch (ForbiddenException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new UnexpectedErrorException(ex);
            }
        } else {
            user = AccountModel.getAnonymous();
        }

        List<Locale> locales = headers.getAcceptableLanguages();

        Locale locale = null;
        for (Locale l : locales) {
            locale = l;
            break;
        }

        if (locale != null && !locale.toString().equals("*")) {
            user.setLocale(locale);
        }

        // Define user to be accessed through SecurityContext
        SecurityContext originalContext = requestContext.getSecurityContext();
        SecurityContext newContext = new SecurityContextProxy(originalContext, user);
        requestContext.setSecurityContext(newContext);
        MDC.put(USER_ID, user.getEmail().toString());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.clear();
    }
}
