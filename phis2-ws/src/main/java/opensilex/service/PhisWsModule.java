//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service;

import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;
import opensilex.service.authentication.Session;
import opensilex.service.injection.SessionFactory;
import opensilex.service.injection.SessionInject;
import opensilex.service.injection.SessionInjectResolver;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opensilex.bigdata.mongodb.MongoDBConfig;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.extensions.APIExtension;
import org.opensilex.server.rest.RestApplication;


/**
 * Phis opensilex module implementation
 */
public class PhisWsModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return PhisWsConfig.class;
    }

    @Override
    public String getConfigId() {
        return "phisws";
    }
    
    @Override
    public void initAPI(RestApplication resourceConfig) {
        RDF4JConfig rdf4jConfig = app.loadConfig("opensilex-core-service-sparql", RDF4JConfig.class);
        MongoDBConfig mongoConfig = app.loadConfig("opensilex-core-service-bigdata", MongoDBConfig.class);
        
        PropertiesFileManager.setOpensilexConfigs(
                getConfig(PhisWsConfig.class),
                rdf4jConfig,
                mongoConfig
                
        );

        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                // cree la session a partir du sessionId reçu
                bindFactory(SessionFactory.class).to(Session.class);
                // Injection de la session grace au type definit dans SessionInjectResolver
                bind(SessionInjectResolver.class)
                        .to(new TypeLiteral<InjectionResolver<SessionInject>>() {
                        })
                        .in(Singleton.class);
            }
        });
    }

    @Override
    public List<String> apiPackages() {
        return Arrays.asList(new String[]{
            "opensilex.service.resource"
        });
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("opensilex.service.json");
        list.add("opensilex.service.resource.request.filters");

        return list;
    }

}
