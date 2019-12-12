//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************n the template in the editor.
package org.opensilex.server.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.exceptions.UnauthorizedException;
import org.opensilex.server.exceptions.UnexpectedErrorException;
import org.opensilex.server.security.dal.SecurityAccessDAO;
import org.opensilex.server.user.UserRegistryService;
import org.opensilex.server.user.dal.UserModel;

/**
 *
 * @author Vincent Migot
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Inject
    AuthenticationService authentication;

    @Inject
    UserRegistryService registry;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method apiMethod = resourceInfo.getResourceMethod();

        if (apiMethod != null) {
            ApiProtected securityAnnotation = apiMethod.getAnnotation(ApiProtected.class);

            if (securityAnnotation != null) {
                String tokenValue = requestContext.getHeaderString(ApiProtected.HEADER_NAME);

                if (tokenValue == null) {
                    throw new UnauthorizedException();
                }

                try {
                    String token = tokenValue.replace(ApiProtected.TOKEN_PARAMETER_PREFIX, "");
                    URI userURI = authentication.decodeTokenUserURI(token);

                    UserModel user;
                    if (registry.hasUserURI(userURI)) {
                        user = registry.getUserByUri(userURI);
                    } else {
                        throw new ForbiddenException("User not found with URI: " + userURI);
                    }

                    if (!user.isAdmin()) {
                        String accessId = SecurityAccessDAO.getSecurityAccessIdFromMethod(apiMethod, requestContext.getMethod());

                        String[] accessList = authentication.decodeTokenAccessList(user.getToken());
                        boolean hasAccess = Arrays.stream(accessList).anyMatch(accessId::equals);
                        if (!hasAccess) {
                            throw new ForbiddenException("You don't have credentials to access this API");
                        }
                    }

                    SecurityContext originalContext = requestContext.getSecurityContext();

                    SecurityContext newContext = new SecurityContextProxy(originalContext, user);

                    requestContext.setSecurityContext(newContext);

                } catch (JWTVerificationException | URISyntaxException ex) {
                    throw new UnauthorizedException();
                } catch (ForbiddenException ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new UnexpectedErrorException(ex);
                }
            }
        }
    }
}
