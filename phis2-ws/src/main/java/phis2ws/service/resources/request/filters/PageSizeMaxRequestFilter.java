//******************************************************************************
//                       PageSizeMaxRequestFilter.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 01 May 2016
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.request.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;

/**
 * Filter for the pageSize max.
 * @update [Morgane Vidal] 03 December, 2018: simplify the pageSize query filter
 * @update [Andréas Garcia] 28 Feb., 2019: get page size limit from config file
 * @author Arnaud Charleroy
 */
@Provider
public class PageSizeMaxRequestFilter implements ContainerRequestFilter {
    
    /**
     * To set the page size max constant only once during the first use of the 
     * filter, a boolean is used
     */
    public static boolean PAGE_SIZE_MAX_HAS_BEEN_SET_UP = false;
    public static int PAGE_SIZE_MAX;
    
    /**
     * Filter the pageSize parameter
     * @param requestContext
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!PAGE_SIZE_MAX_HAS_BEEN_SET_UP) {
            setPageSizeMaxFromConfigFile();
        }
        UriInfo uriInfo = requestContext.getUriInfo();
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        
        if (queryParameters.containsKey("pageSize")) {
            try {
                final int currentPageSize = Integer.valueOf(queryParameters.getFirst("pageSize"));
                if (currentPageSize > PAGE_SIZE_MAX) {
                    final Status pageSizeError = new Status( "You are trying to retrieve to much result at one time.",StatusCodeMsg.ERR, "The current pageSize limit is : " + PAGE_SIZE_MAX + " items");
                    requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(pageSizeError)).type(MediaType.APPLICATION_JSON).build());
                }
            } catch (java.lang.NumberFormatException ex) {
                final Status pageSizeError = new Status("Wrong page size format.",StatusCodeMsg.ERR, "It might be because you gave a page size higher than the current page size limit (" + PAGE_SIZE_MAX + ")");
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(pageSizeError)).type(MediaType.APPLICATION_JSON).build());
            }
        }
    }
    
    /**
     * Set the page size max value from the configuration file
     */
    private void setPageSizeMaxFromConfigFile() {        
        PAGE_SIZE_MAX = Integer.parseInt(PropertiesFileManager.getConfigFileProperty("service", "pageSizeMax"));
        PAGE_SIZE_MAX_HAS_BEEN_SET_UP = true;
    }
}
