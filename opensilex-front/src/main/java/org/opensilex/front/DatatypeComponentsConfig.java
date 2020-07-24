/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vmigot
 */
public interface DatatypeComponentsConfig {

    @ConfigDescription(
            value = "Datatype input component name"
    )
    public String inputComponent();

    @ConfigDescription(
            value = "Datatype view component name"
    )
    public String viewComponent();
}
