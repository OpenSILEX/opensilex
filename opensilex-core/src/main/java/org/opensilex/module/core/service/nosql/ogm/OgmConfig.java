/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.nosql.ogm;

import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vincent
 */
public interface OgmConfig {

    @ConfigDescription(
            value = "Hibernate OGM Provider",
            defaultString = MongoDB.DATASTORE_PROVIDER_NAME
    )
    String provider();

    @ConfigDescription(
            value = "Hibernate OGM provider options",
            defaultMap = {
                AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY + ": jta",
                AvailableSettings.JTA_PLATFORM + ": JBossTS"
            }
    )
    Map<String, String> options();
}
