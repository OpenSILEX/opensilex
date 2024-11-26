//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest;

import io.swagger.jaxrs.config.BeanConfig;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceFactory;
import org.opensilex.service.reflection.SelfBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * This class is the main entry point of OpenSILEX REST API.
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
 * @see org.opensilex.server.extensions.APIExtension
 * @author Vincent Migot
 */
@ApplicationPath("/rest")
@Singleton
public class RestApplication extends ResourceConfig {

    /**
     * Class Logger.
     */
    final private static Logger LOGGER = LoggerFactory.getLogger(ResourceConfig.class);

    /**
     * Reference to the main application.
     */
    private OpenSilex opensilex;

    /**
     * Main constructor.
     *
     * @param ctx servlet context to get OpenSilex instance reference
     * @throws Exception
     */
    public RestApplication(@Context ServletContext ctx) throws Exception {
        this((OpenSilex) ctx.getAttribute("opensilex"));
    }

    /**
     * Constructor for opensilex Application.
     * <pre>
     * - Load core configuration
     * - register packages for API classes implementing APIExtension
     * - initialize swagger
     * - register services for injection
     * - register modules classes for injection
     * - call all modules initRestApplication method
     * </pre>
     *
     * @param opensilex OpenSilex instance
     * @throws Exception If any initialization error occured for Jersey Rest Application
     */
    public RestApplication(OpenSilex opensilex) throws Exception {
        this.opensilex = opensilex;

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        // Register JSON Multipart Feature for Jersey to allow MediaType.MULTIPART_FORM_DATA for file upload
        register(MultiPartFeature.class);

        // Enable Jackson JSON serialier/deserializer for Jersey
        register(ObjectMapperContextResolver.class);

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
     * Initialize packages list to scan for services and request filters (A.K.A. components) from all modules.
     *
     * @see https://jersey.github.io/apidocs/2.28/jersey/org/glassfish/jersey/server/ResourceConfig.html#packages-boolean-java.lang.String...-
     */
    private void registerAPI() {
        List<String> packageList = new ArrayList<>();

        // Get packages list from every modules implementing APIExtension
        getAPIExtensionModules().forEach((APIExtension api) -> {
            packageList.addAll(api.getPackagesToScan());
        });

        // Add package list for components scan
        LOGGER.info("Registred packages:\n" + String.join("\n", packageList));
        packages(String.join(";", packageList));

    }

    /**
     * Initialize swagger UI registering every packages to scan for services defined in modules.
     */
    private void initSwagger() {
        // Load all packages to scan from modules implementing APIExtension
        List<String> packageList = new ArrayList<>();

        getAPIExtensionModules().forEach((APIExtension api) -> {
            packageList.addAll(api.apiPackages());
        });
        
        packageList.add("org.opensilex.server.rest.serialization");
        
        // Init swagger UI
        BeanConfig beanConfig = new BeanConfig();

        try {
            beanConfig.setVersion(opensilex.getModuleByClass(ServerModule.class).getOpenSilexVersion());
        } catch (OpenSilexModuleNotFoundException ex) {
            LOGGER.warn("Error while getting API version", ex);
        }
        beanConfig.setResourcePackage(String.join(",", packageList));
        beanConfig.setTitle("OpenSilex API");
        beanConfig.setExpandSuperTypes(false);
        beanConfig.setScan(true);
    }

    /**
     * Return list of modules implementing APIExtension.
     *
     * @return List of modules as APIExtension
     */
    private List<APIExtension> getAPIExtensionModules() {
        return opensilex.getModulesImplementingInterface(APIExtension.class);
    }

    /**
     * Call start method of every OpenSILEX modules.
     */
    private void initModules() {
        getAPIExtensionModules().forEach((APIExtension api) -> {
            api.initRestApplication(this);
        });
    }

    /**
     * Register services for injection.
     */
    private void registerServices() {
        // Create and register binding for service injection
        register(new AbstractBinder() {
            @Override
            // This suppress warning is for module injection cast in loop
            @SuppressWarnings("unchecked")
            protected void configure() {
                // Make opensilex instance injectable
                bind(opensilex).to(OpenSilex.class);

                // Make every module and their config injectable
                for (OpenSilexModule module : opensilex.getModules()) {
                    bind(module).to((Class<? super OpenSilexModule>) module.getClass());

                    // Make the module config injectable
                    Class<?> moduleConfigClass = module.getConfigClass();
                    if (moduleConfigClass != null) {
                        bind(module.getConfig()).to((Class<? super Object>) moduleConfigClass);
                    }
                }

                // Make every service injectable
                opensilex.getServiceManager().forEachInterface((Class<? extends Service> serviceClass, Map<String, Service> implementations) -> {
                    implementations.forEach((String name, Service implementation) -> {
                        if (implementation instanceof ServiceFactory) {
                            ServiceFactory<? extends Service> factory = (ServiceFactory<? extends Service>) implementation;
                            bindFactory(factory).to((Class<? super Service>) factory.getServiceClass())
                                    .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
                        } else {
                            bind(implementation).named(name).to((Class<? super Service>) serviceClass);
                        }
                    });
                });

                // Make every self-bound service injectable
                opensilex.getAnnotatedClasses(SelfBound.class).forEach(serviceClass -> {
                    if (serviceClass.isAnnotationPresent(org.jvnet.hk2.annotations.Service.class)) {
                        bindAsContract(serviceClass).in(RequestScoped.class);
                    }
                });

                getAPIExtensionModules().forEach((APIExtension api) -> {
                    api.bindServices(this);
                });
            }
        });

    }
}
