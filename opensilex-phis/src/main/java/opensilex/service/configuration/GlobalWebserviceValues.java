//******************************************************************************
//                          GlobalWebserviceValues.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

import opensilex.service.documentation.StatusCodeMsg;
import javax.ws.rs.core.Response;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormGET;

/**
 * Global web service values.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public final class GlobalWebserviceValues {

    public static final String AUTHORIZATION_PROPERTY = "Authorization";
    public static final String AUTHENTICATION_SCHEME = "Bearer";
    public static final Response ACCESS_DENIED = Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ResponseFormGET(new Status("Access error",StatusCodeMsg.ERR, "You cannot access this resource"))).build();
    public static final Response ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN)
            .entity(new ResponseFormGET(new Status("Access error",StatusCodeMsg.ERR, "Access blocked for all users !!"))).build();

    public static final String DATA_TYPE_STRING = "string";
    public static final String HEADER = "header";
    public static final String AUTHORIZATION = "Authorization";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE = "page";
}
