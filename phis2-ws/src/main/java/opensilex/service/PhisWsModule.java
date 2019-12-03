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
import org.opensilex.OpenSilex;
import org.opensilex.bigdata.mongodb.MongoDBConfig;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.extensions.APIExtension;

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
    public void init() {
        OpenSilex app = OpenSilex.getInstance();
        RDF4JConfig rdf4jConfig = app.loadConfigPath("opensilex.sparql.rdf4j", RDF4JConfig.class);
        MongoDBConfig mongoConfig = app.loadConfigPath("opensilex.bigData.mongodb", MongoDBConfig.class);

        PropertiesFileManager.setOpensilexConfigs(
                getConfig(PhisWsConfig.class),
                rdf4jConfig,
                mongoConfig
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

}
