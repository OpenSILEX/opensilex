/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.datanucleus;

import com.mongodb.client.MongoClient;
import java.util.Properties;
import org.opensilex.nosql.datanucleus.mongo.MongoDBConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 *
 * @author vince
 */
@ServiceDefaultDefinition(implementation = MongoDBConnection.class)
public interface DataNucleusServiceConnection extends Service {

    public void definePersistentManagerProperties(Properties pmfProperties);

    public void setDatanucleus(DataNucleusService datanucleus);
    
    public MongoClient getMongoDBClient();
}
