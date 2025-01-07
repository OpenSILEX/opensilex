/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import org.opensilex.config.ConfigDescription;
import org.opensilex.core.config.SharedResourceInstanceItem;

import java.util.List;

/**
 *
 * @author charlero
 */
public interface CoreConfig {

    @ConfigDescription(
            value = "Activate access logs by user",
            defaultBoolean = false
    )
    boolean enableLogs();

    @ConfigDescription(
            value = "Metrics options"
    )
    public MetricsConfig metrics();

    @ConfigDescription(
            value = "List of shared resource instances"
    )
    List<SharedResourceInstanceItem> sharedResourceInstances();

    @ConfigDescription(
            value = "Agroportal options"
    )
    AgroportalAPIConfig agroportal();

}
