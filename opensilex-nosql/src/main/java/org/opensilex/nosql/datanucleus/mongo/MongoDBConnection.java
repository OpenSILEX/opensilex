//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.datanucleus.DataNucleusServiceConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * MongoDB connection for DataNucleus.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.datanucleus.DataNucleusService
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBConnection extends BaseService implements DataNucleusServiceConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(MongoDBConnection.class);

    public MongoDBConnection(MongoDBConfig config) {
        super(config);
    }

    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }

    private DataNucleusService datanucleus;

    @Override
    public void definePersistentManagerProperties(Properties pmfProperties) {
        MongoDBConfig cfg = getImplementedConfig();
        pmfProperties.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + cfg.host() + ":" + cfg.port() + "/" + cfg.database());
        pmfProperties.setProperty("javax.jdo.option.Mapping", "mongodb");
        pmfProperties.setProperty("datanucleus.schema.autoCreateAll", "true");
    }

    @Override
    public void setDatanucleus(DataNucleusService datanucleus) {
        this.datanucleus = datanucleus;
    }

    @Override
    public MongoClient getMongoDBClient() {
        String connectionString = "mongodb://";
        MongoDBConfig cfg = getImplementedConfig();
        if (cfg.username() != null && cfg.password() != null && !cfg.username().isEmpty() && !cfg.password().isEmpty()) {
            connectionString += cfg.username() + ":" + cfg.password() + "@";
        }
        connectionString += cfg.host() + ":" + cfg.port();
        connectionString += "/" + cfg.database();

        Map<String, String> options = new HashMap<>();
        options.putAll(cfg.options());
        if (cfg.authDB() != null) {
            options.put("authSource", cfg.authDB());
        }
        if (options.size() > 0) {
            connectionString += "?";
            Set<String> keys = options.keySet();
            int i = 0;
            for (String key : keys) {
                String value = options.get(key);
                connectionString += key + "=" + value;
                i++;

                if (i < options.size()) {
                    connectionString += "&";
                }
            }
        }

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new URICodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();
        return MongoClients.create(clientSettings);
    }

}
