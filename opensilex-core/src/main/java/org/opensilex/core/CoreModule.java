//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.net.URI;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OA;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.ontology.dal.cache.*;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.InterestEntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupDAO;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.cache.JCSApiCacheExtension;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, SPARQLExtension, JCSApiCacheExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);
    private static final String ONTOLOGIES_DIRECTORY = "ontologies";

    /**
     * {@link OntologyCache} instance to use inside the module.
     */
    private static OntologyCache ontologyCache;

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
                "http://www.opensilex.org/vocabulary/set/factor/category",
                ONTOLOGIES_DIRECTORY+"/peco-factors.rdf",
                Lang.RDFXML,
                "peco-factors"
        ));
        list.add(new OntologyFileDefinition(
                OA.NS,
                ONTOLOGIES_DIRECTORY+"/oa.rdf",
                Lang.RDFXML,
                "oa"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeso#",
                ONTOLOGIES_DIRECTORY+"/oeso-core.owl",
                Lang.RDFXML,
                "vocabulary"
        ));
        list.add(new OntologyFileDefinition(
                "http://www.opensilex.org/vocabulary/oeev#",
                ONTOLOGIES_DIRECTORY+"/oeev.owl",
                Lang.RDFXML,
                "oeev"
        ));
        list.add(new OntologyFileDefinition(
                OWL.NAMESPACE,
                ONTOLOGIES_DIRECTORY+"/owl2.ttl",
                Lang.TURTLE,
                OWL.PREFIX
        ));
        list.add(new OntologyFileDefinition(
                Time.NS,
                ONTOLOGIES_DIRECTORY+"/time.ttl",
                Lang.TURTLE,
                Time.PREFIX
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

        invalidateCache();
        populateOntologyCache();
        
        insertDefaultVariablesGroup();
        insertDefaultMethod();
        insertDefaultInterestEntities();
    }


    @Override
    public void startup() throws Exception {
        invalidateCache();
        populateOntologyCache();
    }


    private void insertDefaultProvenance() throws Exception {

        SPARQLConfig sparqlConfig = getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        MongoDBConfig config = getOpenSilex().loadConfigPath(MongoDBConfig.DEFAULT_CONFIG_PATH, MongoDBConfig.class);
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

    /**
     * Init the {@link #ontologyCache} by reading the {@link CoreConfig} associated with the given {@link OpenSilex} instance
     * @param opensilex the {@link OpenSilex} instance, used to read {@link CoreConfig}
     */
    private static void initOntologyCache(OpenSilex opensilex) throws OntologyCacheException {

        if (ontologyCache != null) {
            return;
        }

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        CoreConfig coreConfig;
        try {
            coreConfig = opensilex.getModuleConfig(CoreModule.class, CoreConfig.class);
        } catch (OpenSilexModuleNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }

        /* #TODO : sparql should not be null , but when building module with maven, the sparql is null while it's not when launching StartServer  */

        if (sparql != null) {
            ontologyCache = coreConfig.enableOntologyCaching() ?
                    CaffeineOntologyCache.getInstance(sparql) :
                    NoOntologyCacheImpl.getInstance(sparql);
        }
    }

    public static OntologyCache getOntologyCacheInstance() {
        return ontologyCache;
    }

    protected void invalidateCache() throws OntologyCacheException {
        CoreModule.initOntologyCache(getOpenSilex());

        if (ontologyCache != null) {
            LOGGER.info("Invalidate ontology cache");
            ontologyCache.invalidate();
            LOGGER.info("Ontology cache invalidated with success");
        }
    }

    private static final String ONTOLOGY_CACHE_SUCCESS_MSG = "Ontology cache loaded with success. Duration: %d ms";

    protected void populateOntologyCache() throws OntologyCacheException {
        CoreModule.initOntologyCache(getOpenSilex());

        if (ontologyCache != null) {
            LOGGER.info("Populating ontology cache");
            Instant begin = Instant.now();
            ontologyCache.populate(AbstractOntologyCache.getRootModelsToLoad());

            String successMsg = String.format(ONTOLOGY_CACHE_SUCCESS_MSG, Duration.between(begin, Instant.now()).toMillis());
            LOGGER.info(successMsg);
        }
    }


    private static final String DEFAULT_VARIABLE_GROUP_NAME = "Environmental variables";
    private static final String DEFAULT_VARIABLE_GROUP_DESCRIPTION = "This group is about environmental variables\"";

    private void insertDefaultVariablesGroup() throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        
        VariablesGroupModel variablesGroup = new VariablesGroupModel();
        variablesGroup.setName(DEFAULT_VARIABLE_GROUP_NAME);
        variablesGroup.setDescription(DEFAULT_VARIABLE_GROUP_DESCRIPTION);
        
        LOGGER.info("Insert default variables group: {}",variablesGroup.getUri());
        try {
           sparql.create(variablesGroup);
        } catch (Exception e) {
            throw new SPARQLException("Couldn't create default variables group : " + e.getMessage(), e);
        }
    }

    private static final URI STANDARD_METHOD_URI = URI.create("http://www.opensilex.org/vocabulary/oeso#standard_method");
    private static final String STANDARD_METHOD_NAME = "Standard method";
    private static final String STANDARD_METHOD_DESCRIPTION = "For usual method";

    private void insertDefaultMethod() throws Exception {

        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        MethodModel method = new MethodModel();
        method.setUri(STANDARD_METHOD_URI);
        method.setName(STANDARD_METHOD_NAME);
        method.setDescription(STANDARD_METHOD_DESCRIPTION);

        LOGGER.info("Insert default method {}", method.getUri());
        try {
            sparql.create(method);
        } catch (Exception e) {
            throw new SPARQLException("Couldn't create default method : " + e.getMessage(), e);
        }
    }
    
    private void insertDefaultInterestEntities() throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        
        InterestEntityModel plot = new InterestEntityModel();
        InterestEntityModel plant = new InterestEntityModel();
        InterestEntityModel genotype = new InterestEntityModel();
        InterestEntityModel site = new InterestEntityModel();
        InterestEntityModel greenHouse = new InterestEntityModel();
        InterestEntityModel chamberGrowth = new InterestEntityModel();
        
        String plotName = "Plot";
        String plantName = "Plant";
        String genotypeName = "Genotype";
        String siteName = "Site";
        String greenHouseName = "Green house";
        String chamberGrowthName = "Chamber growth";
        
        plot.setName(plotName);
        plant.setName(plantName);
        genotype.setName(genotypeName);
        site.setName(siteName);
        greenHouse.setName(greenHouseName);
        chamberGrowth.setName(chamberGrowthName);
        
        try {
           sparql.create(plot);
           sparql.create(plant);
           sparql.create(genotype);
           sparql.create(site);
           sparql.create(greenHouse);
           sparql.create(chamberGrowth);
        } catch (Exception e) {
            throw new SPARQLException("Couldn't create default entities of interest : " + e.getMessage(), e);
        }
    }
}
