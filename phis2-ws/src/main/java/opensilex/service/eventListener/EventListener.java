//******************************************************************************
//                                ApplicationEventListener.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 sept. 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.eventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import opensilex.service.PropertiesFileManager;
import opensilex.service.shinyProxy.ShinyProxyService;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventListener
 * Represents events that propagate by the webservice
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class EventListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);

    @Override
    public void onEvent(ApplicationEvent applicationEvent) {
        ShinyProxyService shinyProxyProcess = new ShinyProxyService();
        switch (applicationEvent.getType()) {
            case INITIALIZATION_START:
                LOGGER.info("Initialization OpenSILEX WS started");
                break;
            case INITIALIZATION_FINISHED:
                LOGGER.info("Initialization OpenSILEX WS finished");
                break;
            case INITIALIZATION_APP_FINISHED:
                LOGGER.info("Initialization of OpenSILEX WS finished");
                boolean shinyproxyMustBeActived = Boolean.valueOf(
                        PropertiesFileManager.getConfigFileProperty(
                                "data_analysis_config",
                                "shinyproxy.run"
                        ));
                if (shinyproxyMustBeActived) {
                    CompletableFuture<String> completableFuture
                            = new CompletableFuture<>();
                    Executors.newCachedThreadPool().submit(() -> {
                        shinyProxyProcess.reload();
                        completableFuture.complete("Shiny Proxy Complete");
                        return null;
                    });
                }
                break;
            case RELOAD_FINISHED:
                LOGGER.info("Reload OpenSILEX WS completed");
                break;
            case DESTROY_FINISHED:
                LOGGER.info("Destroying ShinyProxy Process completed");
                shinyProxyProcess.stop();
                LOGGER.info("Destroy OpenSILEX WS completed");
                break;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }
}
