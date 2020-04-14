/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
@Provider
@Priority(Priorities.ENTITY_CODER)
public class ApiCacheFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCacheFilter.class);

    @Inject
    private ApiCacheService cache;

    @Context
    private ResourceInfo resourceInfo;

    private ApiCache cacheAnnotation;

    private String key;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        Method apiMethod = resourceInfo.getResourceMethod();
        if (apiMethod != null) {
            cacheAnnotation = apiMethod.getAnnotation(ApiCache.class);
            if (cacheAnnotation != null) {
                switch (context.getRequest().getMethod()) {
                    case HttpMethod.GET: {
                        try {
                            key = computeCacheKey(context);
                            if (this.cache.exists(cacheAnnotation.category(), key)) {
                                ContainerResponseContext responseContext = (ContainerResponseContext) this.cache.retrieve(cacheAnnotation.category(), key);
                                Response.ResponseBuilder responseCacheBuilder = Response.ok().entity(responseContext.getEntity());
                                context.abortWith(responseCacheBuilder.build());
                            }
                        } catch (Throwable ex) {
                            LOGGER.error("Error while loading cache", ex);
                        }
                    }
                    break;

                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext context, ContainerResponseContext responseContext) throws IOException {
        if (cacheAnnotation != null) {
            switch (context.getRequest().getMethod()) {
                case HttpMethod.GET:
                    if (key != null && responseContext.getStatus() == Status.OK.getStatusCode()) {
                        this.cache.store(cacheAnnotation.category(), key, responseContext);
                    }
                    break;
                case HttpMethod.POST:
                    clear();
                    break;
                case HttpMethod.PUT:
                    clear();
                    break;
                case HttpMethod.DELETE:
                    clear();
                    break;
                default:
                    break;
            }
        }
    }

    private String getBase64(String input) {
        return new String(Base64.getEncoder().encode(input.getBytes()));
    }

    private String computeCacheKey(ContainerRequestContext context) throws Throwable {
        String langKey = getBase64(context.getHeaderString(org.apache.http.HttpHeaders.ACCEPT_LANGUAGE));
        String key = getBase64(context.getUriInfo().getRequestUri().toString()) + "|" + langKey;

        if (cacheAnnotation.userDependent() && context.getSecurityContext() != null && context.getSecurityContext().getUserPrincipal() != null) {
            String userId = getBase64(context.getSecurityContext().getUserPrincipal().getName());
            key = userId + "|" + key;
        }

        return key;
    }

    private void clear() {
        this.cache.remove(cacheAnnotation.category());
        for (String category : cacheAnnotation.clearCategories()) {
            this.cache.remove(category);
        }
    }

}
