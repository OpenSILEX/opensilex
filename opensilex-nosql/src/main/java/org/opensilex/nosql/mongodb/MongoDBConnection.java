//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.naming.NamingException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceConstructorArguments;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB connection for DataNucleus.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        configClass = MongoDBConfig.class,
        configID = ""
)
public class MongoDBConnection extends AbstractDataNucleusConnection implements NoSQLConnection {

    
    private final GridFSBucket gridFSBucket ;

    /**
     * Constructor for MongoDB connection
     * <pre>
     * TODO setup correct configuration
     * </pre>
     *
     * @param config MongoDB configuration
     */
    public MongoDBConnection(MongoDBConfig config) throws NamingException {
        super(config);
        
        MongoDatabase myDatabase =  null;
        try (MongoClient persistenceManager = (MongoClient) this.getPersistenceManager()) {
            myDatabase = persistenceManager.getDatabase(config.database());
            gridFSBucket = GridFSBuckets.create(myDatabase, "files");
        }; 
    }
    
 

    @Override
    public void startup() throws NamingException {
     
    }

    @Override
    public void shutdown() {
        closePersistanteManagerFactory();
        // TODO implement mongodb shutdown mechanism
    }

    private final MongoDBConfig config;

    public MongoDBConnection(MongoDBConfig config) {
        this.config = config;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + config.host() + ":" + config.port() + "/" + config.database());
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.Mapping", "mongodb");
        this.PMF_PROPERTIES.setProperty("datanucleus.schema.autoCreateAll", "true");
    }

    @Override
    public OpenSilex getOpenSilex() {
        return this.opensilex;
    }

    @Override
    protected PersistenceUnitMetaData getConfigProperties(ServiceConfig config) {
        MongoDBConfig mongoConfig = (MongoDBConfig) config;
        PersistenceUnitMetaData pumd = new PersistenceUnitMetaData("MyPersistenceUnit", "RESOURCE_LOCAL", null);

        pumd.addProperty("javax.jdo.option.ConnectionURL", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        pumd.addProperty("javax.jdo.option.ConnectionURL", "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());
        LOGGER.debug("javax.jdo.option.ConnectionURL" + "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());

        pumd.addProperty("javax.jdo.option.Mapping", "mongodb");
        pumd.addProperty("datanucleus.schema.autoCreateAll", "true"); 

        return pumd;
    }
    
    public ObjectId createFileFromStream(String name, InputStream streamToUploadFrom){
        // Create some custom options
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(358400)
                .metadata(new Document("uri", "presentation"));
        return gridFSBucket.uploadFromStream("mongodb-tutorial", streamToUploadFrom, options); 
    }
}
