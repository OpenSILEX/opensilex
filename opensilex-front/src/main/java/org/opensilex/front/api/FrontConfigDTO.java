/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.front.config.VersionLabel;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Schema
public class FrontConfigDTO {

    @NotNull
    String pathPrefix;

    @NotNull
    String homeComponent;

    @NotNull
    String notFoundComponent;

    @NotNull
    String headerComponent;

    @NotNull
    String loginComponent;

    @NotNull
    String menuComponent;

    @NotNull
    String footerComponent;

    @NotNull
    List<RouteDTO> routes;

    String themeModule;

    String themeName;

    String openIDAuthenticationURI;

    String openIDConnectionTitle;

    String samlProxyLoginURI;

    String samlConnectionTitle;

    Boolean activateResetPassword;

    String geocodingService;

    List<String> menuExclusions;

    VersionLabel versionLabel;

    String applicationName;

    Boolean connectAsGuest;

    DashboardConfigDTO dashboard;

    Boolean gdprFileIsConfigured;

    MatomoConfigDTO matomo;

    Map<String, String> notificationMessage;

    String notificationColorTheme;

    LocalDate notificationEndDate;

    AgroportalOntologiesConfigDTO agroportal;

    @Schema(description = "Application url path prefix", example = "app")
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Schema(description = "Home component identifier", example = "opensilex-DefaultHomeComponent")
    public String getHomeComponent() {
        return homeComponent;
    }

    public void setHomeComponent(String homeComponent) {
        this.homeComponent = homeComponent;
    }

    @Schema(description = "Not found component identifier", example = "opensilex-DefaultNotFoundComponent")
    public String getNotFoundComponent() {
        return notFoundComponent;
    }

    public void setNotFoundComponent(String notFoundComponent) {
        this.notFoundComponent = notFoundComponent;
    }

    @Schema(description = "Menu component identifier", example = "opensilex-DefaultMenuComponent")
    public String getMenuComponent() {
        return menuComponent;
    }

    public void setMenuComponent(String menuComponent) {
        this.menuComponent = menuComponent;
    }

    @Schema(description = "Footer component identifier", example = "opensilex-DefaultFooterComponent")
    public String getFooterComponent() {
        return footerComponent;
    }

    public void setFooterComponent(String footerComponent) {
        this.footerComponent = footerComponent;
    }

    @Schema(description = "Header component identifier", example = "opensilex-DefaultHeaderComponent")
    public String getHeaderComponent() {
        return headerComponent;
    }

    public void setHeaderComponent(String headerComponent) {
        this.headerComponent = headerComponent;
    }

    @Schema(description = "Login component identifier", example = "opensilex-DefaultLoginComponent")
    public String getLoginComponent() {
        return loginComponent;
    }

    public void setLoginComponent(String loginComponent) {
        this.loginComponent = loginComponent;
    }

    @Schema(description = "List of configured routes")
    public List<RouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDTO> routes) {
        this.routes = routes;
    }

    @Schema(description = "Theme module identifier")
    public String getThemeModule() {
        return themeModule;
    }

    public void setThemeModule(String themeModule) {
        this.themeModule = themeModule;
    }

    @Schema(description = "Theme module name")
    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    @Schema(description = "OpenID Authorization URI")
    public String getOpenIDAuthenticationURI() {
        return openIDAuthenticationURI;
    }

    public void setOpenIDAuthenticationURI(String openIDAuthorizationURI) {
        this.openIDAuthenticationURI = openIDAuthorizationURI;
    }

    public String getOpenIDConnectionTitle() {
        return openIDConnectionTitle;
    }

    public void setOpenIDConnectionTitle(String openIDConnectionTitle) {
        this.openIDConnectionTitle = openIDConnectionTitle;
    }

    public String getSamlProxyLoginURI() {
        return samlProxyLoginURI;
    }

    public void setSamlProxyLoginURI(String samlProxyLoginURI) {
        this.samlProxyLoginURI = samlProxyLoginURI;
    }

    public String getSamlConnectionTitle() {
        return samlConnectionTitle;
    }

    public void setSamlConnectionTitle(String samlConnectionTitle) {
        this.samlConnectionTitle = samlConnectionTitle;
    }

    public Boolean getActivateResetPassword() {
        return activateResetPassword;
    }

    public void setActivateResetPassword(Boolean activateResetPassword) {
        this.activateResetPassword = activateResetPassword;
    }

    @Schema(description = "Geocoding service")
    public String getGeocodingService() {
        return geocodingService;
    }

    public void setGeocodingService(String geocodingService) {
        this.geocodingService = geocodingService;
    }

    @Schema(description = "Menu exclusions")
    public List<String> getMenuExclusions() {
        return menuExclusions;
    }

    public void setMenuExclusions(List<String> menuExclusions) {
        this.menuExclusions = menuExclusions;
    }

    @Schema(description = "Version label to use in the header")
    public VersionLabel getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(VersionLabel versionLabel) {
        this.versionLabel = versionLabel;
    }

    public DashboardConfigDTO getDashboard() {
        return dashboard;
    }

    public void setDashboard(DashboardConfigDTO dashboard) {
        this.dashboard = dashboard;
    }

    public AgroportalOntologiesConfigDTO getAgroportal() {
        return agroportal;
    }

    public void setAgroportal(AgroportalOntologiesConfigDTO agroportal) {
        this.agroportal = agroportal;
    }

    @Schema(description = "Name of the application to display")
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Schema(description = "Ability to be logged as guest")
    public boolean getConnectAsGuest() {
        return connectAsGuest;
    }

    public void setConnectAsGuest(boolean connectAsGuest) {
        this.connectAsGuest = connectAsGuest;
    }

    @Schema(description = "GDPR PDF is configured")
    public Boolean getGdprFileIsConfigured() { return gdprFileIsConfigured; }

    public void setGdprFileIsConfigured(Boolean gdprFileIsConfigured) { this.gdprFileIsConfigured = gdprFileIsConfigured; }

    public MatomoConfigDTO getMatomo() {
        return matomo;
    }

    public void setMatomo(MatomoConfigDTO matomo) {
        this.matomo = matomo;
    }

    @Schema(description = "Notification message for the instance", example = "deployment of version 1.2 on January 22, 2024")
    public Map <String, String> getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(Map <String, String> notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

     @Schema(description = "Color theme for the notification message", example = "Warning")
    public String getNotificationColorTheme() {
        return notificationColorTheme;
    }

    public void setNotificationColorTheme(String notificationColorTheme) {
        this.notificationColorTheme = notificationColorTheme;
    }

    @Schema(description = "Date until which to send the notification", example = "2024-04-31")
    public LocalDate getNotificationEndDate() {
        return notificationEndDate;
    }

    public void setNotificationEndDate(String notificationEndDate) {
        // this.notificationEndDate = notificationEndDate;
        try {
            this.notificationEndDate = LocalDate.parse(notificationEndDate);
        } catch(DateTimeParseException e){
            notificationEndDate = null;
        }
    }

}