//**********************************************************************************************
//                                       GlobalWebserviceValues.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Recognize sessionInject utilisation in jersey resources
//***********************************************************************************************
package phis2ws.service.configuration;

import phis2ws.service.documentation.StatusCodeMsg;
import javax.ws.rs.core.Response;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;

/**
 * Définit des réponse qui seront utilisées par défaut dans le
 * WebService
 * @date 07/16
 * @author Arnaud CHARLEROY
 */
public final class GlobalWebserviceValues {

// BRAPI V1
    public static final String AUTHORIZATION_PROPERTY = "Authorization";
    public static final String AUTHENTICATION_SCHEME = "Bearer";
    public static final Response ACCESS_DENIED = Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ResponseFormGET(new Status("Access error",StatusCodeMsg.ERR, "You cannot access this resource"))).build();
    public static final Response ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN)
            .entity(new ResponseFormGET(new Status("Access error",StatusCodeMsg.ERR, "Access blocked for all users !!"))).build();

}
