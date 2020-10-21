/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.config.ConfigDescription;
import org.opensilex.nosql.mongodb.MongoDBService;

/**
 *
 * @author vidalmor
 */
public interface NoSQLConfig {

    @ConfigDescription(
            value = "No SQL data source"
    )
    public NoSQLService nosql();

    @ConfigDescription(
            value = "MongoDB data source"
    )
    public MongoDBService mongodb();
}
