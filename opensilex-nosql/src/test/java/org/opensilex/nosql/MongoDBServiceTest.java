//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql;

import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.nosql.EmbedMongoClient.MONGO_HOST;
import static org.opensilex.nosql.EmbedMongoClient.MONGO_PORT;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.unit.test.AbstractUnitTest;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author vincent
 */
public class MongoDBServiceTest extends AbstractUnitTest {

    protected static EmbedMongoClient embedMongoClient;

    @BeforeClass
    public static void start(){
        try{
            embedMongoClient = EmbedMongoClient.getInstance();
        }catch (IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void destroy() {
        embedMongoClient.stop();
    }

    @Test
    public void testConnection() throws IOException {
        try(Socket socket = new Socket(MONGO_HOST, MONGO_PORT)){
            assertTrue("Embed mongo client start",socket.isConnected());
        }
    }

    @Test
    public void testGetService(){
        MongoDBServiceV2 service = getNewMongoDBService();
        assertNotNull(service);
    }

    protected static MongoDBServiceV2 getNewMongoDBService(){
        return getOpensilex().getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);
    }


}
