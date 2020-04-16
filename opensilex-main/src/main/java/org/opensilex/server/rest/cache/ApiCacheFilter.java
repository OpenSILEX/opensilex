//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
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
 * Jersey filter for Api Cache.
 *
 * @author Vincent Migot
 */
@Provider
@Priority(Priorities.ENTITY_CODER)
public class ApiCacheFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCacheFilter.class);

    /**
     * Cache service.
     */
    @Inject
    private ApiCacheService cache;

    /**
     * Information on matched API.
     */
    @Context
    private ResourceInfo resourceInfo;

    /**
     * Current cache annotation.
     */
    private ApiCache cacheAnnotation;

    /**
     * Current cache key.
     */
    private String key;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        Method apiMethod = resourceInfo.getResourceMethod();
        if (apiMethod != null) {
            cacheAnnotation = apiMethod.getAnnotation(ApiCache.class);
            if (cacheAnnotation != null) {
                switch (context.getRequest().getMethod()) {
                    case HttpMethod.GET:
                        loadCacheIfExists(context);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    /**
     * Load cache corresponding to given context.
     *
     * @param context request context
     */
    private void loadCacheIfExists(ContainerRequestContext context) {
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

    /**
     * Compute Base64 string.
     *
     * @param input string to encode
     * @return encoded string
     */
    private String getBase64(String input) {
        return new String(Base64.getEncoder().encode(input.getBytes()));
    }

    /**
     * Compute current API cache key.
     *
     * @param context request context.
     * @return request cache key.
     * @throws Throwable
     */
    private String computeCacheKey(ContainerRequestContext context) throws Throwable {
        String langKey = getBase64(context.getHeaderString(org.apache.http.HttpHeaders.ACCEPT_LANGUAGE));
        String key = getBase64(context.getUriInfo().getRequestUri().toString()) + "|" + langKey;

        if (cacheAnnotation.userDependent() && context.getSecurityContext() != null && context.getSecurityContext().getUserPrincipal() != null) {
            String userId = getBase64(context.getSecurityContext().getUserPrincipal().getName());
            key = userId + "|" + key;
        }

        return key;
    }

    /**
     * Clear cache corresponding to current cache annotation.
     */
    private void clear() {
        this.cache.remove(cacheAnnotation.category());
        for (String category : cacheAnnotation.clearCategories()) {
            this.cache.remove(category);
        }
    }

}
