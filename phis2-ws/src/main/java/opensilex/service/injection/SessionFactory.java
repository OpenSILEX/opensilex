//******************************************************************************
//                            SessionFactory.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.injection;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.authentication.Session;
import opensilex.service.authentication.TokenManager;
import opensilex.service.configuration.GlobalWebserviceValues;

/**
 * Links user information to session injection.
 * @see https://jersey.java.net/documentation/latest/ioc.html
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class SessionFactory implements Factory<Session> {

    final static Logger LOGGER = LoggerFactory.getLogger(SessionFactory.class);

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
                if (TokenManager.Instance().checkAuthentication(sessionId)) {
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