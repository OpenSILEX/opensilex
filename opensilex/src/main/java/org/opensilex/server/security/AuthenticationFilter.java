/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.server.security.user.User;

/**
 *
 * @author vincent
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
                    URI userURI = authentication.decodeTokenUserURI(tokenValue);
                    User user = sparql.getByURI(User.class, userURI);

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

    private class SecurityContextProxy implements SecurityContext {

        SecurityContext parentContext;
        User user;

        private SecurityContextProxy(SecurityContext parentContext, User user) {
            this.parentContext = parentContext;
            this.user = user;
        }

        @Override
        public Principal getUserPrincipal() {
            return user;
        }

        @Override
        public boolean isUserInRole(String role) {
            return parentContext.isUserInRole(role);
        }

        @Override
        public boolean isSecure() {
            return parentContext.isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
            return parentContext.getAuthenticationScheme();
        }
    }

    private static WebApplicationException getAccessDeniedException() {
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Access denied", "You must be authenticate and having the right authorizations to access this URL"))
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
