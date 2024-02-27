/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.config.ConfigDescription;
import org.opensilex.front.config.CustomMenuItem;
import org.opensilex.front.config.MatomoConfig;

import java.util.List;
import java.util.Map;

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
            defaultString = "opensilex-HomeView"
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
            defaultString = "opensilex-front#opensilex"
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

    @ConfigDescription(
            value = "Name of the application to display",
            defaultString = "OpenSILEX"
    )
    String applicationName();

    @ConfigDescription(
            value = "Ability to be logged as guest",
            defaultBoolean = false
    )
    boolean connectAsGuest();

    @ConfigDescription(
            value = "Dashboard component"
    )
    DashboardConfig dashboard();

    @ConfigDescription(
            value = "Matomo configuration"
    )
    MatomoConfig matomo();

    @ConfigDescription(
            value = "Notification message for the instance"
    )
    Map<String, String> notificationMessage();

    @ConfigDescription(
            value = "Color theme for the notification message"
    )
    String notificationColorTheme();

    @ConfigDescription(
        value = "Date until which to send the notification"
    )
    String notificationEndDate();
}