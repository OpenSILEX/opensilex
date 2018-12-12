package phis2ws.service.resources.request.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;

/**
 * Filter for the pageSize max.
 * @author Arnaud Charleroy
 * @date 05/16
 * @update 07/16
 * @update [Morgane Vidal] 03 December, 2018 : simplify the pageSize query filter
 */
@Provider
public class PageSizeMaxRequestFilter implements ContainerRequestFilter {
    public final static int PAGE_SIZE_DEFAULT_LIMIT = 2097152; // 2 MB

    /**
     * Filter the pageSize parameter
     * @param requestContext
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        UriInfo uriInfo = requestContext.getUriInfo();
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        
        if (queryParameters.containsKey("pageSize")) {
            try {
                final int currentPageSize = Integer.valueOf(queryParameters.getFirst("pageSize"));
                if (currentPageSize > PAGE_SIZE_DEFAULT_LIMIT) {
                    final Status pageSizeError = new Status( "You are trying to retrieve to much result at one time.",StatusCodeMsg.ERR, "The current pageSize limit is : " + PAGE_SIZE_DEFAULT_LIMIT + " items");
                    requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(pageSizeError)).type(MediaType.APPLICATION_JSON).build());
                }
            } catch (java.lang.NumberFormatException ex) {
                final Status pageSizeError = new Status("Wrong page size format.",StatusCodeMsg.ERR, "It might be because you gave a page size higher than the current page size limit (" + PAGE_SIZE_DEFAULT_LIMIT + ")");
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(pageSizeError)).type(MediaType.APPLICATION_JSON).build());
            }
        }
    }
}
