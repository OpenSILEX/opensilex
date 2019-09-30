/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.bigdata.mongodb;

import java.util.Map;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author Vincent Migot
 */
public interface MongoDBConfig {
    
    @ConfigDescription(
        value = "MongoDB main host",
        defaultString = "localhost"
    )
    public String host();
    
    @ConfigDescription(
        value = "MongoDB main host port",
        defaultInt = 27017
    )
    public int port();
    
    @ConfigDescription(
        value = "MongoDB database"
    )
    public String database();
    
    @ConfigDescription(
        value = "MongoDB username"
    )
    public String username();
    
    @ConfigDescription(
        value = "MongoDB password"
    )
    public String password();
    
    @ConfigDescription(
        value = "MongoDB authentication database"
    )
    public String authDB();
    
    @ConfigDescription(
        value = "MongoDB other connection options"
    )
    public Map<String, String> options();
}
