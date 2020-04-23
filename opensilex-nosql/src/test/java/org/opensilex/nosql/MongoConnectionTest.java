/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.opensilex.integration.test.IntegrationTestCategory;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBConnection;

/**
 *
 * @author charlero
 */
@Category(IntegrationTestCategory.class)
public class MongoConnectionTest extends NoSQLServiceTest {

    @BeforeClass
    public static void setupMongo() throws Exception {
        MongoDBConnection connection = new MongoDBConnection(new MongoDBConfig() {
            @Override
            public String host() {
                return "127.0.0.1";
            }

            @Override
            public int port() {
                return 37017;
            }

            @Override
            public String database() {
                return "test";
            }

            @Override
            public String username() {
                return null;
            }

            @Override
            public String password() {
                return null;
            }

            @Override
            public String authDB() {
                return null;
            }

            @Override
            public Map<String, String> options() {
                return new HashMap<>();
            }
        });
        connection.setOpenSilex(opensilex);
        connection.setup();
        connection.startup();
        initialize(connection);
    }
}
