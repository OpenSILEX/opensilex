/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.filter;

import com.google.common.reflect.ClassPath.ResourceInfo;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.CoreConfig;
import org.opensilex.core.CoreModule;
import org.opensilex.core.logs.dal.LogModel;
import org.opensilex.core.logs.dal.LogsDAO;
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author charlero
 */
@Provider
public class LogFilter implements ContainerRequestFilter {

    final static Logger LOGGER = LoggerFactory.getLogger(LogFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest servletRequest;

    final static String MAP_FIELD_QUERY_PARAMETERS = "queryParameters";
    final static String MAP_FIELD_RESSOURCE_PATH = "ressourcePath";
    @Inject
    AuthenticationService authentication;

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
        if (user != null && !user.isAnonymous()) {
            boolean enableLogs = false;
            try {
                CoreConfig moduleConfig = opensilex.getModuleConfig(CoreModule.class, CoreConfig.class);
                enableLogs = moduleConfig.enableLogs();
            } catch (OpenSilexModuleNotFoundException e) {
                LOGGER.error(e.getMessage(),e);
            }

            //2 . check access log configuration
            if (enableLogs) {
                final UriInfo uriInfo = requestContext.getUriInfo();
                final String resourcePath = uriInfo.getPath();
                String httpMethod = servletRequest.getMethod();
                
                    //3 . check if the path equals to data service and sub services
                if (resourcePath != null
                        && httpMethod.equals("GET")) {
                    MultivaluedMap<String, String> queryPathParameters = uriInfo.getQueryParameters();
                    LOGGER.debug("filter" + queryPathParameters.toString());
 
                    LOGGER.debug("filter");
                    try {
                        // 6. save data search query
                        LogsDAO logsDAO = new LogsDAO(nosql);
                        LogModel logModel = new LogModel();
                        logModel.setUserUri(user.getUri().toString());
                        logModel.setRemoteAdress(servletRequest.getRemoteAddr());
                        logModel.setRequest(resourcePath);

                        Map<String, Object> queryParmeters = new HashMap<>();
                        queryParmeters.put(MAP_FIELD_QUERY_PARAMETERS, queryPathParameters);
                        queryParmeters.put(MAP_FIELD_RESSOURCE_PATH, resourcePath);

                        logModel.setQueryParmeters(queryParmeters);
                        logModel.setDate(new Date());
                        logsDAO.create(logModel);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
            }
        }
    }
}
