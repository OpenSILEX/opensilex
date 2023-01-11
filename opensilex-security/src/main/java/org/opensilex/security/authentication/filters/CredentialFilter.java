//******************************************************************************
//                      AuthenticationFilter.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************n the template in the editor.
package org.opensilex.security.authentication.filters;

import org.opensilex.security.authentication.AuthenticationService;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.exceptions.UnauthorizedException;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.dal.AuthenticationDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Priority(Priorities.AUTHORIZATION)
public class CredentialFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialFilter.class);

    @Context
    ResourceInfo resourceInfo;

    @Inject
    AuthenticationService authentication;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        AccountModel user = (AccountModel) requestContext.getSecurityContext().getUserPrincipal();
        Method apiMethod = resourceInfo.getResourceMethod();

        if (apiMethod != null) {
            // Get method ApiProtected annotation
            ApiProtected securityAnnotation = apiMethod.getAnnotation(ApiProtected.class);

            if (securityAnnotation != null) {

                // Throw exception if no token
                if (user == null || user.isAnonymous()) {
                    throw new UnauthorizedException();
                }

                boolean hasCredential = false;
                if (user.isAdmin()) {
                    hasCredential = true;
                } else if (securityAnnotation.adminOnly()) {
                    hasCredential = false;
                } else {
                    // If user is not an admin check credentials if needed
                    String credentialId = AuthenticationDAO.getCredentialIdFromMethod(apiMethod);
                    if (credentialId != null) {
                        // Get current API service credential

                        // Get user credentials from token
                        String[] accessList = authentication.decodeTokenCredentialsList(user.getToken());

                        // Check user credential existence
                        hasCredential = Arrays.stream(accessList).anyMatch(credentialId::equals);
                    } else {
                        // If no specific credential, user logged in is sufficient
                        hasCredential = true;
                    }
                }

                if (!hasCredential) {
                    throw new ForbiddenException("You don't have credentials to access this API");
                }
            }
        }
    }
}
