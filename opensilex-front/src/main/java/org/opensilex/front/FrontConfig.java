/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.config.ConfigDescription;
import org.opensilex.front.config.CustomMenuItem;

import java.util.List;

/**
 *
 * @author vincent
 */
public interface FrontConfig {

    @ConfigDescription(
            value = "Front login component definition",
            defaultString = "opensilex-DefaultLoginComponent"
    )
    String loginComponent();

    @ConfigDescription(
            value = "Front home component once logged",
            defaultString = "opensilex-DefaultHomeComponent"
    )
    String homeComponent();

    @ConfigDescription(
            value = "Front not found component",
            defaultString = "opensilex-DefaultNotFoundComponent"
    )
    String notFoundComponent();

    @ConfigDescription(
            value = "Front header component definition",
            defaultString = "opensilex-DefaultHeaderComponent"
    )
    String headerComponent();

    @ConfigDescription(
            value = "Front menu component definition",
            defaultString = "opensilex-DefaultMenuComponent"
    )
    String menuComponent();

    @ConfigDescription(
            value = "Front footer component definition",
            defaultString = "opensilex-DefaultFooterComponent"
    )
    String footerComponent();

    @ConfigDescription(
            value = "Front theme identifier",
            defaultString = "opensilex-front#phis"
    )
    String theme();

    @ConfigDescription(
            value = "List of menu identifiers to exclude"
    )
    List<String> menuExclusions();

    @ConfigDescription(
            value = "Application menu custom definition"
    )
    List<CustomMenuItem> customMenu();

    @ConfigDescription(
            value = "Geocoding service to autocomplete the address",
            defaultString = "Photon"
    )
    String geocodingService();

    @ConfigDescription(
            value = "Version label to display at the top of the app"
    )
    String versionLabel();
}
