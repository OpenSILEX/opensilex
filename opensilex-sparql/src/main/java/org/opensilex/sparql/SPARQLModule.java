/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    public final static String ONTOLOGY_BASE_DOMAIN = "http://www.opensilex.org/";

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return SPARQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "ontologies";
    }

    @Override
    public void startup() throws Exception {
        final String basePrefix;
        SPARQLConfig sparqlConfig = this.getConfig(SPARQLConfig.class);
        if (sparqlConfig != null) {
            basePrefix = sparqlConfig.baseURIAlias() + "-";
        } else {
            basePrefix = "";
        }

        SPARQLService sparql = sparqlConfig.sparql();

        if (sparqlConfig.usePrefixes()) {
            SPARQLClassObjectMapper.forEach((Resource resource, SPARQLClassObjectMapper<?> mapper) -> {
                String resourceNamespace = mapper.getResourceGraphNamespace();
                String resourcePrefix = mapper.getResourceGraphPrefix();
                if (resourceNamespace != null && resourcePrefix != null && !resourcePrefix.isEmpty()) {
                    sparql.addPrefix(basePrefix + mapper.getResourceGraphPrefix(), resourceNamespace + "#");
                }
            });
        } else {
            sparql.clearPrefixes();
        }
        URIDeserializer.setPrefixes(sparql.getPrefixMapping());

        SPARQLConfig cfg = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        sparql.addPrefix(cfg.baseURIAlias(), cfg.baseURI());

    }

    /**
     * Return configured platform base URI
     *
     * @return plateform URI
     */
    public static URI getPlatformURI() {
        try {
            if (OpenSilex.getInstance() == null) {
                return new URI("http://test.opensilex.org/");
            } else {
                return new URI(OpenSilex.getInstance().getModuleByClass(SPARQLModule.class).getConfig(SPARQLConfig.class).baseURI());
            }
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Base module not found, can't really happend because it's in the same package", ex);
            return null;
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid Base URI", ex);
            return null;
        }
    }

    public static URI getPlatformDomainGraphURI(String graphSuffix) {
        return getPlatformURI().resolve(graphSuffix);
    }

    public static void clearPlatformGraphs(SPARQLService sparql, List<String> graphsSuffixToClear) throws SPARQLQueryException {
        for (String graphName : graphsSuffixToClear) {
            sparql.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        sparql.clearGraph(SPARQLModule.getPlatformURI());
    }

    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Initialize RDF4J");
        initRDF4J(reset);
//        LOGGER.info("Insert species");
//        insertSpecies(reset);
    }
    
    @Override
    public void check() throws Exception {
        LOGGER.info("Check RDF4J connnection & ontologies initialization");
        OpenSilex opensilex = OpenSilex.getInstance();
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        List<SPARQLStatement> results = sparql.describe(new URI("http://www.opensilex.org/vocabulary/oeso"));
        
        if (results.size() == 0) {
            LOGGER.error("There is missing data into your triple store, did you execute 'opensilex system setup' command ?");
            throw new Exception("There is missing data into your triple store, did you execute 'opensilex system setup' command ?");
        }
    }

    public static void initRDF4J(boolean reset) throws Exception {
        OpenSilex opensilex = OpenSilex.getInstance();
        
        // initialise repository manager
        RDF4JConfig config = opensilex.loadConfigPath("ontologies.sparql.rdf4j", RDF4JConfig.class);

        // Create repository
        createRDF4JRepository(config, reset);

        // Restart repository to reload sparql service
        opensilex.restart();
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);

        // Import default ontologies
        LOGGER.info("Install oa ontology: http://www.w3.org/ns/oa");
        InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oa.rdf"));
        sparql.loadOntologyStream(new URI("http://www.w3.org/ns/oa"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeso ontology: http://www.opensilex.org/vocabulary/oeso");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oeso.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeso"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeev ontology: http://www.opensilex.org/vocabulary/oeev");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oeev.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeev"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();
        
        SPARQLConfig sparqlConfig = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        URI graph = new URI(sparqlConfig.baseURI() + "species");
        
        LOGGER.info("Install Agrovoc species: " + graph.toString());
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/species.ttl"));
        sparql.loadOntologyStream(graph, ontologyStream, Lang.TTL);
        ontologyStream.close();
    }
    
    private static void createRDF4JRepository(RDF4JConfig config, boolean reset) throws IOException {
        RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(config.serverURI());
        repositoryManager.init();

        // Remove existing repository
        if (reset) {
            repositoryManager.removeRepository(config.repository());
        }

        // Read repository configuration file located in jar
        File rdf4jApiJar = ClassUtils.getJarFile(RepositoryConfig.class);
        File templateFile = ClassUtils.getFileFromJar(rdf4jApiJar, "org/eclipse/rdf4j/repository/config/native-shacl.ttl");
        InputStream templateStream = new FileInputStream(templateFile);
        String template;
        try {
            template = IOUtil.readString(new InputStreamReader(templateStream, "UTF-8"));
        } finally {
            templateStream.close();
        }
        final ConfigTemplate configTemplate = new ConfigTemplate(template);

        // This variable contains all keys parsed from template, use debugger to watch them.
        // final Map<String, List<String>> variableMap = configTemplate.getVariableMap();
        // Define repository template parameters
        final Map<String, String> valueMap = new HashMap<String, String>() {
            {
                put("Repository ID", config.repository());
                put("Repository title", config.repository());
                // Default template value write here for information
                 put("Query Iteration Cache size", "10000");
                 put("Triple indexes", "spoc,posc");
                // put("EvaluationStrategyFactory", "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory");

            }
        };

        final String configString = configTemplate.render(valueMap);
        final Model graph = new LinkedHashModel();

        final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
        rdfParser.setRDFHandler(new StatementCollector(graph));
        rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);

        final org.eclipse.rdf4j.model.Resource repositoryNode = Models
                .subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
                .orElseThrow(() -> new RepositoryConfigException("missing repository node"));

        RepositoryRegistry registry = RepositoryRegistry.getInstance();
        ServiceLoader<RepositoryFactory> services = ServiceLoader.load(RepositoryFactory.class, OpenSilex.getClassLoader());
        services.forEach(action -> {
            registry.add(action);
        });

        final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
        repConfig.validate();

        repositoryManager.addRepositoryConfig(repConfig);
        repositoryManager.shutDown();
    }
    
    // TODO to remove
    private static void insertSpecies(boolean reset) throws Exception {
         SPARQLService sparql = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        SPARQLConfig sparqlConfig = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        UpdateBuilder builder = new UpdateBuilder();
        Node graph = NodeFactory.createURI(sparqlConfig.baseURI() + "species");
        
        if (reset) {
            sparql.clearGraph(new URI(graph.getURI()));
        }
        
        Resource speciesResource = Ontology.resource("http://www.opensilex.org/vocabulary/oeso#Species");
        Map<String, Map<String, String>> speciesInstances = new HashMap<>();

        speciesInstances.put("zeamays", new HashMap<String , String>() {
            {
                put("en", "Maize");
                put("fr", "Maïs");
                put("la", "Zea mays");
            }
        });

        speciesInstances.put("betavulgaris", new HashMap<String , String>() {
            {
                put("en", "Beet");
                put("fr", "Betterave");
                put("la", "Beta vulgaris");
            }
        });

        speciesInstances.put("canabissativa", new HashMap<String , String>() {
            {
                put("en", "Hemp");
                put("fr", "Chanvre");
                put("la", "Canabis sativa");
            }
        });

        speciesInstances.put("glycinemax", new HashMap<String , String>() {
            {
                put("en", "Soybean");
                put("fr", "Soja");
                put("la", "Glycine max");
            }
        });

        speciesInstances.put("gossypiumhirsutum", new HashMap<String , String>() {
            {
                put("en", "Upland cotton");
                put("fr", "Coton mexicain");
                put("la", "Gossypium hirsutum");
            }
        });

        speciesInstances.put("helianthusannuus", new HashMap<String , String>() {
            {
                put("en", "Sunflower");
                put("fr", "Tournesol");
                put("la", "Helianthus annuus");
            }
        });

        speciesInstances.put("linumusitatissum", new HashMap<String , String>() {
            {
                put("en", "Flax");
                put("fr", "Lin");
                put("la", "Linum usitatissimum");
            }
        });

        speciesInstances.put("lupinusalbus", new HashMap<String , String>() {
            {
                put("en", "Lupine");
                put("fr", "Lupin blanc");
                put("la", "Lupinus albus");
            }
        });

        speciesInstances.put("ordeumvulgare", new HashMap<String , String>() {
            {
                put("en", "Barley");
                put("fr", "Orge");
                put("la", "Ordeum vulgare");
            }
        });

        speciesInstances.put("orizasativa", new HashMap<String , String>() {
            {
                put("en", "Rice");
                put("fr", "Riz");
                put("la", "Oriza sativa");
            }
        });

        speciesInstances.put("pennisetumglaucum", new HashMap<String , String>() {
            {
                put("en", "Pearl millet");
                put("fr", "Mil");
                put("la", "Pennisetum glaucum");
            }
        });

        speciesInstances.put("pisumsativum", new HashMap<String , String>() {
            {
                put("en", "Peas");
                put("fr", "Pois protéagineux");
                put("la", "Pisum sativum");
            }
        });

        speciesInstances.put("populus", new HashMap<String , String>() {
            {
                put("en", "Polar");
                put("fr", "Peuplier");
                put("la", "Populus");
            }
        });

        speciesInstances.put("sorghumbicolor", new HashMap<String , String>() {
            {
                put("en", "Sorghum");
                put("fr", "Sorgho");
                put("la", "Sorghum bicolor");
            }
        });

        speciesInstances.put("viciafaba", new HashMap<String , String>() {
            {
                put("en", "Fababean");
                put("fr", "Féverole");
                put("la", "Vicia faba");
            }
        });

        speciesInstances.put("teosinte", new HashMap<String , String>() {
            {
                put("en", "Teosintes");
                put("fr", "Téosintes");
                put("la", "Teosinte");
            }
        });

        speciesInstances.put("tritucumaestivum", new HashMap<String , String>() {
            {
                put("en", "Bread wheat");
                put("fr", "Blé tendre");
                put("la", "Triticum aestivum");
            }
        });

        speciesInstances.put("tritucumturgidum", new HashMap<String , String>() {
            {
                put("en", "Durum wheat");
                put("fr", "Blé dur");
                put("la", "Triticum turgidum");
            }
        });

        speciesInstances.put("banana", new HashMap<String , String>() {
            {
                put("en", "Banana");
                put("fr", "Bananier");
                put("la", "Musa");
            }
        });

        speciesInstances.forEach((key, translations) -> {
            Node speciesURI = NodeFactory.createURI(sparqlConfig.baseURI() + "id/species/" + key);
            builder.addInsert(graph, speciesURI, org.apache.jena.vocabulary.RDF.type, speciesResource);
            translations.forEach((lang, label) -> {
                builder.addInsert(graph, speciesURI, RDFS.label, label + "@" + lang);
            });
        });
        
        sparql.executeUpdateQuery(builder);
    }
    
    

}
