/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import org.opensilex.config.ConfigDescription;
import org.opensilex.nosql.mongodb.MongoDBService;

/**
 *
 * @author vidalmor
 */
public interface NoSQLConfig {

    @ConfigDescription(
            value = "MongoDB data source"
    )
    public MongoDBService mongodb();
}
