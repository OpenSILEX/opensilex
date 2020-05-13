//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import opensilex.service.shinyProxy.ShinyProxyService;
import org.apache.jena.riot.Lang;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.server.ServerConfig;
import org.opensilex.server.ServerModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

/**
 * Phis opensilex module implementation
 */
public class PhisWsModule extends OpenSilexModule implements APIExtension, SPARQLExtension {

    @Override
    public Class<?> getConfigClass() {
        return PhisWsConfig.class;
    }

    @Override
    public String getConfigId() {
        return "phisws";
    }

    @Override
    public void setup() throws Exception {
        OpenSilex app = getOpenSilex();

        PropertiesFileManager.setOpensilexConfigs(
                getConfig(PhisWsConfig.class),
                app.loadConfigPath("ontologies.sparql.rdf4j", RDF4JConfig.class),
                app.loadConfigPath("ontologies", SPARQLConfig.class),
                app.loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class),
                app.loadConfigPath("file-system.storageBasePath", String.class),
                app.loadConfigPath("server.publicURI", String.class)
        );
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("opensilex.service.json");
        list.add("opensilex.service.resource.validation");
        list.add("opensilex.service.resource.request.filters");

        return list;
    }

    @Override
    public Set<String> apiPackages() {
        Set<String> packageSet = APIExtension.super.apiPackages();
        packageSet.add("opensilex.service.resource");
        return packageSet;
    }

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso-phis#",
                "ontologies/oeso-phis.owl",
                Lang.RDFXML,
                "oeso"
        ));
        list.add(new OntologyFileDefinition(
                sparqlConfig.baseURI() + "species",
                "ontologies/species.ttl",
                Lang.TTL,
                sparqlConfig.baseURIAlias() + "-species"
        ));
        return list;
    }

    @Override
    public void startup() throws Exception {
        PhisWsConfig phisConfig = (PhisWsConfig) this.getConfig();
        if (phisConfig.enableShinyProxy()) {
            this.startupShinyProxy(phisConfig);
        }
    }

    @Override
    public void shutdown() throws Exception {
        PhisWsConfig phisConfig = (PhisWsConfig) this.getConfig();
        if (phisConfig.enableShinyProxy()) {
            ShinyProxyService shinyProxyProcess = new ShinyProxyService();
            shinyProxyProcess.stop();
        }
    }

    private void startupShinyProxy(PhisWsConfig phisConfig) throws OpenSilexModuleNotFoundException, IOException {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        ServerConfig serverConfig = getOpenSilex().getModuleConfig(ServerModule.class, ServerConfig.class);
        ShinyProxyService shinyProxyProcess = new ShinyProxyService();
        ShinyProxyService.STARTED = true;
        ShinyProxyService.HOST = phisConfig.shinyProxyHost();
        ShinyProxyService.PORT = phisConfig.shinyProxyPort();
        ShinyProxyService.WS_HOST = serverConfig.publicURI();
        ShinyProxyService.sparql = sparql;
        System.out.println("opensilex.service.PhisWsModule.startup()" + ShinyProxyService.WS_HOST);
        System.out.println("opensilex.service.PhisWsModule.startup()" + ShinyProxyService.HOST);

        System.out.println("opensilex.service.PhisWsModule.startup()" + ShinyProxyService.WS_HOST);
        shinyProxyProcess.initialize();
        shinyProxyProcess.reload();
    }

}
