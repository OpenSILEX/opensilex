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
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.exceptions.ForbiddenException;
import org.opensilex.server.exceptions.UnauthorizedException;
import org.opensilex.server.exceptions.UnexpectedErrorException;
import org.opensilex.server.security.dal.SecurityAccessDAO;
import org.opensilex.server.user.dal.UserDAO;
import org.opensilex.sparql.SPARQLService;
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
    @Named("authentication")
    AuthenticationService authentication;

    @Inject
    @Named("sparql")
    SPARQLService sparql;

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
                    URI userURI = authentication.decodeTokenUserURI(tokenValue.replace(ApiProtected.TOKEN_PARAMETER_PREFIX, ""));
                    UserDAO userDAO = new UserDAO(sparql, authentication);
                    UserModel user = userDAO.getByURI(userURI);

                    if (!user.isAdmin()) {
                        SecurityAccessDAO securityDao = new SecurityAccessDAO(sparql);
                        String accessId = securityDao.getSecurityAccessIdFromMethod(apiMethod, requestContext.getMethod());

                        if (!securityDao.checkUserAccess(user, accessId)) {
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
