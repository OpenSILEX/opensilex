/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.datanucleus;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vince
 */
public interface DataNucleusServiceConfig extends ServiceConfig {

    @ConfigDescription(
            value = "Datanucleus connection to database"
    )
    DataNucleusServiceConnection connection();

}
