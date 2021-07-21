//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.net.URI;
import org.opensilex.OpenSilexModule;

import java.util.List;
import javax.inject.Inject;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OA;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.provenance.api.ProvenanceAPI;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.NoSQLModule;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.extensions.LoginExtension;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.cache.JCSApiCacheExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, SPARQLExtension, JCSApiCacheExtension {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    @Override
    public Class<?> getConfigClass() {
        return CoreConfig.class;
    }

    @Override
    public String getConfigId() {
        return "core";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();

        if (getConfig(CoreConfig.class).enableLogs()) {
            list.add("org.opensilex.core.logs.filter");
        }

        return list;
    }

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = SPARQLExtension.super.getOntologiesFiles();
        list.add(new OntologyFileDefinition(
                "http://aims.fao.org/aos/agrovoc/factors",
                "ontologies/agrovoc-factors.rdf",
                Lang.RDFXML,
                "agrovoc"
        ));
        list.add(new OntologyFileDefinition(
                OA.NS,
                "ontologies/oa.rdf",
                Lang.RDFXML,
                "oa"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso#",
                "ontologies/oeso-core.owl",
                Lang.RDFXML,
                "vocabulary"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeev#",
                "ontologies/oeev.owl",
                Lang.RDFXML,
                "oeev"
        ));
        return list;
    }

    @Override
    public void setup() throws Exception {
        SPARQLService.addPrefix(Oeso.PREFIX, Oeso.NS);
        SPARQLService.addPrefix(Oeev.PREFIX, Oeev.NS);
        SPARQLService.addPrefix(Time.PREFIX,Time.NS);
        URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping(), true);
        SPARQLDeserializers.registerDatatypeClass(Oeso.longString, String.class);
    }
    
    @Override
    public void install(boolean reset) throws Exception {
        insertDefaultProvenance();
    }

    private void insertDefaultProvenance() throws Exception {
        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        MongoDBConfig config = getOpenSilex().loadConfigPath("big-data.mongodb.config", MongoDBConfig.class);
        MongoClient mongo = MongoDBService.getMongoDBClient(config);
        MongoDatabase db = mongo.getDatabase(config.database());
        ProvenanceModel provenance = new ProvenanceModel();
        String name = "standard_provenance";
        provenance.setUri(new URI(sparqlConfig.baseURI() + ProvenanceDAO.PROVENANCE_PREFIX + "/" + name));
        provenance.setName(name);
        provenance.setDescription("This provenance is used when there is no need to describe a specific provenance");
        LOGGER.info("Insert default provenance: " + provenance.getUri());
        try {
           db.getCollection(ProvenanceDAO.PROVENANCE_COLLECTION_NAME, ProvenanceModel.class).insertOne(provenance); 
        } catch (Exception e) {
           LOGGER.warn("Couldn't create default provenance : " + e.getMessage());
        }
    }

}
