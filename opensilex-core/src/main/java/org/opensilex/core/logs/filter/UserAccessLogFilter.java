/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.filter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.opensilex.OpenSilex;
import org.opensilex.core.logs.dal.LogModel;
import org.opensilex.core.logs.dal.LogsDAO;
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.security.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
@Provider
public class UserAccessLogFilter implements ContainerRequestFilter {

    final static Logger LOGGER = LoggerFactory.getLogger(UserAccessLogFilter.class);
    @Context
    private HttpServletRequest servletRequest;

    final static String MAP_FIELD_QUERY_PARAMETERS = "queryParameters";
    final static String MAP_FIELD_RESSOURCE_PATH = "ressourcePath";

    @Inject
    NoSQLService nosql;

    @Inject
    OpenSilex opensilex;

    /**
     * Filters the session token.
     *
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // 1. retreive user informations
        UserModel user = (UserModel) requestContext.getSecurityContext().getUserPrincipal();

        if (!user.isAnonymous()) {
            //2 . check access log configuration
            final UriInfo uriInfo = requestContext.getUriInfo();
            final String resourcePath = uriInfo.getPath();
            String httpMethod = servletRequest.getMethod();

            //3 . check if the path equals to data service and sub services
            if (resourcePath != null
                    && httpMethod.equals("GET")) {
                MultivaluedMap<String, String> queryPathParameters = uriInfo.getQueryParameters();
                try {
                    // 6. save data search query
                    LogsDAO logsDAO = new LogsDAO(nosql);
                    LogModel logModel = new LogModel();
                    if (user != null && !user.isAnonymous()) { // TODO Remove verification
                        logModel.setUserUri(user.getUri());
                    }
                    logModel.setRemoteAdress(servletRequest.getRemoteAddr());
                    logModel.setRequest(resourcePath);

                    Map<String, Object> queryParmeters = new HashMap<>();
                    queryParmeters.put(MAP_FIELD_QUERY_PARAMETERS, queryPathParameters);
                    queryParmeters.put(MAP_FIELD_RESSOURCE_PATH, resourcePath);

                    logModel.setQueryParmeters(queryParmeters);
                    logModel.setDatetime(OffsetDateTime.now());
                    logsDAO.create(logModel);
                } catch (Exception ex) {
                    LOGGER.warn("Error while logging user access to service", ex);
                }
            }
        }
    }
}
