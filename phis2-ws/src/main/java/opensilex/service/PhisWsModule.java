//******************************************************************************
//                           PhisWsModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.auth0.jwt.JWTCreator;
import org.opensilex.OpenSilex;
import org.opensilex.rest.extensions.LoginExtension;
import org.opensilex.rest.group.dal.GroupDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.module.ModuleConfig;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.rest.extensions.APIExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

import static org.opensilex.core.CoreModule.TOKEN_USER_GROUP_URIS;

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
    public void startup() {
        OpenSilex app = OpenSilex.getInstance();

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

}
