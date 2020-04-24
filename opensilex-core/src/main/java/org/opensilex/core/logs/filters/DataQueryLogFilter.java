////******************************************************************************
////                     DataLogAccessFilter.java
//// SILEX-PHIS
//// Copyright © INRAE 2020
//// Creation date: February 2020
//// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
////******************************************************************************
//package org.opensilex.core.logs.filters;
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import javax.inject.Inject;
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.container.ContainerRequestContext;
//import javax.ws.rs.container.ContainerRequestFilter;
//import javax.ws.rs.container.ResourceInfo;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MultivaluedMap;
//import javax.ws.rs.core.UriInfo;
//import javax.ws.rs.ext.Provider;
//import org.opensilex.OpenSilex;
//import org.opensilex.OpenSilexModuleNotFoundException;
//import org.opensilex.core.CoreConfig;
//import org.opensilex.core.CoreModule;
//import org.opensilex.core.logs.dal.LogModel;
//import org.opensilex.core.logs.dal.LogsDao;
//import org.opensilex.nosql.service.NoSQLService;
//import org.opensilex.security.user.dal.UserModel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Filters web service requests to log query done on data.
// *
// * @author Arnaud Charleroy
// */
//@Provider
//public class DataQueryLogFilter implements ContainerRequestFilter {
//
//    final static Logger LOGGER = LoggerFactory.getLogger(DataQueryLogFilter.class);
//
//    @Context
//    private ResourceInfo resourceInfo;
//
//    @Context
//    private HttpServletRequest servletRequest;
//
//    @Inject
//    private OpenSilex opensilex;
//    
//    @Inject
//    private NoSQLService nosql; 
//
//    final static String MAP_FIELD_QUERY_PARAMETERS = "queryParameters";
//    final static String MAP_FIELD_RESSOURCE_PATH = "ressourcePath";
//
//    /**
//     * Filters the session token.
//     *
//     * @param requestContext
//     * @throws IOException
//     */
//    @Override
//    public void filter(ContainerRequestContext requestContext) throws IOException {
//        //1 . get ressource path
//        final UriInfo uriInfo = requestContext.getUriInfo();
//        final String resourcePath = uriInfo.getPath();
//        String httpMethod = servletRequest.getMethod();
//        LOGGER.debug("httpMethod" + httpMethod);
//        //2 . check access log configuration
//        CoreConfig moduleConfig;
//        try {
//            moduleConfig = opensilex.getModuleConfig(CoreModule.class, CoreConfig.class);
//            if (moduleConfig.enableLogs()) {
//                //3 . check if the path equals to data service and sub services
//                 LOGGER.debug("resourcePath" + resourcePath);
//                if (resourcePath != null
//                        && httpMethod.equals("GET")
//                        && !resourcePath.contains("querylog")) {
//
//                    MultivaluedMap<String, String> queryPathParameters = uriInfo.getQueryParameters();
//
//                    // 5. load user model
//                    UserModel user = (UserModel) requestContext.getSecurityContext().getUserPrincipal();
////                    NoSQLService nosql = opensilex.getServiceInstance(NoSQLService.DEFAULT_NOSQL_SERVICE, NoSQLService.class);
//                     LOGGER.debug("resourcePath" + nosql.toString());
//                     
//                    // 6. save data search query
//                    LogsDao logsDao = new LogsDao(nosql);
//                    LogModel log = new LogModel();
//                    Map<String, Object> queryParmeters = new HashMap<>();
//                    queryParmeters.put(MAP_FIELD_QUERY_PARAMETERS, queryPathParameters);
//                    queryParmeters.put(MAP_FIELD_RESSOURCE_PATH, resourcePath);
//                    Date now = new Date();
//                    log.setDate(now);
//                    log.setQuery(queryParmeters);
//                    log.setUserUri(user.getUri());
//                    log.setRemoteAdress(servletRequest.getRemoteAddr());
//                    try {
//                        logsDao.create(log);
//                    } catch (Exception ex) {
//                        LOGGER.error(ex.getMessage(), ex);
//                    }
//                }
//            }
//        } catch (OpenSilexModuleNotFoundException ex) {
//            LOGGER.error(ex.getMessage(), ex);
//        }
//    }
//}
