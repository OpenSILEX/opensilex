//**********************************************************************************************
//                                       ApplicationInitConfig.java 
//
// Author(s): Morgane Vidal, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: january 2017
// Contact:morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  june, 2018
// Subject: Configuration of the webservice
//***********************************************************************************************

package phis2ws.service;

import io.swagger.jaxrs.config.BeanConfig;
import java.io.File;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.authentication.TokenManager;
import phis2ws.service.dao.phis.SessionDaoPhisBrapi;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionFactory;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.injection.SessionInjectResolver;
import phis2ws.service.resources.brapi.BrapiCall;
import phis2ws.service.resources.brapi.CallsResourceService;
import phis2ws.service.resources.brapi.StudyResourceService;
import phis2ws.service.resources.brapi.TokenResourceService;
import phis2ws.service.resources.brapi.VariableResourceService;
import phis2ws.service.resources.brapi.TraitsResourceService;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormPOST;

@ApplicationPath("/rest")
public class ApplicationInitConfig extends ResourceConfig {

    final static String PROPERTY_FILE_NAME = "service";
    final static Logger LOGGER = LoggerFactory.getLogger(ApplicationInitConfig.class);

    
    public ApplicationInitConfig() {
        packages("io.swagger.jaxrs.listing;"
                + "phis2ws.service.resources;"
                + "phis2ws.service.json;"
                + "phis2ws.service.resources.request.filters");
        
        //Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[]{"http"});
        
        beanConfig.setHost(PropertiesFileManager.getConfigFileProperty("service", "host"));
        beanConfig.setBasePath(PropertiesFileManager.getConfigFileProperty("service", "basePath"));
       
        beanConfig.setResourcePackage("phis2ws.service.resources");
        beanConfig.setScan(true);
        
        register(MultiPartFeature.class);
        register(JacksonFeature.class);
        
        // Annotation SessionInject pour obtenir la session en cours et l'utilisateur
        // Liaison entre le createur d'objet a partir de la requete du client et l'application
        // @see https://jersey.java.net/documentation/latest/ioc.html
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                // cree la session a partir du sessionId reçu
                bindFactory(SessionFactory.class).to(Session.class);
                // Injection de la session grace au type definit dans SessionInjectResolver
                bind(SessionInjectResolver.class)
                        .to(new TypeLiteral<InjectionResolver<SessionInject>>() {
                        })
                        .in(Singleton.class); 
                //Brapi services injection
                bind(CallsResourceService.class).to(BrapiCall.class);
                bind(TokenResourceService.class).to(BrapiCall.class);
                bind(StudyResourceService.class).to(BrapiCall.class);
                bind(TraitsResourceService.class).to(BrapiCall.class);
                bind(VariableResourceService.class).to(BrapiCall.class);
            }
        });
    }
    
    @PostConstruct
    public static void initialize() {
        final String logDirectory = PropertiesFileManager.getConfigFileProperty("service", "logDirectory");
        if (logDirectory != null) {
            File logDir = new File(logDirectory);
            if (!logDir.isDirectory()) {
                if (!logDir.mkdirs()) {
                    LOGGER.error("Can't create log directory");
                    throw new WebApplicationException(
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ResponseFormPOST(new Status("Can't create log directory", StatusCodeMsg.ERR, null))).build());
                }
            }
        }
        // make the good rights on log directory on remote server
        try {
            Runtime.getRuntime().exec("chmod -R 755 " + logDirectory);
            LOGGER.info("Log directory rights successful update");
        } catch (IOException e) {
            LOGGER.error("Can't change rights on log directory");
        }
        TokenManager.Instance();
        SessionDaoPhisBrapi sessionDao = new SessionDaoPhisBrapi();
        // It's possible to reload a session that hasn't been desactived
//        sessionDao.reloadActiveSession();
    }
}
