//******************************************************************************
//                    CORSResponseFilter.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.security;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * CORS response filter.
 * For more information on response filters with Jersey
 * see: https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html
 *
 * Allow for cross-origin requests and allow authorization header.
 *
 * <pre>
 * @author Arnaud Charleroy
 */
@Provider
public class CORSResponseFilter implements ContainerResponseFilter {

    final static Logger LOGGER = LoggerFactory.getLogger(CORSResponseFilter.class);
    @Context
    UriInfo ui;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

}
