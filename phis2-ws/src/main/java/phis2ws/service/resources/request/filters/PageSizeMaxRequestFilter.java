package phis2ws.service.resources.request.filters;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;

/**
 * Permet de filtrer les envois au Web Service grâce au header des requêtes
 * ainsi que d'autres paramètres
 *
 * @author Arnaud CHARLEROY
 * @date 05/16
 * @update 07/16
 */
@Provider
public class PageSizeMaxRequestFilter implements ContainerRequestFilter {

    final static Logger LOGGER = LoggerFactory.getLogger(PageSizeMaxRequestFilter.class);
    public final static int PAGE_SIZE_DEFAULT_LIMIT = 2097152; // 2 MB

    /**
     * Filtre pour la taille des pages
     *
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        UriBuilder b = requestContext.getUriInfo().getRequestUriBuilder();
        UriInfo uriInfo = requestContext.getUriInfo();
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
//        b.
//        b.replaceQueryParam(name, values)
        if (queryParameters.containsKey("pageSize")) {
            final int currentPageSize = Integer.valueOf(queryParameters.getFirst("pageSize"));
            final Integer contentLengthConfigValue = Integer.valueOf(PropertiesFileManager.getConfigFileProperty("service", "pageSizeMax"));
            int pageSizeMaxValue = 0;
            if (contentLengthConfigValue != null) {
                pageSizeMaxValue = contentLengthConfigValue;
            } else {
                pageSizeMaxValue = PAGE_SIZE_DEFAULT_LIMIT;
            }
            if (currentPageSize > pageSizeMaxValue) {
                final Status pageSizeError = new Status( "You are trying to retrieve to much result at one time.",StatusCodeMsg.ERR, "The current pageSize limit is : " + pageSizeMaxValue + " items");
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity(new ResponseFormGET(pageSizeError)).type(MediaType.APPLICATION_JSON).build());
            }
        }
    }
}
