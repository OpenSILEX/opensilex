package org.opensilex.benchmark.core;

import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.opensilex.OpenSilex;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@State(Scope.Benchmark)
public class AbstractOpenSilexBenchmark {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenSilexBenchmark.class);

    @Param("")
    String configPath;
    public static final String CONFIG_PATH_JMH_PARAM = "configPath";

    protected OpenSilex openSilex;
    protected SPARQLService sparql;
    protected MongoDBService mongodb;
    protected FileStorageService fs;

    public void setup() throws Exception {

        if (StringUtils.isEmpty(configPath)) {
            throw new IllegalArgumentException("Null or empty configPath (CONFIG_PATH_JMH_PARAM) : " + configPath);
        }

        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.CONFIG_FILE_ARG_KEY, configPath);

        LOGGER.info("Initializing OpenSILEX with config {}", configPath);
        openSilex = OpenSilex.createInstance(args);
        LOGGER.info("Initialize OpenSILEX for JMH benchmarking [OK]");

        sparql = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        mongodb = openSilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);

        LOGGER.info("Register OpenSILEX SPARQLService [OK] : {}", sparql);
        LOGGER.info("Register OpenSILEX MongoDBService [OK] : {}", mongodb);
        LOGGER.info("Register OpenSILEX FileStorageService [OK] : {}", fs);
    }

}
