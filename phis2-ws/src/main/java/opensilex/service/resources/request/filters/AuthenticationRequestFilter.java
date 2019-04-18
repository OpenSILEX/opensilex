//******************************************************************************
//                     AuthentificationRequestFilter.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.request.filters;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.authentication.TokenManager;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.DataResourceService;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormGET;

/**
 * Authentication request filter.
 * Filters web service requests according to the header and other parameters.
 * @update [Arnaud Charleroy] Oct. 2016: BrAPI v1
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Provider
public class AuthenticationRequestFilter implements ContainerRequestFilter {

    final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationRequestFilter.class);

    @Context
    private ResourceInfo resourceInfo;
        
    /**
     * Filters the session token.
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        Response accessDenied = Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ResponseFormGET(
                        new Status("You cannot access this resource.", StatusCodeMsg.ERR,
                                "Invalid token")))
                .type(MediaType.APPLICATION_JSON).build();
        
        
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String resourcePath = uriInfo.getPath();
        // Swagger.json and token authorized
        if (resourcePath != null 
                && !resourcePath.contains("token") 
                && !resourcePath.contains("calls") 
                && !resourcePath.contains("swagger.json")
                && !(resourceInfo.getResourceClass() == DataResourceService.class && resourceInfo.getResourceMethod().getName().equals("getDataFile"))) {
            // Get request headers
            final MultivaluedMap<String, String> headers = requestContext.getHeaders();
            if (headers != null && !headers.containsKey(GlobalWebserviceValues.AUTHORIZATION_PROPERTY)) {
                throw new WebApplicationException(accessDenied);
            }
            // Fetch authorization header
            
            String authorization = requestContext.getHeaderString(GlobalWebserviceValues.AUTHORIZATION_PROPERTY);
            
            // If no authorization information present; block access
            if (authorization == null || authorization.isEmpty()) {
                throw new WebApplicationException(accessDenied);
            }

            Pattern authorizationPattern = Pattern.compile(GlobalWebserviceValues.AUTHENTICATION_SCHEME + " .*");
            Matcher m = authorizationPattern.matcher(authorization);

            if (!m.matches()) {
                throw new WebApplicationException(accessDenied);
            }

            //Get session id
            String userToken = authorization.replace("Bearer ", "");
            if (!TokenManager.Instance().checkAuthentication(userToken)) {
                throw new WebApplicationException(accessDenied);
            }
        }
    }
}
