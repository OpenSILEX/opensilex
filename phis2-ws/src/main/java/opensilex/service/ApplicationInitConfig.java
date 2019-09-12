//******************************************************************************
//                            ApplicationInitConfig.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: January 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//*****************************************************************************
package opensilex.service;

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
import opensilex.service.authentication.Session;
import opensilex.service.authentication.TokenManager;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.eventListener.EventListener;
import opensilex.service.injection.SessionFactory;
import opensilex.service.injection.SessionInject;
import opensilex.service.injection.SessionInjectResolver;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * Application init configuration
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@ApplicationPath("/rest")
public class ApplicationInitConfig extends ResourceConfig {

    final static String PROPERTY_FILE_NAME = "service";
    final static Logger LOGGER = LoggerFactory.getLogger(ApplicationInitConfig.class);
    
    /**
     * Initializes application configuration
     */
    public ApplicationInitConfig() {
        packages("io.swagger.jaxrs.listing;"
                + "opensilex.service.resource;"
                + "opensilex.service.json;"
                + "opensilex.service.resource.request.filter");
        
        //Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[]{"http"});
        
        beanConfig.setHost(PropertiesFileManager.getConfigFileProperty("service", "host"));
        beanConfig.setBasePath(PropertiesFileManager.getConfigFileProperty("service", "basePath"));
       
        beanConfig.setResourcePackage("opensilex.service.resource");
        beanConfig.setScan(true);
        
        register(MultiPartFeature.class);
        register(JacksonFeature.class);
        
        /* Annotation SessionInject to get current session and user
        Link between the object creator from the client request and the application 
        @see https://jersey.java.net/documentation/latest/ioc.html */
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                
                // create session from given session id
                bindFactory(SessionFactory.class).to(Session.class);
                
                // Session injection from the defined type in SessionInjectResolver
                bind(SessionInjectResolver.class)
                        .to(new TypeLiteral<InjectionResolver<SessionInject>>() {
                        })
                        .in(Singleton.class);
            }
        });
        register(EventListener.class);
    }
    
    /**
     * Initializes application.
     */
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
                            .entity(new ResponseFormPOST(
                                    new Status("Can't create log directory", StatusCodeMsg.ERR, null))).build());
                }
            }
        }
        
        // apply the right rights on the log directory on the remote server
        try {
            Runtime.getRuntime().exec("chmod -R 755 " + logDirectory);
            LOGGER.info("Log directory rights successful update");
        } catch (IOException e) {
            LOGGER.error("Can't change rights on log directory");
        }
        TokenManager.Instance();
    }
}
