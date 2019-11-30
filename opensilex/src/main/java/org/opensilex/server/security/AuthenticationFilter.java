//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************n the template in the editor.
package org.opensilex.server.security;

import com.auth0.jwt.exceptions.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import javax.annotation.*;
import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.*;
import org.opensilex.server.security.model.*;
import org.opensilex.sparql.*;

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
                    throw getAccessDeniedException();
                }

                try {
                    URI userURI = authentication.decodeTokenUserURI(tokenValue.replace("Bearer ", ""));
                    UserModel user = sparql.getByURI(UserModel.class, userURI);

                    SecurityContext originalContext = requestContext.getSecurityContext();

                    SecurityContext newContext = new SecurityContextProxy(originalContext, user);

                    requestContext.setSecurityContext(newContext);

                } catch (JWTVerificationException | URISyntaxException ex) {
                    throw getAccessDeniedException();
                } catch (Throwable ex) {
                    throw getUnexpectedErrorException(ex);
                }

            }
        }
    }

    private static WebApplicationException getAccessDeniedException() {
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse(
                                Response.Status.UNAUTHORIZED,
                                "Access denied",
                                "You must be authenticate and having the right authorizations to access this URL"))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

    private static WebApplicationException getUnexpectedErrorException(Throwable ex) {
        return new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse(ex))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

}
