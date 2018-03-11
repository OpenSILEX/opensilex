//**********************************************************************************************
//                                       SessionFactory.java 
//
// Author(s): Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: custom HK2 Factory implementation that knows how to extract from http session
// @see https://jersey.java.net/documentation/latest/ioc.html
//***********************************************************************************************
package phis2ws.service.injection;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.authentication.TokenManager;
import phis2ws.service.configuration.GlobalWebserviceValues;

/**
 * Extrait les donnees de la requete de l'utilisateur 
 * et les lie à l'objet de l'annotation personnalisee @SessionInject
 * @author A. Charleroy
 */
public class SessionFactory implements Factory<Session> {

    final static Logger logger = LoggerFactory.getLogger(SessionFactory.class);

    private final HttpServletRequest request;

    @Inject
    public SessionFactory(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Session provide() {
        if (request != null) {
            String sessionId = request.getHeader(GlobalWebserviceValues.AUTHORIZATION_PROPERTY);
            if (sessionId != null) {
                sessionId = sessionId.replace(GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ", "");
                if (TokenManager.Instance().checkAuthentification(sessionId)) {
                    return TokenManager.Instance().getSession(sessionId);
                }
            }
        }
        return null;
    }

    @Override
    public void dispose(Session t) {
    }
}