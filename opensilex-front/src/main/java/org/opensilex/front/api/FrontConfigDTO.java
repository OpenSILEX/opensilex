/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.front.config.VersionLabel;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@ApiModel
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

    @ApiModelProperty(value = "Application url path prefix", example = "app")
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @ApiModelProperty(value = "Home component identifier", example = "opensilex-DefaultHomeComponent")
    public String getHomeComponent() {
        return homeComponent;
    }

    public void setHomeComponent(String homeComponent) {
        this.homeComponent = homeComponent;
    }

    @ApiModelProperty(value = "Not found component identifier", example = "opensilex-DefaultNotFoundComponent")
    public String getNotFoundComponent() {
        return notFoundComponent;
    }

    public void setNotFoundComponent(String notFoundComponent) {
        this.notFoundComponent = notFoundComponent;
    }

    @ApiModelProperty(value = "Menu component identifier", example = "opensilex-DefaultMenuComponent")
    public String getMenuComponent() {
        return menuComponent;
    }

    public void setMenuComponent(String menuComponent) {
        this.menuComponent = menuComponent;
    }

    @ApiModelProperty(value = "Footer component identifier", example = "opensilex-DefaultFooterComponent")
    public String getFooterComponent() {
        return footerComponent;
    }

    public void setFooterComponent(String footerComponent) {
        this.footerComponent = footerComponent;
    }

    @ApiModelProperty(value = "Header component identifier", example = "opensilex-DefaultHeaderComponent")
    public String getHeaderComponent() {
        return headerComponent;
    }

    public void setHeaderComponent(String headerComponent) {
        this.headerComponent = headerComponent;
    }

    @ApiModelProperty(value = "Login component identifier", example = "opensilex-DefaultLoginComponent")
    public String getLoginComponent() {
        return loginComponent;
    }

    public void setLoginComponent(String loginComponent) {
        this.loginComponent = loginComponent;
    }

    @ApiModelProperty(value = "List of configured routes")
    public List<RouteDTO> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDTO> routes) {
        this.routes = routes;
    }

    @ApiModelProperty(value = "Theme module identifier")
    public String getThemeModule() {
        return themeModule;
    }

    public void setThemeModule(String themeModule) {
        this.themeModule = themeModule;
    }

    @ApiModelProperty(value = "Theme module name")
    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    @ApiModelProperty(value = "OpenID Authorization URI")
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

    @ApiModelProperty(value = "Geocoding service")
    public String getGeocodingService() {
        return geocodingService;
    }

    public void setGeocodingService(String geocodingService) {
        this.geocodingService = geocodingService;
    }

    @ApiModelProperty(value = "Menu exclusions")
    public List<String> getMenuExclusions() {
        return menuExclusions;
    }

    public void setMenuExclusions(List<String> menuExclusions) {
        this.menuExclusions = menuExclusions;
    }

    @ApiModelProperty(value = "Version label to use in the header")
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

    @ApiModelProperty(value = "Name of the application to display")
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @ApiModelProperty(value = "Ability to be logged as guest")
    public boolean getConnectAsGuest() {
        return connectAsGuest;
    }

    public void setConnectAsGuest(boolean connectAsGuest) {
        this.connectAsGuest = connectAsGuest;
    }

    @ApiModelProperty(value = "GDPR PDF is configured")
    public Boolean getGdprFileIsConfigured() { return gdprFileIsConfigured; }

    public void setGdprFileIsConfigured(Boolean gdprFileIsConfigured) { this.gdprFileIsConfigured = gdprFileIsConfigured; }

    public MatomoConfigDTO getMatomo() {
        return matomo;
    }

    public void setMatomo(MatomoConfigDTO matomo) {
        this.matomo = matomo;
    }

    @ApiModelProperty(value = "Notification message for the instance", example = "deployment of version 1.2 on January 22, 2024")
    public Map <String, String> getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(Map <String, String> notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

     @ApiModelProperty(value = "Color theme for the notification message", example = "Warning")
    public String getNotificationColorTheme() {
        return notificationColorTheme;
    }

    public void setNotificationColorTheme(String notificationColorTheme) {
        this.notificationColorTheme = notificationColorTheme;
    }

    @ApiModelProperty(value = "Date until which to send the notification", example = "2024-04-31")
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