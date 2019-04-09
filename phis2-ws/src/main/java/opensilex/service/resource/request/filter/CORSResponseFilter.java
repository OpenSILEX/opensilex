//******************************************************************************
//                            CORSResponseFilter.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: May 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.request.filter;

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
 * CORS response filter.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Provider
public class CORSResponseFilter implements ContainerResponseFilter {
        final static Logger LOGGER = LoggerFactory.getLogger(CORSResponseFilter.class);
        @Context UriInfo ui;
        
        @Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();

//		headers.add("Access-Control-Allow-Origin", "http://podcastpedia.org"); //allows CORS requests only coming from podcastpedia.org		
		headers.add("Access-Control-Allow-Origin", "*");
                headers.add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
                headers.add("Access-Control-Allow-Credentials", "true");
                headers.add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
	}

}
