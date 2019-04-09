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
import opensilex.service.dao.SessionDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionFactory;
import opensilex.service.injection.SessionInject;
import opensilex.service.injection.SessionInjectResolver;
import opensilex.service.resource.brapi.BrapiCall;
import opensilex.service.resource.brapi.CallsResourceService;
import opensilex.service.resource.brapi.StudyDetailsResourceService;
import opensilex.service.resource.brapi.TokenResourceService;
import opensilex.service.resource.brapi.VariableResourceService;
import opensilex.service.resource.brapi.TraitsResourceService;
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
                + "opensilex.service.resources;"
                + "opensilex.service.json;"
                + "opensilex.service.resources.request.filters");
        
        //Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion("1.0.2");
        beanConfig.setSchemes(new String[]{"http"});
        
        beanConfig.setHost(PropertiesFileManager.getConfigFileProperty("service", "host"));
        beanConfig.setBasePath(PropertiesFileManager.getConfigFileProperty("service", "basePath"));
       
        beanConfig.setResourcePackage("opensilex.service.resources");
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
                // BrAPI services injection
                bind(CallsResourceService.class).to(BrapiCall.class);
                bind(TokenResourceService.class).to(BrapiCall.class);
                bind(StudyDetailsResourceService.class).to(BrapiCall.class);
                bind(TraitsResourceService.class).to(BrapiCall.class);
                bind(VariableResourceService.class).to(BrapiCall.class);
            }
        });
    }
    
    /**
     * Initializes application
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
                            .entity(new ResponseFormPOST(new Status("Can't create log directory", StatusCodeMsg.ERR, null))).build());
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
