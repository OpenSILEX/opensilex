/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vincent
 */
public interface FrontConfig extends ModuleConfig {

    @ConfigDescription(
            value = "Front welcome component if unlogged",
            defaultString = "sixtine#SixtineWelcomeComponent"
    //            defaultString = "shared#DefaultHomeComponent"
    )
    String welcomeComponent();

    @ConfigDescription(
            value = "Front home component once logged",
            defaultString = "sixtine#SixtineHomeComponent"
//            defaultString = "shared#DefaultHomeComponent"
    )
    String homeComponent();

    @ConfigDescription(
            value = "Front not found component",
            defaultString = "shared#DefaultNotFoundComponent"
    )
    String notFoundComponent();

    @ConfigDescription(
            value = "Front header component definition",
            defaultString = "sixtine#SixtineHeaderComponent"
    //            defaultString = "organization#OrganizationComponent"

    )
    String headerComponent();

    @ConfigDescription(
            value = "Front login component definition",
            defaultString = "sixtine#SixtineLoginComponent"
    //            defaultString = "shared#DefaultMenuComponent"
    )
    String loginComponent();

    @ConfigDescription(
            value = "Front menu component definition",
            defaultString = "shared#DefaultMenuComponent"
    )
    String menuComponent();

    @ConfigDescription(
            value = "Front footer component definition",
            defaultString = "shared#DefaultFooterComponent"
    )
    String footerComponent();
}
