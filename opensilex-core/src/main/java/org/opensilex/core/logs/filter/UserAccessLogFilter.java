/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.bson.Document;
import org.opensilex.OpenSilex;
import org.opensilex.core.logs.dal.LogModel;
import org.opensilex.core.logs.dal.LogsDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
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
    MongoDBService nosql;

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
        AccountModel user = (AccountModel) requestContext.getSecurityContext().getUserPrincipal();

        if (!user.isAnonymous()) {
            //2 . check access log configuration
            final UriInfo uriInfo = requestContext.getUriInfo();
            final String resourcePath = uriInfo.getPath();
            String httpMethod = servletRequest.getMethod();
            LOGGER.warn(httpMethod);
            //3 . check if the path equals to data service and sub services
            if (resourcePath != null
                    && resourcePath.contains("core/data")
                    && httpMethod.equals("GET")) {
                MultivaluedMap<String, String> queryPathParameters = uriInfo.getQueryParameters();
                try {
                    // 6. save data search query
                    LogsDAO logsDAO = new LogsDAO(nosql);
                    LogModel logModel = new LogModel();
                    logModel.setUserUri(user.getUri());
                    logModel.setRemoteAdress(servletRequest.getRemoteAddr());
                    logModel.setRequest(resourcePath);
                    Document queryParams = new Document();
                    for (String key: queryPathParameters.keySet()) {
                        queryParams.put(key, queryPathParameters.get(key));
                    }
                    logModel.setQueryParameters(queryParams);
                    logModel.setDatetime(LocalDateTime.now());
                    logsDAO.create(logModel);
                } catch (Exception ex) {
                    LOGGER.warn("Error while logging user access to service", ex);
                }
            }
        }
    }
}
