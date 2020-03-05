//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest;

import io.swagger.jaxrs.config.BeanConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.extensions.APIExtension;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * This class is the main entry point of OpenSILEX REST API:
 * - extends Jersey ResourceConfig
 * - can automatically get OpenSilex instance via ServletContext or through constructor argument
 * - configure all Jersey features and options in constructor
 * - register API classes implementing APIExtension
 * - initialize swagger
 * - register services for injection
 * - call module initialization for classes implementing APIExtension
 *
 * see: https://jersey.github.io/
 * </pre>
 *
 * @see org.opensilex.rest.extensions.APIExtension
 * @author Vincent Migot
 */
@ApplicationPath("/rest")
@Singleton
public class RestApplication extends ResourceConfig {

    final private static Logger LOGGER = LoggerFactory.getLogger(ResourceConfig.class);

    /**
     * Reference to the main application
     */
    private OpenSilex app;

    public RestApplication(@Context ServletContext ctx) {
        this((OpenSilex) ctx.getAttribute("opensilex"));
    }

    /**
     * Constructor for opensilex Application:
     * <pre>
     * - Load core configuration
     * - register packages for API classes implementing APIExtension
     * - initialize swagger
     * - register services for injection
     * - register modules classes for injection
     * - call all modules initAPI method
     * </pre>
     *
     * @param app OpenSilex instance
     */
    public RestApplication(OpenSilex app) {
        this.app = app;

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // Register JSON Multipart Feature for Jersey to allow MediaType.MULTIPART_FORM_DATA for file upload
        register(MultiPartFeature.class);

        // Enable Jackson JSON serialier/deserializer for Jersey
        register(JacksonFeature.class);

        // Enable GZIP for web services
        register(GZipEncoder.class);
        register(EncodingFilter.class);

        // Register all module packages needed for service initialization
        registerAPI();

        // Initialize swagger API
        initSwagger();

        registerServices();

        // Allow all modules to do custom initialization
        initModules();
    }

    /**
     * Initialize packages list to scan for services and request filters (A.K.A.
     * components) from all modules
     *
     * @see
     * https://jersey.github.io/apidocs/2.28/jersey/org/glassfish/jersey/server/ResourceConfig.html#packages-boolean-java.lang.String...-
     */
    private void registerAPI() {
        ArrayList<String> packageList = new ArrayList<>();

        // Get packages list from every modules implementing APIExtension
        getAPIExtensionModules().forEach((APIExtension api) -> {
            packageList.addAll(api.getPackagesToScan());
        });

        // Add package list for components scan
        LOGGER.info("Registred packages:\n" + String.join("\n", packageList));
        packages(String.join(";", packageList));

    }

    /**
     * Initialize swagger UI registering every packages to scan for services
     * defined in modules
     */
    private void initSwagger() {
        // Load all packages to scan from modules implementing APIExtension
        ArrayList<String> packageList = new ArrayList<>();

        getAPIExtensionModules().forEach((APIExtension api) -> {
            packageList.addAll(api.apiPackages());
        });

        // Init swagger UI
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(app.getModulesByProjectId("opensilex-rest").get(0).getOpenSilexVersion());
        beanConfig.setResourcePackage(String.join(",", packageList));
        beanConfig.setTitle("OpenSilex API");
        beanConfig.setExpandSuperTypes(false);
        beanConfig.setScan(true);
    }

    /**
     * Return list of modules implementing APIExtension
     *
     * @return List of modules as APIExtension
     */
    private List<APIExtension> getAPIExtensionModules() {
        return app.getModulesImplementingInterface(APIExtension.class);
    }

    /**
     * Call start method of every OpenSILEX modules
     */
    private void initModules() {
        getAPIExtensionModules().forEach((APIExtension api) -> {
            api.initAPI(this);
        });
    }

    private void registerServices() {
        // Create and register binding for service injection
        register(new AbstractBinder() {
            @Override
            // This suppress warning is for module injection cast in loop
            @SuppressWarnings("unchecked")
            protected void configure() {
                // Make opensilex instance injectable
                bind(app).to(OpenSilex.class);

                // Make every module injectable
                for (OpenSilexModule module : app.getModules()) {
                    bind(module).to((Class<? super OpenSilexModule>) module.getClass());
                }
                
                // Make every service injectable
                app.getServiceManager().forEachInterface((Class<? extends Service> serviceClass, Map<String, Service> implementations) -> {
                    implementations.forEach((String name, Service implementation) -> {
                        if (implementation instanceof ServiceFactory) {
                            ServiceFactory<? extends Service> factory = (ServiceFactory<? extends Service>) implementation;
                            bindFactory(factory).to((Class<? super Service>) factory.getServiceClass())
                                .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
                        }else {
                            bind(implementation).named(name).to((Class<? super Service>) serviceClass);
                        }
                    });
                });
            }
        });

    }
}
