//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql;

import org.junit.AfterClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.nosql.EmbedMongod.MONGO_HOST;
import static org.opensilex.nosql.EmbedMongod.MONGO_PORT;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.unit.test.AbstractUnitTest;

import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author vincent
 */
public abstract class MongoDBServiceTest extends AbstractUnitTest {

    protected static EmbedMongod embedMongod;
    protected static MongoDBServiceV2 mongoDBv2;
    protected static SPARQLService sparql;

    @BeforeClass
    public static void setUp(){
        embedMongod = EmbedMongod.getInstance();
        embedMongod.start();
        mongoDBv2 = getOpensilex().getServiceInstance(MongoDBServiceV2.DEFAULT_SERVICE, MongoDBServiceV2.class);
        sparql = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
    }

    @AfterClass
    public static void stop() {
        embedMongod.stop();
    }

    @Test
    public void testConnection() throws IOException {
        try(Socket socket = new Socket(MONGO_HOST, MONGO_PORT)){
            assertTrue("Embed mongo client started", socket.isConnected());
        }
    }

    @Test
    public void testGetService(){
        assertNotNull(mongoDBv2);
    }


}